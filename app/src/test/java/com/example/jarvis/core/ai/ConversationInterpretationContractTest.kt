package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationInterpretationContractTest {

    private val contract = ConversationInterpretationContract

    private fun task(): ConversationTaskState.Task =
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
    fun promptIncludesExistingTaskAndNewMessage() {
        val text = contract.prompt(
            task(),
            "think it's in Nakasero"
        )

        assertTrue(text.contains("Case Clinic"))
        assertTrue(text.contains("Luwum Street"))
        assertTrue(text.contains("think it's in Nakasero"))
        assertTrue(text.contains("SUGGESTED_AREA"))
        assertTrue(text.contains("classification only"))
    }

    @Test
    fun acceptsUnverifiedAreaInterpretation() {
        val proposal = contract.parse(
            "relation=CONTINUE\n" +
                "field=SUGGESTED_AREA\n" +
                "value=Nakasero"
        )

        assertNotNull(proposal)
        assertEquals(
            ConversationInterpretationContract.Relation.CONTINUE,
            proposal!!.relation
        )
        assertEquals(
            ConversationTaskState.Field.SUGGESTED_AREA,
            proposal.field
        )
        assertEquals("Nakasero", proposal.value)
    }

    @Test
    fun acceptsOriginCorrection() {
        val proposal = contract.parse(
            "relation=CORRECTION\n" +
                "field=ORIGIN\n" +
                "value=Ntinda"
        )

        assertEquals(
            ConversationInterpretationContract.Relation.CORRECTION,
            proposal?.relation
        )
        assertEquals(
            ConversationTaskState.Field.ORIGIN,
            proposal?.field
        )
    }

    @Test
    fun acceptsClearTopicChange() {
        val proposal = contract.parse(
            "relation=NEW_TASK\nfield=NONE\nvalue=-"
        )

        assertNotNull(proposal)
        assertNull(proposal!!.field)
        assertNull(proposal.value)
    }

    @Test
    fun acceptsUncertainInterpretation() {
        val proposal = contract.parse(
            "relation=UNSURE\nfield=NONE\nvalue=-"
        )

        assertEquals(
            ConversationInterpretationContract.Relation.UNSURE,
            proposal?.relation
        )
    }

    @Test
    fun rejectsUnrecognizedFieldAndExtraLines() {
        assertNull(contract.parse(
            "relation=CONTINUE\nfield=VERIFIED_ADDRESS\nvalue=Nakasero"
        ))

        assertNull(contract.parse(
            "relation=CONTINUE\nfield=ORIGIN\nvalue=Ntinda\nexecute=true"
        ))
    }

    @Test
    fun rejectsToolCallsAndModelNarration() {
        assertNull(contract.parse(
            """{"tool":"GoogleMaps","params":{}}"""
        ))

        assertNull(contract.parse(
            "I think the user means Nakasero."
        ))
    }

    @Test
    fun rejectsOversizedOrUnsafeFieldValues() {
        assertNull(contract.parse(
            "relation=CONTINUE\nfield=DESTINATION\nvalue=" +
                "x".repeat(81)
        ))

        assertNull(contract.parse(
            "relation=CONTINUE\nfield=DESTINATION\n" +
                "value=<|tool_call|>"
        ))
    }

    @Test
    fun rejectsInconsistentTaskChange() {
        assertNull(contract.parse(
            "relation=NEW_TASK\nfield=ORIGIN\nvalue=Ntinda"
        ))

        assertNull(contract.parse(
            "relation=UNSURE\nfield=NONE\nvalue=Nakasero"
        ))
    }

    @Test
    fun doesNotPromoteProposalToVerifiedFact() {
        val original = task()

        val proposal = contract.parse(
            "relation=CONTINUE\n" +
                "field=SUGGESTED_AREA\nvalue=Nakasero"
        )

        assertNotNull(proposal)
        assertNull(
            original.get(ConversationTaskState.Field.SUGGESTED_AREA)
        )
        assertEquals(
            ConversationTaskState.Provenance.USER_SUPPLIED,
            original.get(
                ConversationTaskState.Field.DESTINATION
            )?.provenance
        )
    }
}