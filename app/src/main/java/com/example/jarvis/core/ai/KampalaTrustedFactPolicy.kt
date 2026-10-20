package com.example.jarvis.core.ai

/**
 * Small curated baseline of stable Kampala facts.
 *
 * This is not a live directory or source-retrieval service.
 * Expand only through independently checked facts with
 * explicit provenance in a future knowledge-store revision.
 */
internal object KampalaTrustedFactPolicy {

    private data class TrustedFact(
        val id: String,
        val statement: String
    )

    private val kampalaLocation = TrustedFact(
        id = "kampala.uganda.capital",
        statement = "Kampala is the capital city of Uganda, " +
            "a country in East Africa."
    )

    private val locationQuestions = listOf(
        Regex(
            """^\s*where\s+is\s+kampala\s*[?.!]*\s*$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^\s*where\s+is\s+kampala\s+located\s*[?.!]*\s*$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^\s*which\s+country\s+is\s+kampala\s+in\s*[?.!]*\s*$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^\s*what\s+country\s+is\s+kampala\s+in\s*[?.!]*\s*$""",
            RegexOption.IGNORE_CASE
        )
    )

    /**
     * Curated answer for an explicitly supported factual request.
     *
     * Return null for anything outside the baseline. In particular,
     * do not guess hills, routes, businesses, or administrative facts.
     */
    fun answer(query: String): String? {
        return if (locationQuestions.any { it.matches(query) }) {
            kampalaLocation.statement
        } else {
            null
        }
    }

    /**
     * Reuse baseline facts when an unverified model response
     * would otherwise be displayed.
     *
     * Tool actions are excluded by the controller.
     */
    fun review(query: String, modelAnswer: String): String {
        return answer(query) ?: modelAnswer
    }
}