package com.example.jarvis.core.ai

/**
 * Builds safe conversational history for model prompts.
 *
 * Filters prompt context only. Persistent conversation data is unchanged.
 * This does not authorize or execute tools.
 */
internal object PromptHistoryPolicy {

    fun format(messages: List<Pair<String, String>>): String {
        return messages.takeLast(6).mapNotNull { (role, text) ->
            val normalizedRole = role.trim().lowercase()
            val normalizedText = text.trim()

            if (normalizedRole != "user" &&
                normalizedRole != "assistant") {
                return@mapNotNull null
            }

            if (normalizedText.isBlank()) {
                return@mapNotNull null
            }

            // Treat model protocol tokens as unsafe regardless of origin.
            // They should never be embedded inside a future prompt.
            if (!ModelOutputGuard.isUsable(normalizedText)) {
                return@mapNotNull null
            }

            // Large assistant responses are not reliable prompt context.
            // Keep persisted conversation history unchanged.
            if (normalizedRole == "assistant" &&
                normalizedText.length > 1200) {
                return@mapNotNull null
            }

            "$normalizedRole: $normalizedText"
        }.joinToString("\n")
    }
}