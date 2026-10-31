package com.example.jarvis.core.ai

/**
 * Controls explicit Gemma interpretation experiments.
 * Never authorizes task mutation or external actions.
 */
internal object ConversationInterpretationProbePolicy {

    fun prepare(
        task: ConversationTaskState.Task?,
        message: String,
        explicitlyEnabled: Boolean
    ): String? {
        if (!explicitlyEnabled || task == null) return null

        if (task.goal.isBlank() || task.goal.length > 300) {
            return null
        }

        if (message.isBlank() || message.length > 500) {
            return null
        }

        if (task.fields.size > 6) return null

        if (task.fields.values.any {
                it.value.length > 200
            }
        ) {
            return null
        }

        val prompt = ConversationInterpretationContract.prompt(
            task = task,
            message = message
        )

        return prompt.takeIf {
            it.isNotBlank() && it.length <= 8000
        }
    }

    fun validate(
        raw: String?
    ): ConversationInterpretationContract.Proposal? {
        if (raw.isNullOrBlank()) return null

        return ConversationInterpretationContract.parse(raw)
    }
}