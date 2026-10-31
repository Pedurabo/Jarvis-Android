package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class SourceClarificationA67BTest {

    private fun place(): KampalaSourceBackedPlacePolicy.Place =
        requireNotNull(
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Watoto Church Downtown"
            )
        )

    @Test
    fun originIsRetainedInClarification() {
        val result =
            KampalaSourceClarificationResponsePolicy.respond(
                place(),
                "ntinda"
            )

        assertTrue(result.contains("ntinda"))
        assertTrue(result.contains("Watoto Church Downtown"))
        assertTrue(result.contains("Plot 87 Kampala Road"))
    }

    @Test
    fun sourceAttributionIsPreserved() {
        val result =
            KampalaSourceClarificationResponsePolicy.respond(
                place(),
                "Ntinda"
            )

        assertTrue(result.contains("watotochurch.com"))
        assertTrue(result.contains("Official source"))
    }

    @Test
    fun noUnverifiedDirectionsAreProduced() {
        val result =
            KampalaSourceClarificationResponsePolicy.respond(
                place(),
                "Ntinda"
            )

        assertTrue(
            result.contains("haven't calculated or checked")
        )

        assertFalse(
            result.contains("Open directions:")
        )
    }

    @Test
    fun clarificationDoesNotDuplicateDowntown() {
        val clarified =
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church Downtown",
                    "the Downtown branch"
                )

        assertEquals(
            "Watoto Church Downtown",
            clarified
        )
    }

    @Test
    fun missingOriginIsNotInvented() {
        val result =
            KampalaSourceClarificationResponsePolicy.respond(
                place(),
                null
            )

        assertFalse(result.contains("starting point is still"))
        assertTrue(result.contains("Watoto Church Downtown"))
    }

    @Test
    fun unifiedJourneyRemainsUncalculated() {
        val journey =
            KampalaDestinationJourney.fromPublishedPlace(
                place(),
                "Ntinda"
            )

        assertEquals("Ntinda", journey.origin)
        assertFalse(journey.routeCalculated)
    }

    @Test
    fun unrelatedPlaceDoesNotBorrowWatotoAddress() {
        assertNull(
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Some Other Church Downtown"
            )
        )
    }

    @Test
    fun emergencyRegressionRemainsIntact() {
        val result =
            KampalaEmergencyResponsePolicy.respondOrNull(
                "I was bitten by a dog. What should I do?"
            )

        assertNotNull(result)
        assertTrue(result!!.contains("rabies", true))
    }
}