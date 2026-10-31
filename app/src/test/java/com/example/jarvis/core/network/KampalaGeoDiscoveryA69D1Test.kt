package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaGeoDiscoveryA69D1Test {

    private val bounds =
        KampalaGeoDiscoveryContract.Bounds(
            south = 0.1,
            west = 32.4,
            north = 0.6,
            east = 32.8
        )

    private fun request(
        pageSize: Int = 20,
        pageToken: String? = null
    ) = KampalaGeoDiscoveryContract.SearchRequest(
        query = "hospital",
        bounds = bounds,
        pageSize = pageSize,
        pageToken = pageToken
    )

    private fun place(
        id: String = "example-1",
        latitude: Double = 0.3,
        longitude: Double = 32.6,
        source: String = "https://example.org/places/1",
        publicListing: Boolean = true
    ) = KampalaGeoDiscoveryContract.Place(
        providerId = id,
        name = "Example Public Hospital",
        category = "hospital",
        coordinates =
            KampalaGeoDiscoveryContract.Coordinates(
                latitude,
                longitude
            ),
        publicAddress = "Example public address",
        area = "Kampala",
        sourceUrl = source,
        isPublicListing = publicListing
    )

    private fun success(
        places: List<KampalaGeoDiscoveryContract.Place>,
        nextToken: String? = null
    ) = KampalaGeoDiscoveryContract.Result.Success(
        KampalaGeoDiscoveryContract.Page(
            provider = "test-provider",
            places = places,
            nextPageToken = nextToken
        )
    )

    @Test
    fun boundedRequestIsValid() {
        assertTrue(
            KampalaGeoDiscoveryContract.validRequest(request())
        )
    }

    @Test
    fun queryOrCategoryIsRequired() {
        val empty =
            KampalaGeoDiscoveryContract.SearchRequest(
                bounds = bounds
            )

        assertFalse(
            KampalaGeoDiscoveryContract.validRequest(empty)
        )
    }

    @Test
    fun invalidGeographicBoundsAreRejected() {
        val inverted =
            KampalaGeoDiscoveryContract.Bounds(
                south = 0.6,
                west = 32.4,
                north = 0.1,
                east = 32.8
            )

        assertFalse(
            KampalaGeoDiscoveryContract.validBounds(inverted)
        )
    }

    @Test
    fun nonFiniteCoordinatesAreRejected() {
        assertFalse(
            KampalaGeoDiscoveryContract.validCoordinates(
                KampalaGeoDiscoveryContract.Coordinates(
                    Double.NaN,
                    32.6
                )
            )
        )
    }

    @Test
    fun validProviderResultIsPreserved() {
        val original = success(
            listOf(place()),
            nextToken = "page-2"
        )

        val validated =
            KampalaGeoDiscoveryContract.validate(
                request(),
                original
            )

        assertEquals(original, validated)
    }

    @Test
    fun outsideBoundsCoordinatesFailClosed() {
        val validated =
            KampalaGeoDiscoveryContract.validate(
                request(),
                success(
                    listOf(place(latitude = 1.5))
                )
            )

        assertTrue(
            validated is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun invalidSourceCannotBecomePlaceResult() {
        val validated =
            KampalaGeoDiscoveryContract.validate(
                request(),
                success(
                    listOf(
                        place(source = "http://unsafe.example")
                    )
                )
            )

        assertTrue(
            validated is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun privateLocationCannotBePublished() {
        val validated =
            KampalaGeoDiscoveryContract.validate(
                request(),
                success(
                    listOf(
                        place(publicListing = false)
                    )
                )
            )

        assertTrue(
            validated is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun pageSizeLimitIsEnforced() {
        assertFalse(
            KampalaGeoDiscoveryContract.validRequest(
                request(pageSize = 100)
            )
        )
    }

    @Test
    fun excessiveProviderResultsAreRejected() {
        val validated =
            KampalaGeoDiscoveryContract.validate(
                request(pageSize = 1),
                success(
                    listOf(
                        place(id = "one"),
                        place(id = "two")
                    )
                )
            )

        assertTrue(
            validated is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun repeatingPageTokenIsRejected() {
        val validated =
            KampalaGeoDiscoveryContract.validate(
                request(pageToken = "page-2"),
                success(
                    listOf(place()),
                    nextToken = "page-2"
                )
            )

        assertTrue(
            validated is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun duplicateProviderIdsAreRejected() {
        val validated =
            KampalaGeoDiscoveryContract.validate(
                request(),
                success(
                    listOf(place(), place())
                )
            )

        assertTrue(
            validated is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun providerOutageIsNotReportedAsEmptySuccess() {
        val outage =
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "Provider unavailable"
            )

        val validated =
            KampalaGeoDiscoveryContract.validate(
                request(),
                outage
            )

        assertEquals(outage, validated)
    }

    @Test
    fun validEmptyPageIsDifferentFromOutage() {
        val validated =
            KampalaGeoDiscoveryContract.validate(
                request(),
                success(emptyList())
            )

        assertTrue(
            validated is KampalaGeoDiscoveryContract.Result.Success
        )
    }
}