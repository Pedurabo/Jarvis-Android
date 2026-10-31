package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.4-B1
 *
 * Pure eligibility gate. Does not execute inference,
 * mutate conversational state, or override protected routes.
 */
internal object ConversationInterpretationEligibility {

    enum class Decision {
        ELIGIBLE,
        DISABLED,
        NO_ACTIVE_TASK,
        PROTECTED_ROUTE,
        INVALID_MESSAGE,
        STANDALONE_MESSAGE
    }

    fun evaluate(
        task: ConversationTaskState.Task?,
        message: String,
        explicitlyEnabled: Boolean,
        protectedRoute: Boolean
    ): Decision {
        if (!explicitlyEnabled) {
            return Decision.DISABLED
        }

        if (protectedRoute) {
            return Decision.PROTECTED_ROUTE
        }

        if (task == null) {
            return Decision.NO_ACTIVE_TASK
        }

        val text = message.trim()

        if (text.isBlank() ||
            text.length > 180 ||
            text.any { it.isISOControl() }
        ) {
            return Decision.INVALID_MESSAGE
        }

        // Conservative upper bound, not semantic classification.
        // Long requests should follow normal routing.
        val words = text.split(Regex("\\s+"))

        if (words.size > 25) {
            return Decision.STANDALONE_MESSAGE
        }

        return Decision.ELIGIBLE
    }
}