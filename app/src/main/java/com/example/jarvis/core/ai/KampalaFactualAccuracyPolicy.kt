package com.example.jarvis.core.ai

object KampalaFactualAccuracyPolicy {

    private val locationQuestion = Regex(
        """\bkampala\b""",
        RegexOption.IGNORE_CASE
    )

    private val falseIndianLocation = Regex(
        """\bkampala\s+(?:is|was|lies|sits|stands)\b[^.!?\n]{0,140}\b(?:india|indian|kaveri|cauvery)\b""",
        RegexOption.IGNORE_CASE
    )

    // Reject false country attribution, including the Samsung
    // observation that placed Kampala in Cambodia.
    private val falseCambodianLocation = Regex(
        """\bkampala\s*,\s*cambodia\b|\bkampala\b[^.!?\n]{0,130}\b(?:in|within)\s+cambodia\b""",
        RegexOption.IGNORE_CASE
    )
    private val falseIndianRiver = Regex(
        """\b(?:kaveri|cauvery)\s+river\b""",
        RegexOption.IGNORE_CASE
    )

    private val historicalQuestion = Regex(
        """\b(?:history|historical|origin|founded|founding)\b""",
        RegexOption.IGNORE_CASE
    )

    fun shouldReject(query: String, answer: String): Boolean {
        if (!locationQuestion.containsMatchIn(query)) {
            return false
        }

        if (falseCambodianLocation.containsMatchIn(answer)) {
            return true
        }

        if (falseIndianLocation.containsMatchIn(answer)) {
            return true
        }

        return historicalQuestion.containsMatchIn(query) &&
            falseIndianRiver.containsMatchIn(answer)
    }

    const val SAFE_RESPONSE =
        "I can't reliably verify that account of Kampala's history. " +
        "Kampala is the capital of Uganda, in East Africa. " +
        "Please consult a trusted Uganda-specific historical source " +
        "for a detailed account."
}