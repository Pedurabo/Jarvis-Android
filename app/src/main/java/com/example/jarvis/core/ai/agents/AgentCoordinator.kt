package com.example.jarvis.core.ai.agents

import android.util.Log
import com.example.jarvis.core.ai.IntentAnalyzer
import com.example.jarvis.core.ai.QueryType
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates multiple agents to solve a user request.
 */
@Singleton
class AgentCoordinator @Inject constructor(
    private val memoryAgent: MemoryAgent,
    private val reasoningAgent: ReasoningAgent,
    private val toolAgent: ToolAgent,
    private val intentAnalyzer: IntentAnalyzer
) {
    private val TAG = "AgentCoordinator"

    suspend fun coordinate(query: String, onUpdate: (String) -> Unit): String {
        Log.d(TAG, "Coordinating agents for query: ${query.take(20)}...")

        // 1. Memory Agent: Gather background context
        onUpdate("Recalling context...")
        val memoryResult = memoryAgent.execute(query, "")
        val context = memoryResult.output

        // 2. Intent Analysis (Small internal step for the coordinator)
        val intent = intentAnalyzer.analyze(query)

        // 3. Reasoning Agent: Decide on a plan
        onUpdate("Thinking...")
        val reasoningResult = reasoningAgent.execute(query, context)
        var currentOutput = reasoningResult.output

        // 4. Check for tool usage requirement in the reasoning output
        if (isToolAction(currentOutput)) {
            onUpdate("Executing tool...")
            val toolResult = toolAgent.execute(currentOutput, context)

            // Final synthesis pass with tool results
            onUpdate("Finalizing...")
            val finalReasoning = reasoningAgent.execute(
                input = "Summarize this tool result for the user: ${toolResult.output}",
                context = context
            )
            currentOutput = finalReasoning.output
        }

        return currentOutput
    }

    private fun isToolAction(output: String): Boolean {
        return try {
            val json = JSONObject(output)
            json.has("action") && json.getString("action") != "FINAL_ANSWER"
        } catch (e: Exception) {
            false
        }
    }
}
