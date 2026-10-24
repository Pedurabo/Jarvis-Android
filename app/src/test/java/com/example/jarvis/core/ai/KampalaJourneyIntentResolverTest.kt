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

    // CDI.5.55-A: Real Samsung navigation failures.

    @Test
    fun kololoToEntebbeAirportWithPoliteEnding() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "am currently in kololo, I need to get to Entebbe airport, please direct me"
        )

        assertNotNull(journey)
        assertEquals("kololo", journey!!.origin)
        assertEquals("Entebbe airport", journey.destination)
    }

    @Test
    fun kajansiToAmericanEmbassyWithOriginalWording() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "am cure in kajansi, I need to reach to Nsambya at American Ambassy"
        )

        assertNotNull(journey)
        assertEquals("kajansi", journey!!.origin)
        assertEquals(
            "Nsambya at American Ambassy",
            journey.destination
        )
    }

    @Test
    fun rubagaToMengoPalaceWithDirectionRequest() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "am in Rubaga, can you direct me to mengo, Buganda palace"
        )

        assertNotNull(journey)
        assertEquals("Rubaga", journey!!.origin)
        assertEquals(
            "mengo, Buganda palace",
            journey.destination
        )
    }

    @Test
    fun katweToMuyengaStillWorks() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "am currently in katwe, need to get to muyenga"
        )

        assertNotNull(journey)
        assertEquals("katwe", journey!!.origin)
        assertEquals("muyenga", journey.destination)
    }

    @Test
    fun technicalQuestionMustNotTriggerNavigation() {
        assertNull(
            KampalaJourneyIntentResolver.resolve(
                "Can you explain how Android navigation works?"
            )
        )
    }

    @Test
    fun missingDestinationDoesNotCreateJourney() {
        assertNull(
            KampalaJourneyIntentResolver.resolve(
                "I am currently in Kololo"
            )
        )
    }

    // CDI.5.56-A: Destination-only clarification tests.

    @Test
    fun needToGetToEntebbeAirportWithoutOrigin() {
        assertEquals(
            "Entebbe Airport",
            KampalaJourneyIntentResolver.destinationWithoutOrigin(
                "I need to get to Entebbe Airport"
            )
        )
    }

    @Test
    fun directMeToMengoPalaceWithoutOrigin() {
        assertEquals(
            "Mengo Palace",
            KampalaJourneyIntentResolver.destinationWithoutOrigin(
                "Can you direct me to Mengo Palace?"
            )
        )
    }

    @Test
    fun guideMeToEmbassyWithoutOrigin() {
        assertEquals(
            "the American Embassy",
            KampalaJourneyIntentResolver.destinationWithoutOrigin(
                "Please guide me to the American Embassy"
            )
        )
    }

    @Test
    fun fromToJourneyIsNotDestinationOnly() {
        assertNull(
            KampalaJourneyIntentResolver.destinationWithoutOrigin(
                "How do I get from Katwe to Luzira?"
            )
        )
    }

    @Test
    fun nonNavigationQuestionIsNotDestinationOnly() {
        assertNull(
            KampalaJourneyIntentResolver.destinationWithoutOrigin(
                "Explain Kotlin coroutines"
            )
        )
    }

    @Test
    fun directoryNavigationRemainsSeparate() {
        assertNull(
            KampalaJourneyIntentResolver.destinationWithoutOrigin(
                "Take me to Acacia Mall"
            )
        )
    }

    // CDI.5.57-C: Real Samsung destination-first regression.

    @Test
    fun airportFromKololoProducesCompleteJourney() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "I need to get to entebbe airport from kololo"
        )

        assertNotNull(journey)
        assertEquals("kololo", journey!!.origin)
        assertEquals("entebbe airport", journey.destination)
    }

    @Test
    fun airportFromKololoIsNotDestinationOnly() {
        assertNull(
            KampalaJourneyIntentResolver.destinationWithoutOrigin(
                "I need to get to entebbe airport from kololo"
            )
        )
    }

    @Test
    fun destinationFirstWorksWithQuestionMark() {
        val journey = KampalaJourneyIntentResolver.resolve(
            "I want to go to Mengo Palace from Rubaga?"
        )

        assertNotNull(journey)
        assertEquals("Rubaga", journey!!.origin)
        assertEquals("Mengo Palace", journey.destination)
    }

    @Test
    fun destinationOnlyAirportStillRequiresOrigin() {
        assertEquals(
            "entebbe airport",
            KampalaJourneyIntentResolver.destinationWithoutOrigin(
                "I need to get to entebbe airport"
            )
        )
    }

    @Test
    fun destinationFirstDoesNotClassifyOrdinaryQuestion() {
        assertNull(
            KampalaJourneyIntentResolver.resolve(
                "Explain how flights reach Entebbe Airport"
            )
        )
    }
}