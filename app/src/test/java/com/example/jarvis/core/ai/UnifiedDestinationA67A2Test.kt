package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class UnifiedDestinationA67A2Test {

    private fun journey(): KampalaDestinationJourney {
        val place =
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Watoto Church Downtown"
            )

        assertNotNull(place)

        return KampalaDestinationJourney.fromPublishedPlace(
            place!!
        )
    }

    @Test
    fun publishedDestinationHasCanonicalIdentity() {
        val state = journey()

        assertEquals(
            "Watoto Church Downtown",
            state.destinationName
        )
        assertEquals(
            "Plot 87 Kampala Road, Kampala",
            state.address
        )
        assertEquals(
            "Central Kampala",
            state.area
        )
        assertFalse(state.routeCalculated)
    }

    @Test
    fun originCorrectionRetainsDestinationAndSource() {
        val original = journey()
        val updated = original.withUserOrigin("Ntinda")

        assertEquals("Ntinda", updated.origin)
        assertEquals(
            original.destinationName,
            updated.destinationName
        )
        assertEquals(original.address, updated.address)
        assertEquals(original.sourceUrl, updated.sourceUrl)
        assertFalse(updated.routeCalculated)
    }

    @Test
    fun publishedAddressAvoidsRepeatedBranchQuestion() {
        val response =
            journey().withUserOrigin("Ntinda")
                .originFollowUpNotice()

        assertTrue(response.contains("Ntinda"))
        assertTrue(
            response.contains("Watoto Church Downtown")
        )
        assertTrue(response.contains("Plot 87"))
        assertTrue(response.contains("watotochurch.com"))

        assertFalse(
            response.contains(
                "Which branch or exact location",
                ignoreCase = true
            )
        )

        assertTrue(
            response.contains(
                "has not calculated",
                ignoreCase = true
            )
        )
    }

    @Test
    fun combinedOriginCorrectionIsRecognized() {
        assertEquals(
            "Ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "Actually, am starting from Ntinda"
            )
        )
    }

    @Test
    fun splitOriginCorrectionIsRecognized() {
        assertTrue(
            KampalaSplitCorrectionPolicy
                .isUnfinishedCorrection("Actually,")
        )

        assertEquals(
            "Ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "Am starting from Ntinda"
            )
        )
    }

    @Test
    fun lowercaseInputRemainsLowercase() {
        assertEquals(
            "ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "am starting from ntinda"
            )
        )
    }

    @Test
    fun downtownClarificationRemainsCanonical() {
        assertEquals(
            "Watoto Church Downtown",
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church Downtown",
                    "The Downtown branch"
                )
        )
    }

    @Test
    fun unrelatedChurchCannotBorrowPublishedAddress() {
        assertNull(
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Other Church Downtown"
            )
        )
    }

    @Test
    fun emergencyPolicyRemainsAvailable() {
        val response =
            KampalaEmergencyResponsePolicy.respondOrNull(
                "I was bitten by a dog. What should I do?"
            )

        assertNotNull(response)
        assertTrue(response!!.contains("rabies", true))
    }
}