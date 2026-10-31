package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaNavigationPlaceholderRegressionTest {

    private fun assertDestination(
        input: String,
        expected: String
    ) {
        val intent = KampalaNavigationIntentCoordinator.classify(
            input,
            awaitingOrigin = false
        )

        assertTrue(
            intent is KampalaNavigationIntentCoordinator.Intent.DestinationOnly
        )

        assertEquals(
            expected,
            (intent as KampalaNavigationIntentCoordinator.Intent.DestinationOnly)
                .destination
        )
    }

    @Test
    fun recognizesWatotoDirectionsRequest() {
        assertDestination(
            "can you get me directions to watoto church downtown?",
            "watoto church downtown"
        )
    }

    @Test
    fun recognizesPoliteDirectionsRequest() {
        assertDestination(
            "Please help me get to Watoto Church",
            "Watoto Church"
        )
    }

    @Test
    fun preservesNumberedStreetRecognition() {
        assertDestination(
            "need to get to industrial area, street 8",
            "industrial area, street 8"
        )
    }

    @Test
    fun rejectsPlaceholderLocationAnswer() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Can you get me directions to Watoto Church downtown?",
                "The address is: [Street address, city, country]"
            )
        )
    }

    @Test
    fun allowsActualNavigationClarification() {
        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "Can you get me directions to Watoto Church downtown?",
                "Which Watoto Church location do you mean, " +
                    "and where will you be starting from?"
            )
        )
    }

    @Test
    fun permitsTechnicalPlaceholderDiscussion() {
        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "Explain placeholder address templates in code",
                "A template may contain [Street address, city, country]."
            )
        )
    }
}