package com.example.jarvis.core.ai

/**
 * Identifies programming tasks that warrant extra accuracy checks.
 *
 * Classification does not select a provider or authorize tools.
 */
internal object TechnicalTaskPolicy {

    enum class Kind {
        GENERAL,
        EXPLANATION,
        CODE_TASK
    }

    private val programmingContext = Regex(
        """\b(?:kotlin|java|python|javascript|typescript|android|compose|gradle|programming|code|function|functions|class|classes|compiler|compilation|list|mutablelist|listof|api|coroutine|suspend)\b""",
        RegexOption.IGNORE_CASE
    )

    private val codeTask = Regex(
        """\b(?:write|generate|implement|debug|fix|refactor|compile|correct|review|build|create)\b""",
        RegexOption.IGNORE_CASE
    )

    private val explanation = Regex(
        """^(?:please\s+)?(?:explain|describe|what is|what are|what's|how does|how do|how do i|tell me about)\b""",
        RegexOption.IGNORE_CASE
    )

    fun classify(query: String): Kind {
        val text = query.trim()
        if (text.isEmpty()) return Kind.GENERAL

        if (!programmingContext.containsMatchIn(text)) {
            return Kind.GENERAL
        }

        if (codeTask.containsMatchIn(text)) {
            return Kind.CODE_TASK
        }

        return if (explanation.containsMatchIn(text)) {
            Kind.EXPLANATION
        } else {
            Kind.GENERAL
        }
    }
}