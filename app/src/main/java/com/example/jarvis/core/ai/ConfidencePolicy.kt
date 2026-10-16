package com.example.jarvis.core.ai

/**
 * Conservative response-confidence classification.
 *
 * This is not factual verification or a calibrated probability.
 * HIGH is deliberately reserved for future verified-evidence logic.
 */
object ConfidencePolicy {

    fun classify(
        usable: Boolean,
        explicitlyUncertain: Boolean
    ): ConfidenceLevel {
        return when {
            !usable -> ConfidenceLevel.LOW
            explicitlyUncertain -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.MEDIUM
        }
    }
}