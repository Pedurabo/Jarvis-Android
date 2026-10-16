package com.example.jarvis.core.ai.agents

/**
 * Base interface for all Jarvis Agents.
 */
interface Agent {
    val name: String

    /**
     * Process a request and return a result.
     * @param input The user query or current state.
     * @param context Additional context (memories, history, etc.)
     */
    suspend fun execute(input: String, context: String): AgentResult
}

data class AgentResult(
    val output: String,
    val metadata: Map<String, Any> = emptyMap()
)
