package com.example.jarvis.core.ai

/**
 * Deterministic interpretation of an LLM validation response.
 *
 * Only an exact recognized label counts as a classification.
 * Unexpected text is inconclusive, not an implicit validation.
 */
object ValidationDecisionPolicy {

    enum class Decision {
        VALID,
        UNSUPPORTED,
        CONTRADICTORY,
        INCONCLUSIVE
    }

    fun parse(output: String): Decision {
        return when (output.trim().uppercase()) {
            "VALID" -> Decision.VALID
            "UNSUPPORTED" -> Decision.UNSUPPORTED
            "CONTRADICTORY" -> Decision.CONTRADICTORY
            else -> Decision.INCONCLUSIVE
        }
    }
}