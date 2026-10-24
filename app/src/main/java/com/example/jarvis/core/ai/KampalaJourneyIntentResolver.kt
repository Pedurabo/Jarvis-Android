package com.example.jarvis.core.ai

/**
 * Resolves explicitly supplied two-point journeys.
 *
 * This component does not verify locations, calculate routes,
 * retrieve traffic conditions, or invoke Android actions.
 */
internal object KampalaJourneyIntentResolver {

    data class Journey(
        val origin: String,
        val destination: String
    )

    // CDI.5.53-C5: Accept "currently in" without a pronoun.
    private val patterns = listOf(
        Regex(
            """^(?:(?:(?:i'm|i\s+am|am)\s+)?(?:currently\s+)?(?:in|at)\s+)(.+?)\s*,?\s*(?:and\s+)?(?:i\s+)?(?:need|want|have)\s+to\s+(?:get|go)\s+to\s+(.+?)$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^(?:how\s+(?:do|can)\s+i\s+get\s+)?from\s+(.+?)\s+to\s+(.+?)$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^directions\s+from\s+(.+?)\s+to\s+(.+?)$""",
            RegexOption.IGNORE_CASE
        )
    )

    private val placePattern = Regex(
        """[A-Za-z][A-Za-z '\-]{1,78}"""
    )

    fun resolve(input: String): Journey? {
        val text = input.trim()
            .trimEnd('.', '?', '!')
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (text.isBlank()) return null

        for (pattern in patterns) {
            val match = pattern.matchEntire(text) ?: continue

            val origin = match.groupValues[1].trim()
            val destination = match.groupValues[2].trim()

            if (
                origin.length !in 2..80 ||
                destination.length !in 2..80 ||
                !placePattern.matches(origin) ||
                !placePattern.matches(destination) ||
                origin.equals(destination, ignoreCase = true)
            ) {
                return null
            }

            return Journey(
                origin = origin,
                destination = destination
            )
        }

        return null
    }
}