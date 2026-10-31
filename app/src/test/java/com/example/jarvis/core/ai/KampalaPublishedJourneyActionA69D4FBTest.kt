package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class KampalaPublishedJourneyActionA69D4FBTest {

    private fun published(
        origin: String? = "Muyenga"
    ): KampalaDestinationJourney {
        val place =
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Watoto Church Downtown"
            )

        assertNotNull(place)

        return KampalaDestinationJourney.fromPublishedPlace(
            place!!,
            origin
        )
    }

    @Test
    fun watotoGetsSourceBackedMapsAction() {
        val output = KampalaPublishedJourneyAction.append(
            "Watoto Church Downtown published address.",
            published()
        )

        assertTrue(output.contains("Review route in Google Maps"))
        assertTrue(output.contains("https://www.google.com/maps/dir/"))
        assertTrue(output.contains("api=1"))
        assertTrue(output.contains("origin=Muyenga"))
        assertTrue(output.contains("destination="))
    }

    @Test
    fun missingOriginNeverCreatesMapsAction() {
        val notice = "Published destination only."

        assertEquals(
            notice,
            KampalaPublishedJourneyAction.append(
                notice,
                published(null)
            )
        )
    }

    @Test
    fun missingJourneyNeverCreatesMapsAction() {
        assertEquals(
            "No published location.",
            KampalaPublishedJourneyAction.append(
                "No published location.",
                null
            )
        )
    }

    @Test
    fun mapActionIsNotDuplicated() {
        val first = KampalaPublishedJourneyAction.append(
            "Published address; route not verified.",
            published()
        )

        val second = KampalaPublishedJourneyAction.append(
            first,
            published()
        )

        assertEquals(first, second)
    }

    @Test
    fun userOriginChangesHandoffDestinationDoesNot() {
        val first = KampalaPublishedJourneyAction.append(
            "Official address.",
            published("Muyenga")
        )

        val second = KampalaPublishedJourneyAction.append(
            "Official address.",
            published("Ntinda")
        )

        assertTrue(first.contains("origin=Muyenga"))
        assertTrue(second.contains("origin=Ntinda"))
        assertTrue(first.contains("destination="))
        assertTrue(second.contains("destination="))
        assertNotEquals(first, second)
    }

    @Test
    fun emergencyAndMapSearchAreNotModifiedByHelper() {
        val emergency = "A dog bite needs prompt medical attention."
        val mapNotice = "Geographic discovery is unavailable."

        assertEquals(
            emergency,
            KampalaPublishedJourneyAction.append(emergency, null)
        )

        assertEquals(
            mapNotice,
            KampalaPublishedJourneyAction.append(mapNotice, null)
        )
    }
}