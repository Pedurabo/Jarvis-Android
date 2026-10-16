package com.example.jarvis.core.ai

/**
 * A resolved application name and its known Android package.
 *
 * Resolution is not authorization.
 * No Android APIs or application launches happen here.
 */
data class AppLaunchProposal(
    val displayName: String,
    val packageName: String
)

object AppLaunchCommandRoute {

    private val supportedApps = mapOf(
        "android settings" to AppLaunchProposal(
            "Android Settings", "com.android.settings"
        ),
        "settings" to AppLaunchProposal(
            "Android Settings", "com.android.settings"
        ),
        "whatsapp" to AppLaunchProposal(
            "WhatsApp", "com.whatsapp"
        ),
        "x" to AppLaunchProposal(
            "X", "com.twitter.android"
        ),
        "twitter" to AppLaunchProposal(
            "X", "com.twitter.android"
        ),
        "youtube" to AppLaunchProposal(
            "YouTube", "com.google.android.youtube"
        )
    )

    private val commandPattern = Regex(
        """^\s*(?:please\s+)?(?:open|launch)\s+(.+?)\s*[.!]?\s*$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Recognizes explicit launch instructions for supported apps.
     * Returns null for questions, negative commands, unsupported apps,
     * and additional instructions.
     */
    fun resolve(command: String): AppLaunchProposal? {
        val match = commandPattern.matchEntire(command)
            ?: return null

        val name = match.groupValues[1]
            .trim()
            .trimEnd('.', '!')
            .trim()
            .lowercase()

        return supportedApps[name]
    }
}