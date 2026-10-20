package com.example.jarvis.core.network

/**
 * M1F-I3C.120N.2
 *
 * Routes explicit named-destination requests through the existing
 * source-grounded Places service.
 *
 * This component does not contain addresses, perform bookings,
 * silently correct speech recognition, or provide directions.
 */
object KampalaNamedDestinationRoute {

    private val namedRequest = Regex(
        """^(?:where\s+is|where\s+do\s+i\s+find|where\s+can\s+i\s+find|find|locate|show\s+me|search\s+for)\s+(?:the\s+)?(.+?)\??$""",
        RegexOption.IGNORE_CASE
    )

    private val namedEnding = Regex(
        """\b(mall|hotel|hospital|mosque|centre|center|taxi\s+park|market|museum|stadium|tower|plaza)\b$""",
        RegexOption.IGNORE_CASE
    )

    private val correctionPattern = Regex(
        """^(?:where\s+is\s+|where\s+do\s+i\s+find\s+|find\s+|locate\s+)?(?:the\s+)?new\s+tax\s+park(?:\s+in\s+kampala)?\??$""",
        RegexOption.IGNORE_CASE
    )

    private val excluded = setOf(
        "hotel",
        "hospital",
        "mall",
        "mosque",
        "market",
        "museum",
        "centre",
        "center",
        "taxi park",
        "kampala",
        "old kampala"
    )

    fun resolve(input: String): KampalaPlacesQueryRoute.SearchRequest? {
        val text = input.trim()
        if (text.isEmpty()) return null
        if (correctionPattern.matches(text)) return null

        // Natural destination phrases observed during Samsung testing.
        val phraseNormalized = text
            .lowercase()
            .trimEnd('?')
            .trim()
            .replace(Regex("""\s+"""), " ")

        if (
            phraseNormalized == "new taxi park in kampala" ||
            phraseNormalized == "location of new taxi park kampala"
        ) {
            return KampalaPlacesQueryRoute.SearchRequest(
                query = "New Taxi Park"
            )
        }
        val match = namedRequest.matchEntire(text) ?: return null

        val candidate = match.groupValues[1]
            .trim()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""\s+in\s+kampala$""", RegexOption.IGNORE_CASE), "")
            .trim()

        val normalized = candidate.lowercase()

        if (normalized in excluded) return null
        if (candidate.length !in 6..70) return null
        if (!candidate.matches(Regex("""[A-Za-z][A-Za-z0-9 '\-&]+"""))) {
            return null
        }

        val recognizedName =
            normalized == "kingdom kampala" ||
                namedEnding.containsMatchIn(candidate)

        if (!recognizedName) return null

        return KampalaPlacesQueryRoute.SearchRequest(
            query = candidate
        )
    }

    fun notice(input: String): String? {
        if (!correctionPattern.matches(input.trim())) return null

        return "Did you mean New Taxi Park in Kampala? " +
            "Please confirm the destination and search again using " +
            "New Taxi Park. I have not automatically changed your " +
            "request, searched another place, or confirmed an address."
    }
}
