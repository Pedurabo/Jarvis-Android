package com.example.jarvis.core.network

object KampalaKnowledgeQueryRoute {

    enum class Topic {
        HISTORY
    }

    private val historyPatterns = listOf(
        Regex(
            """^(?:tell me about|explain|describe)\s+(?:the\s+)?(?:history|historical background)\s+of\s+kampala$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^(?:tell me about|explain|describe)\s+(?:kampala's|kampala)\s+(?:history|historical background)$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^(?:what is|what's)\s+(?:the\s+)?history\s+of\s+kampala$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^(?:give me|provide)\s+(?:a\s+|an\s+)?(?:brief\s+|short\s+)?(?:history|historical overview)\s+of\s+kampala$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^(?:the\s+)?history\s+of\s+kampala$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^kampala\s+history$""",
            RegexOption.IGNORE_CASE
        )
    )

    fun resolve(input: String): Topic? {
        val normalized = input.trim()
            .trimEnd('.', '?', '!')
            .trim()

        if (normalized.isBlank()) return null

        return if (historyPatterns.any { it.matches(normalized) }) {
            Topic.HISTORY
        } else {
            null
        }
    }
}