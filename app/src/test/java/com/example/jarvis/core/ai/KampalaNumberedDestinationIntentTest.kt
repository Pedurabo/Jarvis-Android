package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaNumberedDestinationIntentTest {

    @Test
    fun numberedStreetDestinationIsRecognized() {
        val intent = KampalaNavigationIntentCoordinator.classify(
            "need to get to industrial area, street 8",
            awaitingOrigin = false
        )

        assertTrue(
            intent is KampalaNavigationIntentCoordinator.Intent.DestinationOnly
        )

        assertEquals(
            "industrial area, street 8",
            (intent as KampalaNavigationIntentCoordinator.Intent.DestinationOnly)
                .destination
        )
    }

    @Test
    fun ordinaryDestinationStillWorks() {
        val intent = KampalaNavigationIntentCoordinator.classify(
            "need to get to Kololo",
            awaitingOrigin = false
        )

        assertTrue(
            intent is KampalaNavigationIntentCoordinator.Intent.DestinationOnly
        )
    }

    @Test
    fun numberedDestinationWithOriginIsRecognized() {
        val intent = KampalaNavigationIntentCoordinator.classify(
            "from Makindye to Industrial Area, Street 8",
            awaitingOrigin = false
        )

        assertTrue(
            intent is KampalaNavigationIntentCoordinator.Intent.CompleteJourney
        )

        val journey =
            (intent as KampalaNavigationIntentCoordinator.Intent.CompleteJourney)
                .journey

        assertEquals("Makindye", journey.origin)
        assertEquals("Industrial Area, Street 8", journey.destination)
    }
}