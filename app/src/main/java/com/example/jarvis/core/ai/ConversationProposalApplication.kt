package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.4-A
 *
 * A guarded boundary between model interpretation and
 * immutable conversation task state.
 *
 * This class does not invoke a model, navigate, persist
 * state, or authorize external actions.
 */
internal object ConversationProposalApplication {

    enum class Outcome {
        APPLIED,
        REJECTED,
        NO_STATE_CHANGE
    }

    data class Result(
        val task: ConversationTaskState.Task?,
        val outcome: Outcome
    )

    fun apply(
        current: ConversationTaskState.Task?,
        proposal: ConversationInterpretationContract.Proposal?
    ): Result {
        if (current == null || proposal == null) {
            return Result(current, Outcome.NO_STATE_CHANGE)
        }

        val relation = proposal.relation

        if (relation == ConversationInterpretationContract.Relation.UNSURE ||
            relation == ConversationInterpretationContract.Relation.NEW_TASK
        ) {
            return Result(current, Outcome.NO_STATE_CHANGE)
        }

        val field = proposal.field
            ?: return Result(current, Outcome.REJECTED)

        val value = proposal.value?.trim()
            ?: return Result(current, Outcome.REJECTED)

        if (value.isBlank() ||
            value.length > 80 ||
            value.any { it.isISOControl() || it == '<' || it == '>' }
        ) {
            return Result(current, Outcome.REJECTED)
        }

        val decision = ConversationDestinationSafetyPolicy.evaluate(
            proposal = proposal,
            destinationAlreadySet =
                current.get(ConversationTaskState.Field.DESTINATION) != null
        )

        if (decision ==
            ConversationDestinationSafetyPolicy.Decision.REJECT_DESTINATION_REPLACEMENT
        ) {
            return Result(current, Outcome.REJECTED)
        }

        val existing = current.get(field)

        // No-op updates should not falsely report state changes.
        if (existing?.value == value) {
            return Result(current, Outcome.NO_STATE_CHANGE)
        }

        // A model interpretation is not proof of a verified
        // address, provider listing, or external source.
        val updated = ConversationTaskState.reduce(
            current,
            ConversationTaskState.Change.SetField(
                field = field,
                value = value,
                provenance = ConversationTaskState.Provenance.USER_SUPPLIED
            )
        )

        return if (updated != null && updated != current) {
            Result(updated, Outcome.APPLIED)
        } else {
            Result(current, Outcome.NO_STATE_CHANGE)
        }
    }
}