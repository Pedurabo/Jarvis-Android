package com.example.jarvis.core.ai

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialist to refine draft answers for maximum impact.
 * Focuses on linguistic quality, formatting, and brevity.
 */
@Singleton
class ResponseRefiner @Inject constructor(
    private val modelRouter: ModelRouter
) {
    private val TAG = "ResponseRefiner"

    /**
     * Performs a second-pass refinement on the draft.
     */
    suspend fun refine(
        query: String,
        draft: String,
        context: String,
        route: ModelRouter.Route
    ): String {
        Log.d(TAG, "Refining response with [Route: $route]...")

        val systemPrompt = """
            You are a professional editor. Your goal is to refine the Draft Answer for the User.

            Rules:
            1. IMPROVE CLARITY: Ensure the answer is easy to read and logically flows.
            2. REMOVE FLUFF: Delete any repetitive words or unnecessary filler sentences.
            3. VERIFY CORRECTNESS: Cross-reference the answer with the Provided Context.
            4. CONCISE & INTELLIGENT: Keep it brief but maintain a helpful and smart tone.
            5. NO MARKDOWN: Avoid bolding or complex markdown that might break voice output.

            Provided Context:
            $context
        """.trimIndent()

        val userPrompt = """
            User Query: $query
            Draft Answer: $draft
        """.trimIndent()

        val fullPrompt = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>" +
                "<|start_header_id|>user<|end_header_id|>\n\n$userPrompt<|eot_id|>" +
                "<|start_header_id|>assistant<|end_header_id|>\n\n"

        return try {
            val refined = modelRouter.generateResponse(fullPrompt, route)
            refined.ifBlank { draft }
        } catch (e: Exception) {
            Log.e(TAG, "Refinement failed", e)
            draft
        }
    }
}
