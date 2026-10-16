package com.example.jarvis.core.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conservative UI confidence assessment.
 *
 * MEDIUM indicates basic response usability, not factual verification.
 * HIGH requires a future evidence-verification policy.
 *
 * No additional language-model inference is performed.
 */
@Singleton
class ConfidenceScorer @Inject constructor() {

    suspend fun calculateConfidence(
        query: String,
        response: String,
        retrievedDataCount: Int,
        toolUsageCount: Int
    ): ConfidenceResult {
        val usable = ModelOutputGuard.isUsable(response)

        val normalized = response.trimStart().lowercase()

        val explicitlyUncertain = listOf(
            "i don't know",
            "i do not know",
            "i'm not sure",
            "i am not sure",
            "i cannot verify",
            "i can't verify",
            "i cannot confirm"
        ).any { phrase ->
            normalized == phrase ||
                normalized.startsWith("$phrase ") ||
                normalized.startsWith("$phrase,") ||
                normalized.startsWith("$phrase.") ||
                normalized.startsWith("$phrase:")
        }

        val level = ConfidencePolicy.classify(
            usable = usable,
            explicitlyUncertain = explicitlyUncertain
        )

        // Display-category scores, not calibrated probabilities.
        val score = when (level) {
            ConfidenceLevel.LOW -> 0.1f
            ConfidenceLevel.MEDIUM -> 0.5f
            ConfidenceLevel.HIGH -> 0.7f
        }

        val reasons = when {
            !usable ->
                listOf("The response failed basic usability checks.")

            explicitlyUncertain ->
                listOf("The response explicitly expresses uncertainty.")

            else ->
                listOf(
                    "The response passed basic usability checks.",
                    "Factual correctness has not been independently verified."
                )
        }

        return ConfidenceResult(level, score, reasons)
    }
}