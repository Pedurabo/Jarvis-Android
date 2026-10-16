package com.example.jarvis.core.memory

import android.util.Log
import com.example.jarvis.core.auth.AuthManager
import com.example.jarvis.core.memory.models.ConversationMessage
import com.example.jarvis.core.memory.models.Memory
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Senior Architect Implementation of Firebase-backed Memory System.
 *
 * Features:
 * - Offline-first: Uses Firestore's local cache for immediate UI updates.
 * - Minimal Cost: Queries use Snapshot listeners or specific Sources where possible.
 * - Coroutine-based: Non-blocking API using kotlinx-coroutines-play-services.
 */
@Singleton
class FirebaseMemoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager
) {
    private val TAG = "MemoryRepository"

    private fun getUserDoc() = authManager.userId?.let {
        firestore.collection("users").document(it)
    } ?: throw IllegalStateException("User must be logged in to access memories")

    /**
     * Saves a long-term memory for the user.
     */
    suspend fun saveMemory(text: String, tags: List<String> = emptyList()) {
        try {
            val memory = Memory(
                text = text,
                timestamp = Timestamp.now(),
                tags = tags.map { it.lowercase().trim() },
                userId = authManager.userId ?: ""
            )
            getUserDoc().collection("memories").add(memory).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving memory", e)
        }
    }

    /**
     * Retrieves memories relevant to a query string.
     * Note: Firestore doesn't support full-text search. This implementation
     * filters by tags or retrieves recent context for local processing.
     */
    suspend fun getRelevantMemories(query: String, limit: Long = 5): List<Memory> {
        return try {
            val keywords = query.lowercase().split(" ").filter { it.length > 3 }

            val collection = getUserDoc().collection("memories")

            // Try to find memories matching tags first (efficient indexing)
            if (keywords.isNotEmpty()) {
                val snapshot = collection
                    .whereArrayContainsAny("tags", keywords.take(10)) // Firestore limit
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get(Source.DEFAULT) // Use CACHE if offline, SERVER if online
                    .await()

                val memories = snapshot.toObjects(Memory::class.java)
                if (memories.isNotEmpty()) return memories
            }

            // Fallback: Just get the most recent memories for context
            collection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .toObjects(Memory::class.java)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching relevant memories", e)
            emptyList()
        }
    }

    /**
     * Appends a message to a specific conversation thread.
     */
    suspend fun saveMessage(conversationId: String, text: String, sender: String) {
        try {
            val message = ConversationMessage(
                text = text,
                sender = sender,
                timestamp = Timestamp.now()
            )
            getUserDoc()
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
     * Retrieves the conversation history for context.
     */
    suspend fun getConversationHistory(conversationId: String, limit: Long = 20): List<ConversationMessage> {
        return try {
            getUserDoc()
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(limit)
                .get()
                .await()
                .toObjects(ConversationMessage::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching history", e)
            emptyList()
        }
    }
}
