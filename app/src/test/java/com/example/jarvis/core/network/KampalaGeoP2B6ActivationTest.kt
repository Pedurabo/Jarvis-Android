package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaGeoP2B6ActivationTest {

    private val enabled = setOf(
        "Ntinda",
        "Nakawa",
        "Kansanga",
        "Kabalagala",
        "Rubaga",
        "Muyenga",
        "Bugoloobi"
    )

    @Test
    fun exactlySevenNewAreasAreEnabled() {
        val areas = KampalaGeoP2ApprovedAreas.areas

        assertEquals(7, areas.size)
        assertEquals(enabled, areas.map { it.label }.toSet())

        assertNull(
            KampalaGeoAreaResolver.DEFAULT.resolve("Old Kampala")
        )

        assertNotNull(
            KampalaGeoAreaResolver.DEFAULT.resolve("Central Kampala")
        )
    }

    @Test
    fun enabledAreasPreserveReviewedCoordinates() {
        val originals = KampalaGeoP2CandidateCatalog.areas
            .associateBy { it.label }

        for (name in enabled) {
            val active = KampalaGeoAreaResolver.DEFAULT
                .resolve(name) ?: error("Missing $name")

            assertEquals(originals[name]!!.bounds, active.bounds)

            assertTrue(
                active.provenance.contains(
                    "experimentally enabled for search"
                )
            )

            assertTrue(
                active.provenance.contains("not KCCA-verified")
            )
        }
    }

    @Test
    fun allElevenCategoriesWorkWithApprovedAreas() {
        val categories = mapOf(
            "hospitals" to "hospital",
            "clinics" to "clinic",
            "pharmacies" to "pharmacy",
            "schools" to "school",
            "universities" to "university",
            "banks" to "bank",
            "fuel stations" to "fuel_station",
            "hotels" to "hotel",
            "guesthouses" to "guest_house",
            "police stations" to "police",
            "embassies" to "embassy"
        )

        for (name in enabled) {
            for ((phrase, category) in categories) {
                val command =
                    KampalaExplicitGeoDiscovery.parseOrNull(
                        "find $phrase in $name"
                    )

                assertNotNull("$phrase / $name", command)
                assertEquals(category, command!!.category)
                assertEquals(name, command.label)

                assertTrue(
                    KampalaGeoDiscoveryContract.validRequest(
                        command.toRequest()
                    )
                )
            }
        }
    }

    @Test
    fun oldKampalaRemainsInactive() {
        assertNull(
            KampalaExplicitGeoDiscovery.parseOrNull(
                "find banks in old kampala"
            )
        )

        assertNull(
            KampalaExplicitGeoDiscovery.parseOrNull(
                "find hospitals in old kampala"
            )
        )
    }

    @Test
    fun centralKampalaBoundsRemainUnchanged() {
        val command = KampalaExplicitGeoDiscovery.parseOrNull(
            "find hospitals in central kampala"
        ) ?: error("Central Kampala missing")

        assertEquals("hospital", command.category)
        assertEquals("Central Kampala", command.label)

        assertEquals(
            0.300000, command.bounds.south, 0.0000001
        )
        assertEquals(
            32.550000, command.bounds.west, 0.0000001
        )
        assertEquals(
            0.350000, command.bounds.north, 0.0000001
        )
        assertEquals(
            32.600000, command.bounds.east, 0.0000001
        )
    }
}