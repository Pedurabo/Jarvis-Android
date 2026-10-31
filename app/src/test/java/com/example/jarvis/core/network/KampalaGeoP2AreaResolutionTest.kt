package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaGeoP2AreaResolutionTest {

    @Test
    fun existingHospitalRequestStillWorks() {
        val command = KampalaExplicitGeoDiscovery.parseOrNull(
            "find hospitals in Central Kampala?"
        )

        assertNotNull(command)
        assertEquals("hospital", command!!.category)
        assertEquals("Central Kampala", command.label)
    }

    @Test
    fun allElevenCategoriesUseTheSameAreaResolver() {
        val cases = mapOf(
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

        for ((phrase, category) in cases) {
            val command = KampalaExplicitGeoDiscovery.parseOrNull(
                "find $phrase in central kampala"
            )

            assertNotNull(phrase, command)
            assertEquals(phrase, category, command!!.category)
            assertEquals("Central Kampala", command.label)
            assertTrue(
                KampalaGeoDiscoveryContract.validRequest(
                    command.toRequest()
                )
            )
        }
    }

    @Test
    fun existingExplicitCommandsArePreserved() {
        val commands = mapOf(
            "map search hospitals in central kampala" to "hospital",
            "map search pharmacies in central kampala" to "pharmacy",
            "map search banks in central kampala" to "bank",
            "map search schools in central kampala" to "school"
        )

        for ((phrase, expected) in commands) {
            assertEquals(
                expected,
                KampalaExplicitGeoDiscovery
                    .parseOrNull(phrase)?.category
            )
        }
    }

    @Test
    fun areaResolverIsIndependentOfCategory() {
        val resolver = KampalaGeoAreaResolver(
            listOf(
                KampalaGeoAreaResolver.Area(
                    label = "Test Area",
                    bounds = KampalaGeoDiscoveryContract.Bounds(
                        south = 0.310,
                        west = 32.560,
                        north = 0.320,
                        east = 32.570
                    ),
                    provenance = "Synthetic JVM test fixture only."
                )
            )
        )

        val hospitals =
            KampalaExplicitGeoDiscovery.parseWithResolver(
                "find hospitals in test area",
                resolver
            )

        val banks =
            KampalaExplicitGeoDiscovery.parseWithResolver(
                "find banks in test area",
                resolver
            )

        assertNotNull(hospitals)
        assertNotNull(banks)

        assertEquals("hospital", hospitals!!.category)
        assertEquals("bank", banks!!.category)
        assertEquals(hospitals.bounds, banks.bounds)

        assertEquals(0.310, hospitals.bounds.south, 0.0000001)
        assertEquals(32.570, hospitals.bounds.east, 0.0000001)
    }

    @Test
    fun unknownAreasFailClosed() {
        val queries = listOf(
            "find hospitals in Old Kampala",
            "find hotels in Entebbe",
            "find banks in Mbarara",
            "find pharmacies in an imaginary area",
            "find embassies in Uganda"
        )

        for (query in queries) {
            assertNull(
                query,
                KampalaExplicitGeoDiscovery.parseOrNull(query)
            )
        }
    }

    @Test
    fun emergenciesAndGpsRequestsDoNotMatch() {
        val queries = listOf(
            "Call the police",
            "I need an ambulance",
            "find hospitals near my current location",
            "find private homes in central kampala",
            "Explain Kotlin coroutines"
        )

        for (query in queries) {
            assertNull(
                query,
                KampalaExplicitGeoDiscovery.parseOrNull(query)
            )
        }
    }

    @Test
    fun invalidAreaBoundsAreRejected() {
        try {
            KampalaGeoAreaResolver(
                listOf(
                    KampalaGeoAreaResolver.Area(
                        label = "Invalid",
                        bounds = KampalaGeoDiscoveryContract.Bounds(
                            south = 0.40,
                            west = 32.60,
                            north = 0.30,
                            east = 32.50
                        ),
                        provenance = "Synthetic invalid test data."
                    )
                )
            )

            fail("Invalid geographic bounds must be rejected.")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun duplicateAreaNamesAreRejected() {
        val bounds = KampalaGeoDiscoveryContract.Bounds(
            south = 0.30,
            west = 32.55,
            north = 0.35,
            east = 32.60
        )

        try {
            KampalaGeoAreaResolver(
                listOf(
                    KampalaGeoAreaResolver.Area(
                        "Central Kampala", bounds, "Fixture A"
                    ),
                    KampalaGeoAreaResolver.Area(
                        "central kampala", bounds, "Fixture B"
                    )
                )
            )

            fail("Duplicate names must be rejected.")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}