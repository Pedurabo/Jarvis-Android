package com.example.jarvis.core.ai

/**
 * Matches complete real-time keywords, not substrings.
 *
 * This policy is a routing hint only.
 * It never authorizes device actions.
 */
internal object RealTimeQueryPolicy {

    private val keywords = Regex(
        """\b(?:weather|time|date|status|battery|location|currently|now)\b""",
        RegexOption.IGNORE_CASE
    )

    fun matches(query: String): Boolean {
        return keywords.containsMatchIn(query)
    }
}