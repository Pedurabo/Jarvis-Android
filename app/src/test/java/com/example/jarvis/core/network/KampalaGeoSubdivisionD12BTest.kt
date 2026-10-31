package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KampalaGeoSubdivisionD12BTest {

    private val bounds = KampalaGeoDiscoveryContract.Bounds(
        0.300, 32.550, 0.350, 32.600
    )

    private val request = KampalaGeoDiscoveryContract.SearchRequest(
        category = "hospital",
        bounds = bounds,
        pageSize = 10
    )

    private fun place(
        b: KampalaGeoDiscoveryContract.Bounds,
        id: String
    ): KampalaGeoDiscoveryContract.Place =
        KampalaGeoDiscoveryContract.Place(
            providerId = id,
            name = "Hospital $id",
            category = "hospital",
            coordinates = KampalaGeoDiscoveryContract.Coordinates(
                (b.south + b.north) / 2.0,
                (b.west + b.east) / 2.0
            ),
            publicAddress = null,
            area = null,
            sourceUrl = "https://www.openstreetmap.org/node/$id",
            isPublicListing = true
        )

    @Test
    fun completeRootResultDoesNotSubdivide() = runBlocking {
        var calls = 0

        val provider = object : KampalaGeoDiscoveryContract.Provider {
            override suspend fun search(
                request: KampalaGeoDiscoveryContract.SearchRequest
            ): KampalaGeoDiscoveryContract.Result {
                calls++
                return KampalaGeoDiscoveryContract.Result.Success(
                    KampalaGeoDiscoveryContract.Page(
                        "OpenStreetMap",
                        listOf(place(request.bounds, "1")),
                        null
                    )
                )
            }
        }

        val result = KampalaGeoSubdivision.search(provider, request)

        assertTrue(result is KampalaGeoDiscoveryContract.Result.Success)
        assertEquals(1, calls)
    }

    @Test
    fun incompleteRootSubdividesAndCombinesTiles() = runBlocking {
        var calls = 0

        val provider = object : KampalaGeoDiscoveryContract.Provider {
            override suspend fun search(
                request: KampalaGeoDiscoveryContract.SearchRequest
            ): KampalaGeoDiscoveryContract.Result {
                calls++

                if (calls == 1) {
                    return KampalaGeoDiscoveryContract.Result.Unavailable(
                        "The Uganda geographic index returned an " +
                            "unusable or incomplete response.",
                        KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE
                    )
                }

                return KampalaGeoDiscoveryContract.Result.Success(
                    KampalaGeoDiscoveryContract.Page(
                        "OpenStreetMap",
                        listOf(place(request.bounds, calls.toString())),
                        null
                    )
                )
            }
        }

        val result = KampalaGeoSubdivision.search(provider, request)

        assertEquals(5, calls)
        assertTrue(result is KampalaGeoDiscoveryContract.Result.Success)

        val page =
            (result as KampalaGeoDiscoveryContract.Result.Success).page

        assertEquals(4, page.places.size)
        assertEquals(4, page.places.map { it.providerId }.distinct().size)
    }

    @Test
    fun failedChildNeverReturnsPartialListings() = runBlocking {
        var calls = 0

        val provider = object : KampalaGeoDiscoveryContract.Provider {
            override suspend fun search(
                request: KampalaGeoDiscoveryContract.SearchRequest
            ): KampalaGeoDiscoveryContract.Result {
                calls++

                if (calls == 1) {
                    return KampalaGeoDiscoveryContract.Result.Unavailable(
                        "An unusable or incomplete response.",
                        KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE
                    )
                }

                if (calls == 3) {
                    return KampalaGeoDiscoveryContract.Result.Unavailable(
                        "Network failure"
                    )
                }

                return KampalaGeoDiscoveryContract.Result.Success(
                    KampalaGeoDiscoveryContract.Page(
                        "OpenStreetMap",
                        listOf(place(request.bounds, calls.toString())),
                        null
                    )
                )
            }
        }

        val result = KampalaGeoSubdivision.search(provider, request)

        assertTrue(result is KampalaGeoDiscoveryContract.Result.Unavailable)
        assertEquals(3, calls)
    }

    @Test
    fun ordinaryFailureDoesNotTriggerSubdivision() = runBlocking {
        var calls = 0

        val provider = object : KampalaGeoDiscoveryContract.Provider {
            override suspend fun search(
                request: KampalaGeoDiscoveryContract.SearchRequest
            ): KampalaGeoDiscoveryContract.Result {
                calls++
                return KampalaGeoDiscoveryContract.Result.Unavailable(
                    "Connection timed out"
                )
            }
        }

        val result = KampalaGeoSubdivision.search(provider, request)

        assertEquals(1, calls)
        assertTrue(result is KampalaGeoDiscoveryContract.Result.Unavailable)
    }
}