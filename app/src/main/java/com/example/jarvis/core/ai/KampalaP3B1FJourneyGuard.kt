package com.example.jarvis.core.ai

/**
 * P3-B1F: Conservative natural-language multi-stage journey guard.
 *
 * This does not attempt to geocode, reorder stops, or infer
 * the user's starting location.
 *
 * A clear sequence of movement actions should never fall
 * through to a category-only response or unrelated AI output.
 */
internal object KampalaP3B1FJourneyGuard {

    private val sequenceMarkers = Regex(
        """\b(?:first|then|before|after|finally)\b""",
        RegexOption.IGNORE_CASE
    )

    private val movementActions = Regex(
        """\b(?:get\s+to|head\s+to|go\s+to|travel\s+to|""" +
            """pick\s+up|drop\s+(?:off|him|her|them)|""" +
            """stop\s+at|proceed(?:ing)?)\b""",
        RegexOption.IGNORE_CASE
    )

    fun noticeOrNull(message: String): String? {
        val text = message.trim()

        if (text.length !in 30..1000) return null

        val sequences = sequenceMarkers.findAll(text).count()
        val movements = movementActions.findAll(text).count()

        if (sequences < 2 || movements < 2) return null

        // P3-B2A: Structured plan where evidence is sufficient.
        // Existing safety clarification remains the fallback.
        val plan = KampalaP3B2ItineraryPlanner.extract(text)
        if (plan != null) {
            return KampalaP3B2ItineraryPlanner.describe(plan)
        }
        return buildString {
            appendLine("I understand this as a multi-stop journey.")
            appendLine()
            appendLine(
                "Your message includes several travel activities " +
                    "that need to happen in sequence."
            )
            appendLine()
            appendLine(
                "I have not yet confirmed your starting location, " +
                    "all intermediate stops, or the final destination."
            )
            appendLine(
                "A restaurant pickup is part of the journey, " +
                    "not a request to place a food order."
            )
            appendLine()
            appendLine(
                "Please confirm your starting point and the " +
                    "specific places for any flexible stops, " +
                    "such as a restaurant."
            )
            appendLine(
                "You can also state the ordered journey as: " +
                    "from [origin] via [first stop] via " +
                    "[next stop] to [destination]."
            )
            append(
                "No route, travel time, booking, or purchase " +
                    "has been generated."
            )
        }
    }
}