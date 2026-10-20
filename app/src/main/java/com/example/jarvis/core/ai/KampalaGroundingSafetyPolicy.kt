package com.example.jarvis.core.ai

/**
 * Conservative safety net after verified directory and knowledge routing.
 *
 * Does not search the web, invent geographic facts, or perform actions.
 * Only handles explicit location requests for recognizable place types
 * and a narrow landmark-ranking question without verified evidence.
 */
object KampalaGroundingSafetyPolicy {

    private val locationQuestion = Regex(
        """^(?:where\s+(?:is|can\s+i\s+find|do\s+i\s+find)|what\s+is\s+the\s+location\s+of|location\s+of)\s+(?:the\s+)?(.+?)(?:\s+located)?\??$""",
        RegexOption.IGNORE_CASE
    )

    private val locationTypes = Regex(
        """\b(hospital|police\s+station|state\s+house|city\s+square|taxi\s+park|bus\s+park|clock\s+tower|museum|stadium|shopping\s+mall|government\s+office)\b""",
        RegexOption.IGNORE_CASE
    )

    private val rankingQuestion = Regex(
        """^(?:what|which)\s+is\s+(?:the\s+)?(?:tallest|highest)\s+(?:building|skyscraper)\s+in\s+kampala(?:\s+city)?\??$""",
        RegexOption.IGNORE_CASE
    )

    fun notice(input: String): String? {
        val query = input.trim().replace(Regex("""\s+"""), " ")
        if (query.isEmpty()) return null

        if (rankingQuestion.matches(query)) {
            return "I don't currently have a verified source identifying " +
                "Kampala's tallest building. I shouldn't guess or claim " +
                "that Kampala has no high-rise buildings. " +
                "A current authoritative source is needed."
        }

        val match = locationQuestion.matchEntire(query) ?: return null

        val destination = match.groupValues[1]
            .trim()
            .trimEnd('?')
            .trim()
            .removeSuffix(" located")
            .trim()

        if (destination.length !in 5..90) return null
        if (!locationTypes.containsMatchIn(destination)) return null

        // Exclude general knowledge and personal-device questions.
        if (
            Regex(
                """\b(my|your|their)\s+(?:phone|location|account)\b""",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(destination)
        ) return null

        return "I can't currently confirm the location of " +
            "$destination from my verified Kampala directory. " +
            "I won't invent an address or directions. " +
            "Please check an official listing or a trusted map " +
            "while the directory coverage is being expanded."
    }
}
