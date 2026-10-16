package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealTimeRoutingKeywordTest {

    @Test
    fun updatedDoesNotMeanDateRequest() {
        assertFalse(
            RealTimeQueryPolicy.matches(
                "Write a Kotlin function that prints the updated list"
            )
        )
    }

    @Test
    fun ordinaryProgrammingWordsAreNotRealTime() {
        assertFalse(
            RealTimeQueryPolicy.matches(
                "Update a Kotlin data class"
            )
        )

        assertFalse(
            RealTimeQueryPolicy.matches(
                "Explain datetime formatting in Kotlin"
            )
        )
    }

    @Test
    fun actualRealTimeRequestsStillMatch() {
        assertTrue(
            RealTimeQueryPolicy.matches("What's the weather now?")
        )

        assertTrue(
            RealTimeQueryPolicy.matches("What's the current time?")
        )

        assertTrue(
            RealTimeQueryPolicy.matches("Check battery status")
        )
    }
}