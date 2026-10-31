package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationDestinationSafetyPolicyTest {

    private fun proposal(
        relation: ConversationInterpretationContract.Relation,
        field: ConversationTaskState.Field?
    ) = ConversationInterpretationContract.Proposal(
        relation = relation,
        field = field,
        value = if (field == null) null else "Nakasero"
    )

    @Test
    fun continuationCannotReplaceExistingDestination() {
        val result = ConversationDestinationSafetyPolicy.evaluate(
            proposal(
                ConversationInterpretationContract.Relation.CONTINUE,
                ConversationTaskState.Field.DESTINATION
            ),
            destinationAlreadySet = true
        )

        assertEquals(
            ConversationDestinationSafetyPolicy.Decision.REJECT_DESTINATION_REPLACEMENT,
            result
        )
    }

    @Test
    fun continuationCanSuggestArea() {
        val result = ConversationDestinationSafetyPolicy.evaluate(
            proposal(
                ConversationInterpretationContract.Relation.CONTINUE,
                ConversationTaskState.Field.SUGGESTED_AREA
            ),
            destinationAlreadySet = true
        )

        assertEquals(
            ConversationDestinationSafetyPolicy.Decision.ALLOW,
            result
        )
    }

    @Test
    fun correctionCanProposeDestinationChange() {
        val result = ConversationDestinationSafetyPolicy.evaluate(
            proposal(
                ConversationInterpretationContract.Relation.CORRECTION,
                ConversationTaskState.Field.DESTINATION
            ),
            destinationAlreadySet = true
        )

        assertEquals(
            ConversationDestinationSafetyPolicy.Decision.ALLOW,
            result
        )
    }

    @Test
    fun continuationCanSetMissingDestination() {
        val result = ConversationDestinationSafetyPolicy.evaluate(
            proposal(
                ConversationInterpretationContract.Relation.CONTINUE,
                ConversationTaskState.Field.DESTINATION
            ),
            destinationAlreadySet = false
        )

        assertEquals(
            ConversationDestinationSafetyPolicy.Decision.ALLOW,
            result
        )
    }
}