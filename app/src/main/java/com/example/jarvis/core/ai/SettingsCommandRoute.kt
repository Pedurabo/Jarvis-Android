package com.example.jarvis.core.ai

/**
 * Recognizes only an explicit request to open Android Settings.
 *
 * This component never launches an app or authorizes execution.
 */
object SettingsCommandRoute {

    private val settingsRequest = Regex(
        """^\s*(?:please\s+)?open\s+(?:the\s+)?(?:android\s+)?settings(?:\s+app)?(?:[.!]\s*ask\s+for\s+my\s+approval\s+before\s+doing\s+anything)?[.!]?\s*$""",
        RegexOption.IGNORE_CASE
    )

    fun isSettingsLaunch(query: String): Boolean =
        settingsRequest.matches(query)
}