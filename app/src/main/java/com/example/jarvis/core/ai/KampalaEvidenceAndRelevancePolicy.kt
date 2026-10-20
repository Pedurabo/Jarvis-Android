package com.example.jarvis.core.ai

/**
 * Evidence and relevance safeguards for Kampala-specific questions.
 *
 * This policy does not invent geographic details, fetch external data,
 * or modify device-action pathways.
 */
internal object KampalaEvidenceAndRelevancePolicy {

    private val hillsReference = Regex(
        """\b(?:hills?\s+(?:of|in|around)\s+kampala|kampala(?:'s)?\s+hills?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val directLocationQuestion = Regex(
        """\b(?:where\s+is|location\s+of)\s+kampala\b""",
        RegexOption.IGNORE_CASE
    )

    private val internalToolContent = Regex(
        """\b(?:json\s+api|tool\s+calls?|traffic\s+tool|possible\s+solutions|code\s+snippet)\b""",
        RegexOption.IGNORE_CASE
    )

    private val ugandaReference = Regex(
        """\buganda\b""",
        RegexOption.IGNORE_CASE
    )

    private val kampalaReference = Regex(
        """\bkampala\b""",
        RegexOption.IGNORE_CASE
    )

    const val HILLS_NOTICE =
        "I don't currently have a sufficiently verified Kampala hills " +
            "reference to provide an accurate historical list and " +
            "descriptions. I won't invent hill names or geographic details."

    const val LOCATION_FAILURE =
        "I couldn't produce a sufficiently relevant answer about " +
            "Kampala's location. Please use a verified geographic source."

    /**
     * Returns a coverage notice only where verified local geographic
     * reference material is required but not available through this route.
     */
    fun notice(query: String): String? {
        return if (hillsReference.containsMatchIn(query)) {
            HILLS_NOTICE
        } else {
            null
        }
    }

    /**
     * Checks relevance of unverified model responses.
     * Unrelated ordinary technical and conversational requests pass through.
     */
    fun review(query: String, answer: String): String {
        if (!directLocationQuestion.containsMatchIn(query)) {
            return answer
        }

        if (internalToolContent.containsMatchIn(answer)) {
            return LOCATION_FAILURE
        }

        if (!kampalaReference.containsMatchIn(answer) ||
            !ugandaReference.containsMatchIn(answer)
        ) {
            return LOCATION_FAILURE
        }

        return answer
    }
}