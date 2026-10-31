package com.example.jarvis.core.ai

/**
 * Conservative boundary for proposed destination updates.
 *
 * This policy does not interpret natural-language messages.
 * It only decides whether a model-proposed destination
 * update is safe to apply to an existing destination.
 *
 * CDI.6.0-A6.3-C: Not yet wired into live task updates.
 */
internal object ConversationDestinationSafetyPolicy {

    enum class Decision {
        ALLOW,
        REJECT_DESTINATION_REPLACEMENT
    }

    fun evaluate(
        proposal: ConversationInterpretationContract.Proposal,
        destinationAlreadySet: Boolean
    ): Decision {
        val destinationUpdate =
            proposal.field == ConversationTaskState.Field.DESTINATION

        val continuation =
            proposal.relation ==
                ConversationInterpretationContract.Relation.CONTINUE

        if (destinationAlreadySet && destinationUpdate && continuation) {
            return Decision.REJECT_DESTINATION_REPLACEMENT
        }

        return Decision.ALLOW
    }
}