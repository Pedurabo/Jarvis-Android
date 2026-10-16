package com.example.jarvis.core.ai.agents

import com.example.jarvis.core.ai.RetrievalManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryAgent @Inject constructor(
    private val retrievalManager: RetrievalManager
) : Agent {
    override val name: String = "MemoryAgent"

    override suspend fun execute(input: String, context: String): AgentResult {
        // MemoryAgent specifically fetches more relevant long-term context if needed
        val memories = retrievalManager.getRelevantMemories(10)
        val memoryText = memories.joinToString("\n") { "- ${it.text}" }

        return AgentResult(
            output = memoryText,
            metadata = mapOf("memory_count" to memories.size)
        )
    }
}
