package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KampalaJourneyIntentResolverTest {

    @Test
    fun katweToLuziraNaturalRequest() {
        val result = KampalaJourneyIntentResolver.resolve(
            "am currently in Katwe need to get to luzira"
        )

        assertNotNull(result)
        assertEquals("Katwe", result!!.origin)
        assertEquals("luzira", result.destination)
    }

    @Test
    fun sentenceWithConjunction() {
        val result = KampalaJourneyIntentResolver.resolve(
            "I'm in Ntinda and I need to get to Muyenga"
        )

        assertNotNull(result)
        assertEquals("Ntinda", result!!.origin)
        assertEquals("Muyenga", result.destination)
    }

    @Test
    fun directJourneyRequest() {
        val result = KampalaJourneyIntentResolver.resolve(
            "How do I get from Nakasero to Kololo?"
        )

        assertNotNull(result)
        assertEquals("Nakasero", result!!.origin)
        assertEquals("Kololo", result.destination)
    }

    @Test
    fun explicitDirectionsRequest() {
        val result = KampalaJourneyIntentResolver.resolve(
            "Directions from Katwe to Luzira"
        )

        assertNotNull(result)
        assertEquals("Katwe", result!!.origin)
        assertEquals("Luzira", result.destination)
    }

    @Test
    fun unrelatedTechnicalQuestion() {
        assertNull(
            KampalaJourneyIntentResolver.resolve(
                "Explain Kotlin coroutines"
            )
        )
    }

    @Test
    fun destinationWithoutOrigin() {
        assertNull(
            KampalaJourneyIntentResolver.resolve(
                "How do I get to Luzira?"
            )
        )
    }

    @Test
    fun identicalOriginAndDestination() {
        assertNull(
            KampalaJourneyIntentResolver.resolve(
                "from Katwe to Katwe"
            )
        )
    }

    @Test
    fun katweToLuziraWithComma() {
        val result = KampalaJourneyIntentResolver.resolve(
            "am currently in Katwe, need to get to luzira"
        )

        assertNotNull(result)
        assertEquals("Katwe", result!!.origin)
        assertEquals("luzira", result.destination)
    }

    @Test
    fun doesNotSilentlyCorrectUnknownPlaceName() {
        val result = KampalaJourneyIntentResolver.resolve(
            "am currently in Katie, need to get to luzira"
        )

        assertNotNull(result)
        assertEquals("Katie", result!!.origin)
        assertEquals("luzira", result.destination)
    }

    @Test
    fun currentlyInKatweWithComma() {
        val result = KampalaJourneyIntentResolver.resolve(
            "currently in katwe, need to get to luzira"
        )

        assertNotNull(result)
        assertEquals("katwe", result!!.origin)
        assertEquals("luzira", result.destination)
    }

    @Test
    fun currentlyInKatweWithoutComma() {
        val result = KampalaJourneyIntentResolver.resolve(
            "currently in katwe need to get to luzira"
        )

        assertNotNull(result)
        assertEquals("katwe", result!!.origin)
        assertEquals("luzira", result.destination)
    }
}