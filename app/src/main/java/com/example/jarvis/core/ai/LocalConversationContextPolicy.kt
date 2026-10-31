package com.example.jarvis.core.ai

/**
 * Prepares recent locally stored messages for model context.
 *
 * The UI saves the current user message before invoking the
 * controller. Avoid presenting that message twice in the prompt.
 *
 * This policy does not persist state, trust source claims, or
 * authorize any action.
 */
internal object LocalConversationContextPolicy {

    fun prepare(
        storedMessages: List<Pair<String, String>>,
        currentQuery: String
    ): String {
        val previousMessages =
            if (
                storedMessages.isNotEmpty() &&
                storedMessages.last().first.equals(
                    "user", ignoreCase = true
                ) &&
                storedMessages.last().second == currentQuery
            ) {
                storedMessages.dropLast(1)
            } else {
                storedMessages
            }

        return PromptHistoryPolicy.format(
            previousMessages.takeLast(6)
        )
    }
}