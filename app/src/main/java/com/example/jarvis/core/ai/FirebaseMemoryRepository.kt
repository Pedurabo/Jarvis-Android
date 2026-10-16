package com.example.jarvis.core.ai

import com.example.jarvis.core.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository to manage AI memories in Firebase Firestore.
 * Supports offline caching and sync with local Room database.
 */
@Singleton
class FirebaseMemoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val memoryDao: MemoryDao,
    private val authManager: AuthManager
) {

    private val userId: String
        get() = authManager.userId ?: "unauthenticated_user"

    /**
     * Firestore Schema:
     * users/{userId}/memories/{memoryId}
     */

    suspend fun saveMemory(text: String, tags: List<String> = emptyList()) {
        val currentUserId = authManager.userId ?: return // Don't save if not logged in

        val timestamp = System.currentTimeMillis()
        val memoryData = hashMapOf(
            "text" to text,
            "timestamp" to timestamp,
            "tags" to tags
        )

        // Save to Firestore under the user's specific document
        firestore.collection("users")
            .document(currentUserId)
            .collection("memories")
            .add(memoryData)
            .await()
    }

    suspend fun getRelevantMemories(limit: Long = 10): List<MemoryEntity> {
        val currentUserId = authManager.userId ?: return memoryDao.getRecentMemories()

        return try {
            val snapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("memories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            snapshot.documents.map { doc ->
                MemoryEntity(
                    text = doc.getString("text") ?: "",
                    embedding = floatArrayOf(),
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }
        } catch (e: Exception) {
            memoryDao.getRecentMemories()
        }
    }
}
