package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaGeoP0RegressionTest {

    private fun fixture(name: String): String {
        val stream = javaClass.classLoader
            ?.getResourceAsStream(name)
            ?: error("Missing geographic test fixture: $name")

        return stream.bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
    }

    @Test
    fun normalHospitalRequestIsRecognized() {
        val command = KampalaExplicitGeoDiscovery.parseOrNull(
            "find hospitals in central kampala"
        )

        assertNotNull(command)
        assertEquals("hospital", command!!.category)
    }

    @Test
    fun punctuationAndCapitalizationAreRecognized() {
        val variants = listOf(
            "Find hospitals in Central Kampala?",
            "FIND HOSPITALS IN CENTRAL KAMPALA.",
            "  find  hospitals  in central kampala!  ",
            "Show me hospitals in Central Kampala?",
            "Are there hospitals in central kampala?"
        )

        for (message in variants) {
            val command = KampalaExplicitGeoDiscovery.parseOrNull(
                message
            )

            assertNotNull(message, command)
            assertEquals(message, "hospital", command!!.category)
        }
    }

    @Test
    fun unrelatedOrSensitiveRequestsDoNotMatch() {
        val messages = listOf(
            "Explain Kotlin coroutines",
            "Find hospitals near my current GPS location",
            "Find hospitals anywhere in Uganda",
            "Call the emergency services",
            "Find private homes in central kampala"
        )

        for (message in messages) {
            assertNull(
                message,
                KampalaExplicitGeoDiscovery.parseOrNull(message)
            )
        }
    }

    @Test
    fun actualSmallAreaApiResponseParsesSuccessfully() {
        val command = KampalaExplicitGeoDiscovery.parseOrNull(
            "find hospitals in central kampala"
        )!!

        val request = command.toRequest().copy(
            bounds = KampalaGeoDiscoveryContract.Bounds(
                south = 0.323000,
                west = 32.573000,
                north = 0.327000,
                east = 32.577000
            )
        )

        val result = KampalaJarvisGeoApiProvider.parse(
            fixture("geo-p0-success.json"),
            request
        )

        assertTrue(
            "Real backend SUCCESS fixture was rejected by " +
                "Android geographic validation: $result",
            result is KampalaGeoDiscoveryContract.Result.Success
        )

        val page =
            (result as KampalaGeoDiscoveryContract.Result.Success).page

        assertTrue(page.places.isNotEmpty())
        assertTrue(page.places.size <= request.pageSize)

        for (place in page.places) {
            assertTrue(
                KampalaGeoDiscoveryContract.withinBounds(
                    place.coordinates,
                    request.bounds
                )
            )
            assertTrue(place.isPublicListing)
        }
    }

    @Test
    fun actualLargeAreaIncompleteResponseIsTyped() {
        val request = KampalaExplicitGeoDiscovery.parseOrNull(
            "find hospitals in central kampala"
        )!!.toRequest()

        val result = KampalaJarvisGeoApiProvider.parse(
            fixture("geo-p0-incomplete.json"),
            request
        )

        assertTrue(
            "Backend INCOMPLETE response was not recognized: $result",
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )

        val unavailable =
            result as KampalaGeoDiscoveryContract.Result.Unavailable

        assertEquals(
            KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE,
            unavailable.kind
        )
    }
}