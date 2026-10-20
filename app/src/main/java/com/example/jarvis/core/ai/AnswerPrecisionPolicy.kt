package com.example.jarvis.core.ai

/**
 * Controls the requested depth of ordinary conversational answers.
 *
 * This is generation guidance, not an output truncation policy.
 * It does not verify facts, execute tools, or modify source-backed answers.
 */
internal object AnswerPrecisionPolicy {

    private val explicitDetail = Regex(
        """\b(?:in\s+detail|detailed|thorough|comprehensive|step[\s-]+by[\s-]+step|walk\s+me\s+through|full\s+explanation|long\s+answer|with\s+(?:an?\s+)?(?:code|example)|code\s+example|show\s+(?:me\s+)?(?:the\s+)?code)\b""",
        RegexOption.IGNORE_CASE
    )

    private val kotlinTopic = Regex(
        """\bkotlin\b""",
        RegexOption.IGNORE_CASE
    )

    private val valKeyword = Regex(
        """\bval\b""",
        RegexOption.IGNORE_CASE
    )

    private val varKeyword = Regex(
        """\bvar\b""",
        RegexOption.IGNORE_CASE
    )

    fun guidance(query: String): String {
        val detailed =
            explicitDetail.containsMatchIn(query) ||
                TechnicalTaskPolicy.classify(query) ==
                    TechnicalTaskPolicy.Kind.CODE_TASK

        val lengthGuidance = if (detailed) {
            """
            ANSWER LENGTH:
            The user has requested detail, examples, or implementation.
            Provide the requested depth and preserve necessary code.
            Prefer clear organization over repetition.
            """.trimIndent()
        } else {
            """
            ANSWER LENGTH:
            Give the direct answer first.
            For an ordinary explanation, normally use about 60 to 120 words.
            Start with a short definition and at most three useful points.
            Avoid unnecessary essays, repeated conclusions, and extra sections.
            If more depth would help, the user can ask a follow-up.
            Do not omit essential qualifications merely to shorten the answer.
            """.trimIndent()
        }

        val kotlinGuidance =
            if (kotlinTopic.containsMatchIn(query) &&
                (valKeyword.containsMatchIn(query) ||
                    varKeyword.containsMatchIn(query))) {
                """
                KOTLIN REFERENCE PRECISION:
                val declares a read-only variable reference that cannot be reassigned.
                var declares a variable reference that can be reassigned.
                A val reference does not automatically make the referenced object immutable.
                Avoid claiming that val always means a compile-time constant.
                Explain object mutation separately from reference reassignment.
                Do not claim that val and var inherently have different access speeds.
                """.trimIndent()
            } else {
                ""
            }

        return if (kotlinGuidance.isBlank()) {
            lengthGuidance
        } else {
            "$lengthGuidance\n\n$kotlinGuidance"
        }
    }
}
