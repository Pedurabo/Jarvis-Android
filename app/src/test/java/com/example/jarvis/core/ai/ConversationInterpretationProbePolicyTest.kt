package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationInterpretationProbePolicyTest {

    private fun activeTask(): ConversationTaskState.Task =
        ConversationTaskState.apply(
            changes = listOf(
                ConversationTaskState.Change.Start(
                    ConversationTaskState.Kind.MEDICAL_ASSISTANCE,
                    "Reach medical care",
                    requiresUrgentCare = true
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.DESTINATION,
                    "Case Clinic",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                ),
                ConversationTaskState.Change.SetField(
                    ConversationTaskState.Field.ORIGIN,
                    "Luwum Street",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
            )
        )!!

    @Test
    fun disabledProbeDoesNotPreparePrompt() {
        assertNull(
            ConversationInterpretationProbePolicy.prepare(
                activeTask(),
                "think it's in Nakasero",
                explicitlyEnabled = false
            )
        )
    }

    @Test
    fun absentTaskDoesNotPreparePrompt() {
        assertNull(
            ConversationInterpretationProbePolicy.prepare(
                null,
                "think it's in Nakasero",
                explicitlyEnabled = true
            )
        )
    }

    @Test
    fun explicitEvaluationUsesTaskContext() {
        val prompt = ConversationInterpretationProbePolicy.prepare(
            activeTask(),
            "think it's in Nakasero",
            explicitlyEnabled = true
        )

        assertNotNull(prompt)
        assertTrue(prompt!!.contains("Case Clinic"))
        assertTrue(prompt.contains("Luwum Street"))
        assertTrue(prompt.contains("think it's in Nakasero"))
    }

    @Test
    fun rejectsOversizedMessage() {
        assertNull(
            ConversationInterpretationProbePolicy.prepare(
                activeTask(),
                "x".repeat(501),
                explicitlyEnabled = true
            )
        )
    }

    @Test
    fun rejectsEmptyMessage() {
        assertNull(
            ConversationInterpretationProbePolicy.prepare(
                activeTask(),
                "  ",
                explicitlyEnabled = true
            )
        )
    }

    @Test
    fun validProposalIsParsedButNotApplied() {
        val original = activeTask()

        val proposal = ConversationInterpretationProbePolicy.validate(
            "relation=CONTINUE\n" +
                "field=SUGGESTED_AREA\n" +
                "value=Nakasero"
        )

        assertNotNull(proposal)
        assertEquals(
            ConversationTaskState.Field.SUGGESTED_AREA,
            proposal!!.field
        )

        assertNull(
            original.get(
                ConversationTaskState.Field.SUGGESTED_AREA
            )
        )
    }

    @Test
    fun rejectsToolSyntax() {
        assertNull(
            ConversationInterpretationProbePolicy.validate(
                "<|tool_call|> MarketNews"
            )
        )
    }

    @Test
    fun rejectsNarrativeInsteadOfClassification() {
        assertNull(
            ConversationInterpretationProbePolicy.validate(
                "I think the clinic is in Nakasero."
            )
        )
    }

    @Test
    fun invalidAndBlankOutputsAreRejected() {
        assertNull(
            ConversationInterpretationProbePolicy.validate(null)
        )
        assertNull(
            ConversationInterpretationProbePolicy.validate("")
        )
        assertNull(
            ConversationInterpretationProbePolicy.validate(
                "relation=CONTINUE"
            )
        )
    }

    @Test
    fun sourceProvenanceIsNotModified() {
        val original = activeTask()

        ConversationInterpretationProbePolicy.validate(
            "relation=CORRECTION\n" +
                "field=ORIGIN\n" +
                "value=Ntinda"
        )

        assertEquals(
            "Luwum Street",
            original.get(
                ConversationTaskState.Field.ORIGIN
            )?.value
        )

        assertEquals(
            ConversationTaskState.Provenance.USER_SUPPLIED,
            original.get(
                ConversationTaskState.Field.ORIGIN
            )?.provenance
        )
    }
}