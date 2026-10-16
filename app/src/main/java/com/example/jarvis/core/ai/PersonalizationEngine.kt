package com.example.jarvis.core.ai

import android.content.Context
import android.util.Log
import com.example.jarvis.core.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalizationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager,
    private val llmManager: LLMManager
) {
    private val TAG = "PersonalizationEngine"
    private var cachedProfile: UserProfile? = null

    suspend fun getUserProfile(): UserProfile {
        val userId = authManager.userId ?: return UserProfile()
        if (cachedProfile != null) return cachedProfile!!

        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val profile = if (doc.exists()) {
                val data = doc.data?.get("personalization") as? String
                if (data != null) {
                    parseProfile(data)
                } else UserProfile()
            } else UserProfile()
            cachedProfile = profile
            profile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user profile", e)
            UserProfile()
        }
    }

    private fun parseProfile(jsonStr: String): UserProfile {
        return try {
            val json = JSONObject(jsonStr)
            val prefsJson = json.optJSONObject("preferences")
            val habitsJson = json.optJSONArray("learnedHabits")

            val prefs = if (prefsJson != null) {
                UserPreferences(
                    responseTone = prefsJson.optString("responseTone", "Helpful and concise"),
                    language = prefsJson.optString("language", "English"),
                    preferredName = prefsJson.optString("preferredName", "User")
                )
            } else UserPreferences()

            val habits = mutableListOf<Habit>()
            if (habitsJson != null) {
                for (i in 0 until habitsJson.length()) {
                    val h = habitsJson.getJSONObject(i)
                    habits.add(Habit(
                        activity = h.optString("activity"),
                        frequency = h.optString("frequency"),
                        context = h.optString("context")
                    ))
                }
            }

            UserProfile(preferences = prefs, learnedHabits = habits)
        } catch (e: Exception) {
            UserProfile()
        }
    }

    /**
     * Periodically called to let the LLM extract habits from recent memories.
     */
    suspend fun updateLearnedData(recentMemories: List<String>) {
        val prompt = """
            Analyze these recent memories and extract user habits, routines, or preferences.
            Be objective. Focus on recurring times, locations, and explicit requests.

            Memories: ${recentMemories.joinToString("\n")}

            Respond STRICTLY in JSON format:
            {
              "learnedHabits": [{"activity": "string", "frequency": "string", "context": "string"}],
              "preferences": {"responseTone": "string", "language": "string", "preferredName": "string"}
            }
        """.trimIndent()

        try {
            val response = llmManager.getSmartResponse(prompt)
            val json = JSONObject(response)
            saveProfileUpdate(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update learned data", e)
        }
    }

    private suspend fun saveProfileUpdate(json: JSONObject) {
        val userId = authManager.userId ?: return
        firestore.collection("users").document(userId)
            .update("personalization", json.toString())
            .await()
        cachedProfile = null // Invalidate cache
    }
}
