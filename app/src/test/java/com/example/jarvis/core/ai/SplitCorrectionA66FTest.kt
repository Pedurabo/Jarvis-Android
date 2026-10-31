package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class SplitCorrectionA66FTest {

    @Test
    fun standaloneActuallyIsUnfinishedCorrection() {
        assertTrue(
            KampalaSplitCorrectionPolicy.isUnfinishedCorrection(
                "actually,"
            )
        )
    }

    @Test
    fun ordinaryOriginCorrectionIsNotAnUnfinishedOpener() {
        assertFalse(
            KampalaSplitCorrectionPolicy.isUnfinishedCorrection(
                "actually, am starting from ntinda"
            )
        )
    }

    @Test
    fun splitCorrectionPreservesExistingDestination() {
        assertTrue(
            KampalaSplitCorrectionPolicy
                .shouldPreserveUnverifiedDestination(
                    "Watoto Church Downtown",
                    "actually,"
                )
        )
    }

    @Test
    fun emptyContextCannotBePreserved() {
        assertFalse(
            KampalaSplitCorrectionPolicy
                .shouldPreserveUnverifiedDestination(
                    null,
                    "actually,"
                )
        )
    }

    @Test
    fun unrelatedQuestionCannotPreserveNavigationContext() {
        assertFalse(
            KampalaSplitCorrectionPolicy
                .shouldPreserveUnverifiedDestination(
                    "Watoto Church Downtown",
                    "Explain Kotlin coroutines"
                )
        )
    }

    @Test
    fun followingOriginMessageRemainsRecognized() {
        assertEquals(
            "ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "am starting from ntinda"
            )
        )
    }

    @Test
    fun combinedOriginMessageRemainsRecognized() {
        assertEquals(
            "ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "actually, am starting from ntinda"
            )
        )
    }

    @Test
    fun branchClarificationKeepsCanonicalName() {
        assertEquals(
            "Watoto Church Downtown",
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church Downtown",
                    "the Downtown branch"
                )
        )
    }

    @Test
    fun sourceBackedAddressRemainsAvailable() {
        val place = KampalaSourceBackedPlacePolicy.resolveOrNull(
            "Watoto Church Downtown"
        )

        assertNotNull(place)
        assertEquals(
            "Plot 87 Kampala Road, Kampala",
            place!!.address
        )
    }

    @Test
    fun noAddressIsBorrowedForUnrelatedChurch() {
        assertNull(
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Another Church Downtown"
            )
        )
    }

    @Test
    fun emergencyResponseIsUnaffected() {
        val emergency =
            KampalaEmergencyResponsePolicy.respondOrNull(
                "I was bitten by a dog. What should I do?"
            )

        assertNotNull(emergency)
        assertTrue(emergency!!.contains("rabies", true))
    }
}