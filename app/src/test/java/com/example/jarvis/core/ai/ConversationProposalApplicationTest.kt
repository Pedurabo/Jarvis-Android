package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationProposalApplicationTest {

    private fun medicalTask(): ConversationTaskState.Task {
        return ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    kind = ConversationTaskState.Kind.MEDICAL_ASSISTANCE,
                    goal = "Reach medical care",
                    requiresUrgentCare = true
                ),
                ConversationTaskState.Change.SetField(
                    field = ConversationTaskState.Field.DESTINATION,
                    value = "Case Clinic",
                    provenance = ConversationTaskState.Provenance.USER_SUPPLIED
                ),
                ConversationTaskState.Change.SetField(
                    field = ConversationTaskState.Field.ORIGIN,
                    value = "Luwum Street",
                    provenance = ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )!!
    }

    private fun proposal(
        relation: ConversationInterpretationContract.Relation,
        field: ConversationTaskState.Field?,
        value: String?
    ) = ConversationInterpretationContract.Proposal(
        relation = relation,
        field = field,
        value = value
    )

    @Test
    fun rejectsGemmaDestinationMisclassification() {
        val original = medicalTask()

        val result = ConversationProposalApplication.apply(
            original,
            proposal(
                ConversationInterpretationContract.Relation.CONTINUE,
                ConversationTaskState.Field.DESTINATION,
                "Nakasero"
            )
        )

        assertEquals(
            ConversationProposalApplication.Outcome.REJECTED,
            result.outcome
        )
        assertEquals(original, result.task)
        assertEquals(
            "Case Clinic",
            result.task?.get(ConversationTaskState.Field.DESTINATION)?.value
        )
    }

    @Test
    fun allowsSuggestedAreaWithoutReplacingClinic() {
        val result = ConversationProposalApplication.apply(
            medicalTask(),
            proposal(
                ConversationInterpretationContract.Relation.CONTINUE,
                ConversationTaskState.Field.SUGGESTED_AREA,
                "Nakasero"
            )
        )

        assertEquals(
            ConversationProposalApplication.Outcome.APPLIED,
            result.outcome
        )
        assertEquals(
            "Case Clinic",
            result.task?.get(ConversationTaskState.Field.DESTINATION)?.value
        )
        assertEquals(
            "Nakasero",
            result.task?.get(ConversationTaskState.Field.SUGGESTED_AREA)?.value
        )
    }

    @Test
    fun allowsExplicitOriginCorrection() {
        val result = ConversationProposalApplication.apply(
            medicalTask(),
            proposal(
                ConversationInterpretationContract.Relation.CORRECTION,
                ConversationTaskState.Field.ORIGIN,
                "Ntinda"
            )
        )

        assertEquals(
            ConversationProposalApplication.Outcome.APPLIED,
            result.outcome
        )
        assertEquals(
            "Ntinda",
            result.task?.get(ConversationTaskState.Field.ORIGIN)?.value
        )
    }

    @Test
    fun uncertainInterpretationPreservesTask() {
        val original = medicalTask()

        val result = ConversationProposalApplication.apply(
            original,
            proposal(
                ConversationInterpretationContract.Relation.UNSURE,
                null,
                null
            )
        )

        assertEquals(
            ConversationProposalApplication.Outcome.NO_STATE_CHANGE,
            result.outcome
        )
        assertEquals(original, result.task)
    }

    @Test
    fun newTaskProposalDoesNotSilentlyClearContext() {
        val original = medicalTask()

        val result = ConversationProposalApplication.apply(
            original,
            proposal(
                ConversationInterpretationContract.Relation.NEW_TASK,
                null,
                null
            )
        )

        assertEquals(
            ConversationProposalApplication.Outcome.NO_STATE_CHANGE,
            result.outcome
        )
        assertEquals(original, result.task)
    }

    @Test
    fun invalidOrMissingValuesCannotUpdateState() {
        val original = medicalTask()

        val result = ConversationProposalApplication.apply(
            original,
            proposal(
                ConversationInterpretationContract.Relation.CORRECTION,
                ConversationTaskState.Field.ORIGIN,
                "   "
            )
        )

        assertEquals(
            ConversationProposalApplication.Outcome.REJECTED,
            result.outcome
        )
        assertEquals(original, result.task)

        assertNull(
            ConversationProposalApplication.apply(null, null).task
        )
    }

    @Test
    fun unchangedFieldDoesNotCountAsUpdate() {
        val original = medicalTask()

        val result = ConversationProposalApplication.apply(
            original,
            proposal(
                ConversationInterpretationContract.Relation.CORRECTION,
                ConversationTaskState.Field.ORIGIN,
                "Luwum Street"
            )
        )

        assertEquals(
            ConversationProposalApplication.Outcome.NO_STATE_CHANGE,
            result.outcome
        )
        assertEquals(original, result.task)
    }
}