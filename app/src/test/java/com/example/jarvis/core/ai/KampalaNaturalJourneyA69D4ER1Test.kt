package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class KampalaNaturalJourneyA69D4ER1Test {

    private fun journey(
        text: String
    ): KampalaJourneyIntentResolver.Journey? =
        KampalaJourneyIntentResolver.resolve(text)

    @Test
    fun acaciaMallFromNtindaIsCompleteJourney() {
        val result = journey(
            "How do I get to Acacia Mall from Ntinda?"
        )
        assertNotNull(result)
        assertEquals("Ntinda", result!!.origin)
        assertEquals("Acacia Mall", result.destination)
    }

    @Test
    fun watotoDowntownFromMuyengaIsCompleteJourney() {
        val result = journey(
            "Take me from Muyenga to Watoto Church Downtown"
        )
        assertNotNull(result)
        assertEquals("Muyenga", result!!.origin)
        assertEquals(
            "Watoto Church Downtown",
            result.destination
        )
    }

    @Test
    fun recognizedJourneysReachCompleteJourneyRoute() {
        val requests = listOf(
            "How do I get to Acacia Mall from Ntinda?",
            "Take me from Muyenga to Watoto Church Downtown"
        )
        requests.forEach { request ->
            assertTrue(
                KampalaNavigationIntentCoordinator.classify(
                    request,
                    awaitingOrigin = false
                ) is KampalaNavigationIntentCoordinator.Intent.CompleteJourney
            )
        }
    }

    @Test
    fun earlierFromToPatternStillWorks() {
        val result = journey(
            "From Kololo to Entebbe Airport"
        )
        assertNotNull(result)
        assertEquals("Kololo", result!!.origin)
        assertEquals("Entebbe Airport", result.destination)
    }

    @Test
    fun ordinaryNavigationWithoutOriginIsNotComplete() {
        assertNull(journey("How do I get to Acacia Mall?"))
    }

    @Test
    fun emergencyQuestionIsNotACompleteJourney() {
        assertNull(
            journey("A dog has bitten me, what should I do?")
        )
    }

    @Test
    fun explicitGeographicSearchIsNotACompleteJourney() {
        assertNull(
            journey("map search hospitals in central kampala")
        )
    }

    @Test
    fun destinationIsNotSwappedWithOrigin() {
        val result = journey(
            "How can I get to Watoto Church Downtown from Muyenga?"
        )
        assertNotNull(result)
        assertEquals("Muyenga", result!!.origin)
        assertEquals(
            "Watoto Church Downtown",
            result.destination
        )
    }

    @Test
    fun sameOriginAndDestinationIsRejected() {
        assertNull(
            journey("Take me from Ntinda to Ntinda")
        )
    }
}