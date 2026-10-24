package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaNavigationIntentCoordinatorTest {

    private fun classify(
        text: String,
        awaiting: Boolean = false
    ) = KampalaNavigationIntentCoordinator.classify(
        text,
        awaiting
    )

    @Test
    fun fromKololoToAirport() {
        val result = classify("From Kololo to Entebbe Airport")
        assertTrue(result is KampalaNavigationIntentCoordinator.Intent.CompleteJourney)
        val journey =
            (result as KampalaNavigationIntentCoordinator.Intent.CompleteJourney).journey
        assertEquals("Kololo", journey.origin)
        assertEquals("Entebbe Airport", journey.destination)
    }

    @Test
    fun destinationFirstRequest() {
        assertTrue(
            classify("I need to get to Entebbe Airport from Kololo") is
                KampalaNavigationIntentCoordinator.Intent.CompleteJourney
        )
    }

    @Test
    fun shortReplyWithPendingOrigin() {
        val result = classify("ntinda", true)
        assertEquals(
            "ntinda",
            (result as KampalaNavigationIntentCoordinator.Intent.OriginReply).origin
        )
    }

    @Test
    fun barePlaceWithoutPendingOrigin() {
        assertTrue(
            classify("ntinda") is KampalaNavigationIntentCoordinator.Intent.Other
        )
    }

    @Test
    fun explicitOriginReply() {
        val result = classify("From Rubaga", true)
        assertEquals(
            "Rubaga",
            (result as KampalaNavigationIntentCoordinator.Intent.OriginReply).origin
        )
    }

    @Test
    fun destinationOnlyRequest() {
        val result = classify("I need to get to Victoria Mall")
        assertEquals(
            "Victoria Mall",
            (result as KampalaNavigationIntentCoordinator.Intent.DestinationOnly).destination
        )
    }

    @Test
    fun newDestinationOverridesPendingOrigin() {
        val result = classify("I need to get to Entebbe Airport", true)
        assertTrue(
            result is KampalaNavigationIntentCoordinator.Intent.DestinationOnly
        )
    }

    @Test
    fun unrelatedQuestionWhilePending() {
        assertTrue(
            classify("Explain Kotlin coroutines", true) is
                KampalaNavigationIntentCoordinator.Intent.Other
        )
    }

    @Test
    fun completeJourneyOverridesPendingOrigin() {
        assertTrue(
            classify("from Katwe to Luzira", true) is
                KampalaNavigationIntentCoordinator.Intent.CompleteJourney
        )
    }

    @Test
    fun ordinaryChat() {
        assertTrue(
            classify("What is Jetpack Compose?") is
                KampalaNavigationIntentCoordinator.Intent.Other
        )
    }
}