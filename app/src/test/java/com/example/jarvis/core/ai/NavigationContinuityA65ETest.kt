package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class NavigationContinuityA65ETest {

    @Test
    fun abbreviatedOriginCorrectionIsRecognized() {
        val message = "actually, am starting from ntinda"

        assertTrue(
            KampalaContextualNavigationPolicy.isFollowUp(message)
        )

        assertEquals(
            "ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(message)
        )
    }

    @Test
    fun firstPersonOriginCorrectionStillWorks() {
        assertEquals(
            "Ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "Actually, I'm starting from Ntinda"
            )
        )

        assertEquals(
            "Ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "I am starting from Ntinda"
            )
        )
    }

    @Test
    fun destinationCorrectionIsNotMisclassifiedAsOrigin() {
        assertNull(
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "Actually, I want to go to Ntinda"
            )
        )
    }

    @Test
    fun unrelatedMessageDoesNotBecomeNavigationFollowUp() {
        assertFalse(
            KampalaContextualNavigationPolicy.isFollowUp(
                "Tell me about businesses in Ntinda"
            )
        )
    }

    @Test
    fun establishedNavigationFollowUpStillWorks() {
        assertTrue(
            KampalaContextualNavigationPolicy.isFollowUp(
                "How do I get there?"
            )
        )
    }

    @Test
    fun dogBiteEmergencyBehaviorStillWorks() {
        val response =
            KampalaEmergencyResponsePolicy.respondOrNull(
                "I was bitten by a dog. What should I do?"
            )

        assertNotNull(response)
        assertTrue(response!!.contains("rabies", ignoreCase = true))
        assertTrue(response.contains("soap", ignoreCase = true))
    }
}