package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KampalaOverpassA69D2BTest {

    private val bounds =
        KampalaGeoDiscoveryContract.Bounds(
            south = 0.30,
            west = 32.55,
            north = 0.35,
            east = 32.60
        )

    private fun request(
        category: String = "hospital",
        query: String? = null,
        pageToken: String? = null
    ) = KampalaGeoDiscoveryContract.SearchRequest(
        query = query,
        category = category,
        bounds = bounds,
        pageSize = 10,
        pageToken = pageToken
    )

    private val sample = """
        {
          "elements": [
            {
              "type": "node",
              "id": 12345,
              "lat": 0.32,
              "lon": 32.57,
              "tags": {
                "name": "Example Hospital",
                "amenity": "hospital"
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun categoryBuildsBoundedQuery() {
        val q = KampalaOverpassDiscoveryProvider
            .buildQuery(request())

        assertNotNull(q)
        assertTrue(q!!.contains("amenity"))
        assertTrue(q.contains("hospital"))
        assertTrue(q.contains("0.300000,32.550000"))
        assertTrue(q.contains("out center 11"))
    }

    @Test
    fun arbitraryQueryCannotBeInjectedIntoOverpass() {
        assertNull(
            KampalaOverpassDiscoveryProvider.buildQuery(
                request(query = "hospital; out;")
            )
        )
    }

    @Test
    fun unsupportedCategoryIsRejected() {
        assertNull(
            KampalaOverpassDiscoveryProvider.buildQuery(
                request(category = "private_residence")
            )
        )
    }

    @Test
    fun unsupportedPaginationIsRejected() {
        assertNull(
            KampalaOverpassDiscoveryProvider.buildQuery(
                request(pageToken = "page2")
            )
        )
    }

    @Test
    fun largeBoundsAreRejected() {
        val oversized = KampalaGeoDiscoveryContract.SearchRequest(
            category = "hospital",
            bounds = KampalaGeoDiscoveryContract.Bounds(
                0.1, 32.4, 0.6, 32.8
            )
        )

        assertNull(
            KampalaOverpassDiscoveryProvider.buildQuery(
                oversized
            )
        )
    }

    @Test
    fun publicNodeProducesAttributedPlace() {
        val result =
            KampalaOverpassDiscoveryProvider.parse(
                sample,
                request()
            )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Success
        )

        val page =
            (result as KampalaGeoDiscoveryContract.Result.Success)
                .page

        assertEquals("OpenStreetMap/Overpass", page.provider)
        assertEquals(1, page.places.size)
        assertEquals("node/12345", page.places[0].providerId)
        assertEquals(
            "https://www.openstreetmap.org/node/12345",
            page.places[0].sourceUrl
        )
        assertNull(page.places[0].publicAddress)
    }

    @Test
    fun unmappedFeatureIsNotInvented() {
        val empty = """{"elements":[]}"""

        val result =
            KampalaOverpassDiscoveryProvider.parse(
                empty,
                request()
            )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Success
        )
    }

    @Test
    fun malformedJsonReturnsProviderFailure() = runBlocking {
        val provider = KampalaOverpassDiscoveryProvider(
            object : KampalaOverpassDiscoveryProvider.Transport {
                override suspend fun fetch(
                    overpassQuery: String
                ): String = "{invalid"
            }
        )

        val result = provider.search(request())

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun fakeTransportReturnsRealisticStructuredResult() = runBlocking {
        val provider = KampalaOverpassDiscoveryProvider(
            object : KampalaOverpassDiscoveryProvider.Transport {
                override suspend fun fetch(
                    overpassQuery: String
                ): String = sample
            }
        )

        val result = provider.search(request())

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Success
        )
    }

    @Test
    fun transportIsNotCalledForRejectedRequest() = runBlocking {
        var called = false

        val provider = KampalaOverpassDiscoveryProvider(
            object : KampalaOverpassDiscoveryProvider.Transport {
                override suspend fun fetch(
                    overpassQuery: String
                ): String {
                    called = true
                    return sample
                }
            }
        )

        provider.search(
            request(category = "private_residence")
        )

        assertFalse(called)
    }
}