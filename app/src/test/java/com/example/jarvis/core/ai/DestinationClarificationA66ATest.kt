package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class DestinationClarificationA66ATest {

    @Test
    fun branchClarificationExtendsUserSuppliedDestination() {
        // CDI.6.0-A6.6-D2:
        // Officially named campus now has a canonical identity.
        assertEquals(
            "Watoto Church Downtown",
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church",
                    "the Downtown branch"
                )
        )
    }

    @Test
    fun areaClarificationExtendsRequestedDestination() {
        assertEquals(
            "Watoto Church in Ntinda",
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church",
                    "in Ntinda"
                )
        )
    }

    @Test
    fun unrelatedMessageCannotInheritOldDestination() {
        assertNull(
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church",
                    "What is the weather today?"
                )
        )
    }

    @Test
    fun noPreviousDestinationMeansNoClarification() {
        assertNull(
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    null,
                    "the Downtown branch"
                )
        )
    }

    @Test
    fun arbitrarySingleWordIsNotAConfirmedBranch() {
        assertNull(
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church",
                    "hello"
                )
        )
    }

    @Test
    fun previousOriginCorrectionStillWorks() {
        assertEquals(
            "ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "actually, am starting from ntinda"
            )
        )
    }

    @Test
    fun dogBiteEmergencyStillReceivesGuidance() {
        val response = KampalaEmergencyResponsePolicy.respondOrNull(
            "I was bitten by a dog. What should I do?"
        )

        assertNotNull(response)
        assertTrue(
            response!!.contains("rabies", ignoreCase = true)
        )
    }
}