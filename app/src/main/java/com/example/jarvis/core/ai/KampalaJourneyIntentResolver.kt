package com.example.jarvis.core.ai

/**
 * CDI.5.55-A: Structured recognition of explicitly supplied
 * origin-to-destination journeys.
 *
 * This resolver only extracts user-provided place text.
 * It never verifies locations, calculates routes, supplies traffic
 * conditions, or performs device actions.
 */
internal object KampalaJourneyIntentResolver {

    data class Journey(
        val origin: String,
        val destination: String
    )

    private val whitespace = Regex("""\s+""")

    private val originIntroduction = Regex(
        """^(?:(?:i'm|i\s+am|am)\s+)?(?:(?:currently|cure)\s+)?(?:in|at)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val travelRequests = listOf(
        Regex(
            """\s*,?\s*(?:and\s+)?(?:i\s+)?(?:need|want|have)\s+to\s+(?:get|go|reach)(?:\s+to)?\s+""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\s*,?\s*(?:(?:can|could)\s+you|please)\s+(?:direct|guide|take)\s+me\s+to\s+""",
            RegexOption.IGNORE_CASE
        )
    )

    private val explicitFromTo = listOf(
        Regex(
            """^(?:how\s+(?:do|can)\s+i\s+get\s+)?from\s+(.+?)\s+to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^directions\s+from\s+(.+?)\s+to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
    )

    private val politeEnding = Regex(
        """\s*,?\s*please\s+(?:direct|guide)\s+me\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val placePattern = Regex(
        """[A-Za-z][A-Za-z ,'\-]{1,79}"""
    )

    private fun cleanPlace(value: String): String =
        value.trim()
            .trim(' ', ',', '.', '?', '!')
            .replace(whitespace, " ")
            .trim()

    private fun createJourney(
        originText: String,
        destinationText: String
    ): Journey? {
        val origin = cleanPlace(originText)
        val destination = cleanPlace(
            destinationText.replace(politeEnding, "")
        )

        if (
            origin.length !in 2..80 ||
            destination.length !in 2..80 ||
            !placePattern.matches(origin) ||
            !placePattern.matches(destination) ||
            origin.equals(destination, ignoreCase = true)
        ) {
            return null
        }

        return Journey(origin, destination)
    }

    fun resolve(input: String): Journey? {
        val normalized = input.trim()
            .replace(whitespace, " ")
            .trim()

        if (normalized.isBlank()) return null

        // Explicit "from X to Y" journeys.
        for (pattern in explicitFromTo) {
            val match = pattern.matchEntire(
                normalized.trimEnd('.', '?', '!')
            ) ?: continue

            return createJourney(
                match.groupValues[1],
                match.groupValues[2]
            )
        }

        // Separate the origin phrase from travel intent.
        val originMatch = originIntroduction.matchEntire(
            normalized
        ) ?: return null

        val remainder = originMatch.groupValues[1]

        val matchedTravel = travelRequests
            .mapNotNull { pattern ->
                pattern.find(remainder)
            }
            .minByOrNull { match ->
                match.range.first
            } ?: return null

        return createJourney(
            remainder.substring(0, matchedTravel.range.first),
            remainder.substring(matchedTravel.range.last + 1)
        )
    }
}