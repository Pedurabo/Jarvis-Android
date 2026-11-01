package com.example.jarvis.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaNearbyDistanceSorterTest {

    private fun place(
        id: String,
        latitude: Double,
        longitude: Double
    ) = KampalaGeoDiscoveryContract.Place(
        providerId = id,
        name = "Pharmacy $id",
        category = "pharmacy",
        coordinates = KampalaGeoDiscoveryContract.Coordinates(
            latitude = latitude,
            longitude = longitude
        ),
        publicAddress = null,
        area = null,
        sourceUrl = "https://www.openstreetmap.org/node/123",
        isPublicListing = true
    )

    private fun page(
        places: List<KampalaGeoDiscoveryContract.Place>,
        nextPageToken: String? = null
    ) = KampalaGeoDiscoveryContract.Page(
        provider = "OpenStreetMap",
        places = places,
        nextPageToken = nextPageToken
    )

    @Test
    fun `sorts returned places from nearest to farthest`() {
        val far = place("far", 0.31, 32.58)
        val near = place("near", 0.251, 32.551)
        val medium = place("medium", 0.26, 32.56)

        val sorted = KampalaNearbyDistanceSorter.sort(
            page(listOf(far, medium, near)),
            latitude = 0.25,
            longitude = 32.55
        )

        assertEquals(
            listOf("near", "medium", "far"),
            sorted.places.map { it.providerId }
        )
    }

    @Test
    fun `preserves original order when distances are equal`() {
        val first = place("first", 0.25, 32.55)
        val second = place("second", 0.25, 32.55)

        val sorted = KampalaNearbyDistanceSorter.sort(
            page(listOf(first, second)),
            latitude = 0.25,
            longitude = 32.55
        )

        assertEquals(
            listOf("first", "second"),
            sorted.places.map { it.providerId }
        )
    }

    @Test
    fun `empty page remains empty`() {
        val sorted = KampalaNearbyDistanceSorter.sort(
            page(emptyList()),
            latitude = 0.25,
            longitude = 32.55
        )

        assertTrue(sorted.places.isEmpty())
    }

    @Test
    fun `preserves provider and pagination token`() {
        val original = page(
            places = listOf(place("one", 0.25, 32.55)),
            nextPageToken = "next-page"
        )

        val sorted = KampalaNearbyDistanceSorter.sort(
            original,
            latitude = 0.25,
            longitude = 32.55
        )

        assertEquals(original.provider, sorted.provider)
        assertEquals(original.nextPageToken, sorted.nextPageToken)
        assertEquals(original.places.size, sorted.places.size)
    }

    @Test
    fun `distance is zero for identical coordinates`() {
        val distance = KampalaNearbyDistanceSorter.distanceMeters(
            0.25, 32.55, 0.25, 32.55
        )

        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `distance is approximately one kilometre for nearby coordinates`() {
        val distance = KampalaNearbyDistanceSorter.distanceMeters(
            0.25, 32.55, 0.259, 32.55
        )

        assertTrue(distance in 950.0..1050.0)
    }
}
