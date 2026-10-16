package com.example.jarvis.core.ai

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ModelRouter: The intelligent routing layer that decides whether to use
 * the on-device LLM (Local) or the high-intelligence Cloud LLM.
 */
@Singleton
class ModelRouter @Inject constructor(
    private val localLLM: LLMManager,
    private val cloudLLM: CloudLLMManager
) {
    private val TAG = "ModelRouter"

    enum class Route { LOCAL, CLOUD }

    /**
     * Decision logic for routing the query.
     */
    suspend fun route(
        query: String,
        isRetry: Boolean = false,
        previousConfidence: ConfidenceLevel? = null
    ): Route {
        // 1. Escalation rule: Low confidence in previous attempt triggers Cloud
        if (isRetry && previousConfidence == ConfidenceLevel.LOW) {
            Log.d(TAG, "Routing to CLOUD: Escalating due to low confidence.")
            return Route.CLOUD
        }

        // 2. Real-time query detection (Priority for Local if it's a simple tool request)
        if (isRealTimeQuery(query)) {
            Log.d(TAG, "Routing to LOCAL: Real-time query (speed prioritized for tool usage).")
            return Route.LOCAL
        }

        // 3. Complexity-based routing
        val complexity = analyzeComplexity(query)
        return if (complexity > 6) {
            Log.d(TAG, "Routing to CLOUD: Complex reasoning detected ($complexity).")
            Route.CLOUD
        } else {
            Log.d(TAG, "Routing to LOCAL: Simple query.")
            Route.LOCAL
        }
    }

    /**
     * Executes the generation task on the selected model.
     */
    suspend fun generateResponse(prompt: String, route: Route): String {
        return when (route) {
            Route.LOCAL -> localLLM.generateResponse(prompt)
            Route.CLOUD -> cloudLLM.generateResponse(prompt)
        }
    }

    private fun analyzeComplexity(query: String): Int {
        var score = 0
        val q = query.lowercase()

        // Complex reasoning markers
        val complexMarkers = listOf("compare", "analyze", "why", "impact", "relationship", "consequences", "history of")
        score += complexMarkers.count { q.contains(it) } * 2

        // Multi-part query markers
        val connectors = listOf(" and also ", " then ", " because ", " however ")
        score += connectors.count { q.contains(it) }

        // Length as a proxy for complexity
        if (query.length > 150) score += 2

        return score
    }

    private fun isRealTimeQuery(query: String): Boolean {
        return RealTimeQueryPolicy.matches(query)
    }
}
