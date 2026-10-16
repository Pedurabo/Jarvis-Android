package com.example.jarvis.core.ai

import android.util.Log
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

enum class QueryType {
    FACTUAL,    // General knowledge, static facts
    REASONING,  // Complex analysis, "why", "how to"
    PERSONAL,   // About the user, their memories, or preferences
    REAL_TIME   // Requires tools (weather, time, device state)
}

data class IntentResult(
    val type: QueryType,
    val intent: String,
    val entities: List<String>,
    val strategy: String
)

/**
 * Analyzes user input to determine the best processing strategy.
 * Uses the on-device LLM for lightweight classification.
 */
@Singleton
class IntentAnalyzer @Inject constructor(
    private val llmManager: LLMManager
) {
    private val TAG = "IntentAnalyzer"

    suspend fun analyze(query: String): IntentResult {
        val systemPrompt = """
            Analyze the user query and respond strictly in JSON format.
            Types: FACTUAL, REASONING, PERSONAL, REAL_TIME.

            JSON Structure:
            {
              "type": "TYPE",
              "intent": "short description of user goal",
              "entities": ["entity1", "entity2"],
              "strategy": "LOCAL_ONLY" or "TOOL_FIRST" or "CLOUD_REQUIRED"
            }

            Examples:
            - "What is the capital of France?" -> {"type": "FACTUAL", "intent": "find capital", "entities": ["France"], "strategy": "LOCAL_ONLY"}
            - "How is the weather in Kampala?" -> {"type": "REAL_TIME", "intent": "get weather", "entities": ["Kampala"], "strategy": "TOOL_FIRST"}
            - "Why does it rain?" -> {"type": "REASONING", "intent": "explain precipitation", "entities": ["rain"], "strategy": "CLOUD_REQUIRED"}
        """.trimIndent()

        val fullPrompt = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>" +
                "<|start_header_id|>user<|end_header_id|>\n\n$query<|eot_id|>" +
                "<|start_header_id|>assistant<|end_header_id|>\n\n"

        return try {
            val response = llmManager.generateResponse(fullPrompt)
            val json = parseJsonResponse(response)

            IntentResult(
                type = QueryType.valueOf(json.optString("type", "FACTUAL")),
                intent = json.optString("intent", "General query"),
                entities = json.optJSONArray("entities")?.let { arr ->
                    List(arr.length()) { i -> arr.getString(i) }
                } ?: emptyList(),
                strategy = json.optString("strategy", "LOCAL_ONLY")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed, falling back to default.", e)
            IntentResult(QueryType.FACTUAL, "General fallback", emptyList(), "LOCAL_ONLY")
        }
    }

    private fun parseJsonResponse(response: String): JSONObject {
        return try {
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}")
            if (jsonStart != -1 && jsonEnd != -1) {
                JSONObject(response.substring(jsonStart, jsonEnd + 1))
            } else {
                JSONObject()
            }
        } catch (e: Exception) {
            JSONObject()
        }
    }
}
