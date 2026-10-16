package com.example.jarvis.core.ai

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight validator to ensure AI responses are grounded and consistent.
 */
@Singleton
class ResponseValidator @Inject constructor(
    private val llmManager: LLMManager
) {
    private val TAG = "ResponseValidator"

    /**
     * Validates a response against the provided context and the original query.
     */
    suspend fun validate(
        query: String,
        response: String,
        context: String
    ): ValidationResult {
        if (response.isBlank()) return ValidationResult.Invalid("Empty response")

        val systemPrompt = """
            You are a factual auditor. Verify if the Assistant's Answer is supported by the Provided Context.
            - If the Answer contains facts NOT in the Context or Query, it is UNSUPPORTED.
            - If the Answer contradicts the Context, it is CONTRADICTORY.
            - Respond ONLY with 'VALID', 'UNSUPPORTED', or 'CONTRADICTORY'.

            Context: $context
            Query: $query
        """.trimIndent()

        val userPrompt = "Assistant's Answer: $response"

        val fullPrompt = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>" +
                "<|start_header_id|>user<|end_header_id|>\n\n$userPrompt<|eot_id|>" +
                "<|start_header_id|>assistant<|end_header_id|>\n\n"

        return try {
            val result = llmManager.generateResponse(fullPrompt).uppercase().trim()
            Log.d(TAG, "Validation Result: $result")

            when (ValidationDecisionPolicy.parse(result)) {
                ValidationDecisionPolicy.Decision.VALID ->
                    ValidationResult.Valid

                ValidationDecisionPolicy.Decision.CONTRADICTORY ->
                    ValidationResult.Invalid("Response contradicts known facts.")

                ValidationDecisionPolicy.Decision.UNSUPPORTED ->
                    ValidationResult.Invalid("Response makes unsupported claims.")

                ValidationDecisionPolicy.Decision.INCONCLUSIVE ->
                    ValidationResult.Invalid("Validation result was inconclusive.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed", e)
            ValidationResult.Invalid("Validation could not be completed.")
        }
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
