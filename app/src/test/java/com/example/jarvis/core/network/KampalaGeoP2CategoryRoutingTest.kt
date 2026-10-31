package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaGeoP2CategoryRoutingTest {

    private val cases = mapOf(
        "Find hospitals in Central Kampala?" to "hospital",
        "Find clinics in Central Kampala?" to "clinic",
        "Find pharmacies in Central Kampala?" to "pharmacy",
        "Find schools in Central Kampala?" to "school",
        "Find universities in Central Kampala?" to "university",
        "Find banks in Central Kampala?" to "bank",
        "Find fuel stations in Central Kampala?" to "fuel_station",
        "Find hotels in Central Kampala?" to "hotel",
        "Find guesthouses in Central Kampala?" to "guest_house",
        "Find police stations in Central Kampala?" to "police",
        "Find embassies in Central Kampala?" to "embassy"
    )

    @Test
    fun allSupportedCentralCategoriesRouteCorrectly() {
        for ((message, expected) in cases) {
            val command =
                KampalaExplicitGeoDiscovery.parseOrNull(message)

            assertNotNull(message, command)
            assertEquals(message, expected, command!!.category)

            val request = command.toRequest()

            assertTrue(
                KampalaGeoDiscoveryContract.validRequest(request)
            )

            assertEquals(10, request.pageSize)
            assertNull(request.pageToken)
            assertNull(request.query)
        }
    }

    @Test
    fun existingExplicitCommandsRemainAvailable() {
        val commands = mapOf(
            "map search hospitals in central kampala" to "hospital",
            "map search pharmacies in central kampala" to "pharmacy",
            "map search banks in central kampala" to "bank",
            "map search schools in central kampala" to "school"
        )

        for ((message, category) in commands) {
            assertEquals(
                category,
                KampalaExplicitGeoDiscovery
                    .parseOrNull(message)?.category
            )
        }
    }

    @Test
    fun searchesUseExistingBoundsNotLiveGps() {
        for (message in cases.keys) {
            val command =
                KampalaExplicitGeoDiscovery.parseOrNull(message)!!

            val bounds = command.toRequest().bounds

            assertEquals(0.300000, bounds.south, 0.0000001)
            assertEquals(32.550000, bounds.west, 0.0000001)
            assertEquals(0.350000, bounds.north, 0.0000001)
            assertEquals(32.600000, bounds.east, 0.0000001)
        }
    }

    @Test
    fun unsupportedAreasAndUnrelatedRequestsFailClosed() {
        val examples = listOf(
            "Find hospitals in Entebbe",
            "Find hotels near my current location",
            "Find embassies across Uganda",
            "Find apartments in private residences",
            "Call the police",
            "Explain Kotlin coroutines",
            "Find banks in Kampala without an area"
        )

        for (message in examples) {
            assertNull(
                message,
                KampalaExplicitGeoDiscovery.parseOrNull(message)
            )
        }
    }
}