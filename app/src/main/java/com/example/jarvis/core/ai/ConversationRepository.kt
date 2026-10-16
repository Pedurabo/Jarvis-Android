package com.example.jarvis.core.ai

import android.util.Log
import com.example.jarvis.core.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing chat history in Firestore.
 * Structure: users/{userId}/conversations/{conversationId}/messages/{messageId}
 */
@Singleton
class ConversationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager
) {
    private val TAG = "ConversationRepository"
    private val conversationId = "main_chat"

    private val userId: String
        get() = authManager.userId ?: "unauthenticated_user"

    data class FirestoreMessage(
        val role: String = "",
        val text: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Saves a message to the sub-collection.
     */
    suspend fun saveMessage(role: String, text: String) {
        val currentUserId = authManager.userId ?: return
        val message = FirestoreMessage(role = role, text = text)

        try {
            firestore.collection("users")
                .document(currentUserId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(message)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message", e)
        }
    }

    /**
     * Fetches the last N messages for context.
     */
    suspend fun getLastMessages(limit: Long = 10): List<FirestoreMessage> {
        val currentUserId = authManager.userId ?: return emptyList()

        return try {
            val snapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            snapshot.toObjects(FirestoreMessage::class.java).reversed()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching messages", e)
            emptyList()
        }
    }
}
