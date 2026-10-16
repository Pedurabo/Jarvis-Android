package com.example.jarvis.core.ai

import android.util.Log
import com.example.jarvis.core.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager
) {
    private val TAG = "FeedbackManager"
    private var cachedLearnings: String? = null

    /**
     * Saves user feedback/correction to Firestore.
     */
    suspend fun saveCorrection(query: String, failedResponse: String, userCorrection: String, category: String = "general") {
        val uid = authManager.userId ?: return
        val feedback = UserFeedback(
            query = query,
            failedResponse = failedResponse,
            correction = userCorrection,
            category = category
        )

        try {
            firestore.collection("users")
                .document(uid)
                .collection("learnings")
                .add(feedback)
                .await()
            cachedLearnings = null // Invalidate cache to fetch fresh data next time
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save feedback", e)
        }
    }

    /**
     * Retrieves recent "Learnings" to inject into the LLM prompt.
     * This implements a few-shot learning approach based on past mistakes.
     */
    suspend fun getCorrectionContext(): String {
        val uid = authManager.userId ?: return ""
        if (cachedLearnings != null) return cachedLearnings!!

        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("learnings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()

            val learnings = snapshot.documents.joinToString("\n") { doc ->
                val f = doc.toObject(UserFeedback::class.java)
                "User asked: '${f?.query}'\nAssistant said: '${f?.failedResponse}'\nCorrection: ${f?.correction}"
            }

            val context = if (learnings.isNotEmpty()) {
                "\n[PAST_CORRECTIONS_AND_LEARNINGS]\nBelow are examples of your previous mistakes and the correct way to handle them. Do not repeat these mistakes:\n$learnings\n"
            } else ""

            cachedLearnings = context
            context
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch corrections", e)
            ""
        }
    }
}
