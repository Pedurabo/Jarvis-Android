package com.example.jarvis.ui

import org.junit.Assert.*
import org.junit.Test

class JarvisGeoResultCardsP1BTest {

    private val valid = """
        Experimental map search: Central Kampala
        Search category: hospital
        Search area: predefined bounds, not live GPS.

        Geographic place discovery
        Provider: OpenStreetMap / Jarvis Uganda snapshot (showing 2 of 76 map listings)

        1. AFYA
        Category: hospital
        Map coordinates: 0.345375, 32.568700
        Map record: https://www.openstreetmap.org/node/11010338422

        2. AIA MEDICAL SERVICES
        Category: hospital
        Map coordinates: 0.315960, 32.571070
        Map record: https://www.openstreetmap.org/node/11010338018

        These are map-provider listings, not independently verified addresses,
        business operations or opening hours.
    """.trimIndent()

    @Test
    fun validGeographicResultsBecomeCards() {
        val result = JarvisGeoResultCardsModel.parse(valid)

        assertNotNull(result)
        assertEquals("Central Kampala", result!!.area)
        assertEquals(2, result.places.size)
        assertEquals("AFYA", result.places[0].name)
        assertEquals(
            "https://www.openstreetmap.org/node/11010338422",
            result.places[0].sourceUrl
        )
    }

    @Test
    fun ordinaryChatIsNotConvertedIntoCards() {
        assertNull(
            JarvisGeoResultCardsModel.parse(
                "Tell me about Kampala history."
            )
        )
    }

    @Test
    fun unavailableGeographicResultsStayUnchanged() {
        assertNull(
            JarvisGeoResultCardsModel.parse(
                "Experimental map search: Central Kampala\n" +
                    "Geographic discovery is currently unavailable."
            )
        )
    }

    @Test
    fun missingMapSourceFailsClosed() {
        assertNull(
            JarvisGeoResultCardsModel.parse(
                valid.replace(
                    "https://www.openstreetmap.org/node/11010338422",
                    "https://example.com/unknown"
                )
            )
        )
    }

    @Test
    fun duplicateSourceIdsFailClosed() {
        assertNull(
            JarvisGeoResultCardsModel.parse(
                valid.replace(
                    "https://www.openstreetmap.org/node/11010338018",
                    "https://www.openstreetmap.org/node/11010338422"
                )
            )
        )
    }
}