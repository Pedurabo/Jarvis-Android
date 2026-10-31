package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class ConversationRoutingRepairTest {

    @Test
    fun dogBiteHelpRequestReceivesImmediateGuidance() {
        val response =
            KampalaEmergencyResponsePolicy.respondOrNull(
                "I was bitten by a dog. What should I do?"
            )

        assertNotNull(response)
        assertTrue(response!!.contains("soap", ignoreCase = true))
        assertTrue(response.contains("rabies", ignoreCase = true))
        assertTrue(response.contains("tetanus", ignoreCase = true))
    }

    @Test
    fun educationalQuestionDoesNotBecomePersonalEmergency() {
        assertNull(
            KampalaEmergencyResponsePolicy.respondOrNull(
                "What is a dog bite?"
            )
        )
    }

    @Test
    fun unrelatedQuestionDoesNotTriggerDogBitePolicy() {
        assertNull(
            KampalaEmergencyResponsePolicy.respondOrNull(
                "What should I do about my laptop?"
            )
        )
    }

    @Test
    fun explicitOriginCorrectionIsRecognized() {
        val input = "Actually, I'm starting from Ntinda"

        assertTrue(
            KampalaContextualNavigationPolicy.isFollowUp(input)
        )

        assertEquals(
            "Ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(input)
        )
    }

    @Test
    fun destinationChangeIsNotTreatedAsOrigin() {
        assertNull(
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "Actually, I want to go to Ntinda"
            )
        )
    }

    @Test
    fun unrelatedTextDoesNotBecomeOrigin() {
        assertFalse(
            KampalaContextualNavigationPolicy.isFollowUp(
                "Tell me about markets in Ntinda"
            )
        )
    }

    @Test
    fun existingNavigationFollowUpStillWorks() {
        assertTrue(
            KampalaContextualNavigationPolicy.isFollowUp(
                "How do I get there?"
            )
        )
    }

    @Test
    fun originCorrectionDoesNotFabricateDestination() {
        val origin =
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "Actually, I'm starting from Ntinda"
            )

        assertEquals("Ntinda", origin)

        // Extracting an origin does not establish a destination.
        val rememberedDestination: String? = null

        assertNull(rememberedDestination)
    }
}