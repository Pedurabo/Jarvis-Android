package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaP3A1MultiStopJourneyTest {

    @Test
    fun explicitSingleIntermediateStop() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "from Marriott Nsambya via Green Hill Academy to Kingdom Kampala"
        )

        assertNotNull(journey)
        assertEquals("Marriott Nsambya", journey!!.origin)
        assertEquals(listOf("Green Hill Academy"), journey.stops)
        assertEquals("Kingdom Kampala", journey.destination)
    }

    @Test
    fun multipleIntermediateStopsKeepTheirOrder() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "from Nsambya via Ntinda via Kololo to Nakasero"
        )

        assertNotNull(journey)
        assertEquals(listOf("Ntinda", "Kololo"), journey!!.stops)
        assertEquals("Nakasero", journey.destination)
    }

    @Test
    fun existingTwoPointJourneyRemainsCompatible() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "from Katwe to Luzira"
        )

        assertNotNull(journey)
        assertEquals("Katwe", journey!!.origin)
        assertEquals("Luzira", journey.destination)
        assertTrue(journey.stops.isEmpty())
    }

    @Test
    fun naturalTwoPointJourneyRemainsCompatible() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "am currently in Katwe need to get to luzira"
        )

        assertNotNull(journey)
        assertEquals("Katwe", journey!!.origin)
        assertEquals("luzira", journey.destination)
        assertTrue(journey.stops.isEmpty())
    }

    @Test
    fun duplicateStopIsRejected() {
        assertNull(
            KampalaJourneyIntentResolver.resolve(
                "from Nsambya via Nsambya to Kingdom Kampala"
            )
        )
    }

    @Test
    fun incompleteExplicitStopIsRejected() {
        assertNull(
            KampalaJourneyIntentResolver.resolve(
                "from Nsambya via A to Kingdom Kampala"
            )
        )
    }

    @Test
    fun unrelatedTechnicalQuestionIsNotNavigation() {
        assertNull(
            KampalaJourneyIntentResolver.resolve(
                "Explain Kotlin coroutine cancellation"
            )
        )
    }
}