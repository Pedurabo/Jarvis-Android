package com.example.jarvis.core.ai

/**
 * Recognizes explicit, simple requests to open the phone dialer.
 *
 * This class does not invoke a skill, launch Android intents,
 * or authorize any phone action.
 */
object DialCommandRoute {

    private val commandPattern = Regex(
        """^\s*(?:please\s+)?dial\s+(\+[1-9][0-9]{7,14})[.]?\s*$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Returns the requested number, or null if this is not
     * a supported explicit dialing command.
     *
     * Actual authorization remains in SkillRegistry.
     */
    fun resolve(command: String): String? {
        val match = commandPattern.matchEntire(command)
            ?: return null

        return match.groupValues[1]
    }
}