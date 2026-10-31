package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.6-F
 *
 * A standalone correction opener may be one chat turn
 * when the user continues typing their location separately.
 *
 * This classifier does not infer a location or route.
 */
internal object KampalaSplitCorrectionPolicy {

    fun isUnfinishedCorrection(message: String): Boolean {
        val cleaned = message.trim()
            .lowercase()
            .trimEnd(',', '.', '!', '?', ':', ';')
            .trim()

        return cleaned == "actually" ||
            cleaned == "no actually" ||
            cleaned == "wait actually"
    }

    fun shouldPreserveUnverifiedDestination(
        pendingDestination: String?,
        message: String
    ): Boolean =
        !pendingDestination.isNullOrBlank() &&
            isUnfinishedCorrection(message)
}