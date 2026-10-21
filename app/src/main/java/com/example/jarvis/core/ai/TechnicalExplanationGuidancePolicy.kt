package com.example.jarvis.core.ai

/**
 * Improves conceptual explanations without supplying canned answers.
 * This is prompt guidance, not factual verification.
 */
internal object TechnicalExplanationGuidancePolicy {

    private val explanationStart = Regex(
        """^\s*(?:please\s+)?(?:explain|describe|define|what\s+is|what\s+are|how\s+does|how\s+do|tell\s+me\s+about)\b""",
        RegexOption.IGNORE_CASE
    )

    private val technicalSubject = Regex(
        """\b(?:kotlin|java|python|javascript|typescript|android|software|programming|dependency injection|api|compiler|coroutines?|architecture|database|algorithm|framework)\b""",
        RegexOption.IGNORE_CASE
    )

    fun guidance(query: String, toolsAvailable: Boolean): String {
        if (toolsAvailable ||
            !explanationStart.containsMatchIn(query) ||
            !technicalSubject.containsMatchIn(query)
        ) {
            return ""
        }

        return """
            TECHNICAL EXPLANATION ACCURACY:
            Begin with a short, precise definition of the concept.
            Distinguish what is essential from what is optional.
            Do not claim a technique requires reflection, runtime discovery,
            a framework, a library, or automatic configuration unless
            that requirement is intrinsic to the technique being explained.
            Differentiate manual implementation from framework assistance.
            Explain mechanisms accurately without inventing guarantees.
            Prefer one clear example over speculative implementation details.
            If uncertain about a technical fact, acknowledge the uncertainty.
            Answer the current question directly, not an earlier question.
        """.trimIndent()
    }
}