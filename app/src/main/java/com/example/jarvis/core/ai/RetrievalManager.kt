package com.example.jarvis.core.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * RetrievalManager is responsible for fetching relevant context/memories
 * to be used in the RAG (Retrieval-Augmented Generation) process.
 */
@Singleton
class RetrievalManager @Inject constructor(
    private val firebaseMemoryRepository: FirebaseMemoryRepository
) {
    /**
     * Fetches relevant memories based on the query.
     */
    suspend fun getRelevantMemories(limit: Long = 5): List<MemoryEntity> {
        return firebaseMemoryRepository.getRelevantMemories(limit)
    }

    /**
     * Fetches relevant context as a formatted string.
     */
    suspend fun getRelevantContext(query: String): String {
        val memories = getRelevantMemories(5)
        return if (memories.isEmpty()) {
            "No relevant past memories found."
        } else {
            memories.joinToString("\n") { "- ${it.text}" }
        }
    }
}
