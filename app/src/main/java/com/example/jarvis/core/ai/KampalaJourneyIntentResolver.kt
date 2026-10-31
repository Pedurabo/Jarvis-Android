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

    // P3-A1: Stops are ordered, user-supplied names.
    // They are not geocoded or verified locations.
    data class Journey(
        val origin: String,
        val destination: String,
        val stops: List<String> = emptyList()
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
            """^(?:(?:how\s+(?:do|can)\s+i\s+get\s+)?|take\s+me\s+)from\s+(.+?)\s+to\s+(.+)$""",
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
        """[A-Za-z][A-Za-z0-9 ,'\-]{1,79}"""
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

    // CDI.5.56-A: Recognize a directions request with no origin.
    // This does not verify the supplied destination.
    private val destinationOnlyPatterns = listOf(
        // CDI.5.62: Natural requests for directions.
        Regex(
            """^(?:(?:can|could)\s+you\s+(?:please\s+)?get\s+me\s+directions\s+to|please\s+help\s+me\s+get\s+to|(?:can|could)\s+you\s+help\s+me\s+get\s+to)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^(?:i\s+)?(?:need|want|have)\s+to\s+(?:get|go|reach)(?:\s+to)?\s+(.+)$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^(?:(?:can|could)\s+you|please)\s+(?:direct|guide|take)\s+me\s+to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
    )

    fun destinationWithoutOrigin(input: String): String? {
        val normalized = input.trim()
            .replace(whitespace, " ")
            .trimEnd('.', '?', '!')
            .trim()

        for (pattern in destinationOnlyPatterns) {
            val match = pattern.matchEntire(normalized) ?: continue
            val destination = cleanPlace(match.groupValues[1])

            if (destinationFirstJourney.matches(normalized)) {
                return null
            }

            if (
                destination.length in 2..80 &&
                placePattern.matches(destination)
            ) {
                return destination
            }
        }

        return null
    }
    // CDI.5.57-C: A complete journey may state destination first.
    // The origin must be explicitly introduced by "from".
    private val destinationFirstJourney = Regex(
        """^(?:i\s+)?(?:need|want|have)\s+to\s+(?:get|go|reach)(?:\s+to)?\s+(.+?)\s+from\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    // P3-A1: Deliberately limited explicit multi-stop syntax.
    // No coordinates, live route, duration or inferred campus.
    private val explicitMultiStop = Regex(
        """^from\s+(.+?)\s+via\s+(.+?)\s+to\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private fun resolveExplicitMultiStop(input: String): Journey? {
        val match = explicitMultiStop.matchEntire(input) ?: return null

        val base = createJourney(
            match.groupValues[1],
            match.groupValues[3]
        ) ?: return null

        val stops = match.groupValues[2]
            .split(Regex("""\s+via\s+""", RegexOption.IGNORE_CASE))
            .map(::cleanPlace)

        if (
            stops.isEmpty() ||
            stops.any {
                it.length !in 2..80 || !placePattern.matches(it)
            }
        ) {
            return null
        }

        val names = listOf(base.origin) + stops + base.destination

        if (names.distinctBy { it.lowercase() }.size != names.size) {
            return null
        }

        return base.copy(stops = stops)
    }

    fun resolve(input: String): Journey? {
        val normalized = input.trim()
            .replace(whitespace, " ")
            .trim()

        if (normalized.isBlank()) return null

        // Multi-stop requests must never be treated as a
        // two-point journey by falling through to legacy parsing.
        if (explicitMultiStop.matches(normalized.trimEnd('.', '?', '!'))) {
            return resolveExplicitMultiStop(
                normalized.trimEnd('.', '?', '!')
            )
        }

        val destinationFirst = destinationFirstJourney.matchEntire(
            normalized.trimEnd('.', '?', '!')
        )

        if (destinationFirst != null) {
            return createJourney(
                destinationFirst.groupValues[2],
                destinationFirst.groupValues[1]
            )
        }

        // CDI.6.0-A6.9-D4E-R1:
        // Destination-first directions with an explicit origin.
        // Example: "How do I get to Acacia Mall from Ntinda?"
        val directionsFrom = Regex(
            """^how\s+(?:do|can)\s+i\s+get\s+to\s+(.+?)\s+from\s+(.+)$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(normalized.trimEnd('.', '?', '!'))

        if (directionsFrom != null) {
            return createJourney(
                directionsFrom.groupValues[2],
                directionsFrom.groupValues[1]
            )
        }

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