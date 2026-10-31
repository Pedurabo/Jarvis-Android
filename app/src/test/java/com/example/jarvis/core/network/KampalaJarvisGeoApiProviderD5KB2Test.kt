package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

internal class KampalaJarvisGeoApiProviderD5KB2Test {

    private val bounds =
        KampalaGeoDiscoveryContract.Bounds(
            south = 0.30,
            west = 32.55,
            north = 0.35,
            east = 32.60
        )

    private val request =
        KampalaGeoDiscoveryContract.SearchRequest(
            category = "hospital",
            bounds = bounds,
            pageSize = 10
        )

    private fun sample(
        status: String = "SUCCESS",
        category: String = "hospital",
        source: String =
            "https://www.openstreetmap.org/node/123",
        verification: String = "MAP_LISTINGS_ONLY"
    ): String = """
        {
          "status": "$status",
          "snapshotGeneratedAt": "2026-10-25T21:21:21Z",
          "attribution": "OpenStreetMap contributors",
          "verification": "$verification",
          "places": [
            {
              "providerId": "node/123",
              "name": "Example Hospital",
              "category": "$category",
              "latitude": 0.325,
              "longitude": 32.575,
              "publicAddress": null,
              "sourceUrl": "$source"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun httpsOriginAccepted() {

        assertEquals(
            "https://example.org/geo/search",
            KampalaJarvisGeoApiProvider.validateBaseUrl(
                "https://example.org"
            )
        )
    }

    @Test
    fun cleartextOriginRejected() {

        try {
            KampalaJarvisGeoApiProvider.validateBaseUrl(
                "http://example.org"
            )

            fail("Expected HTTPS validation failure")

        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun requestIsBounded() {

        val url = KampalaJarvisGeoApiProvider.requestUrl(
            "https://example.org/geo/search",
            request
        )

        assertNotNull(url)
        assertTrue(url!!.contains("category=hospital"))
        assertTrue(url.contains("limit=10"))
    }

    @Test
    fun validRecordMapsToContract() {

        val result = KampalaJarvisGeoApiProvider.parse(
            sample(),
            request
        )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Success
        )

        val place =
            (result as KampalaGeoDiscoveryContract.Result.Success)
                .page.places.single()

        assertEquals("node/123", place.providerId)
        assertEquals("hospital", place.category)
        assertTrue(place.isPublicListing)
    }

    @Test
    fun incompleteResponseFailsClosed() {

        val result = KampalaJarvisGeoApiProvider.parse(
            sample(status = "INCOMPLETE"),
            request
        )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun wrongCategoryFailsClosed() {

        val result = KampalaJarvisGeoApiProvider.parse(
            sample(category = "bank"),
            request
        )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun forgedSourceUrlFailsClosed() {

        val result = KampalaJarvisGeoApiProvider.parse(
            sample(source = "https://example.org/node/123"),
            request
        )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun unsupportedVerificationFailsClosed() {

        val result = KampalaJarvisGeoApiProvider.parse(
            sample(verification = "OFFICIALLY_VERIFIED"),
            request
        )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun injectedTransportReturnsResult() = runBlocking {

        val fake =
            object : KampalaJarvisGeoApiProvider.Transport {

                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult =
                    KampalaJarvisGeoApiProvider.HttpResult(
                        statusCode = 200,
                        body = sample()
                    )
            }

        val provider = KampalaJarvisGeoApiProvider(
            "https://example.org",
            fake
        )

        assertTrue(
            provider.search(request) is
                KampalaGeoDiscoveryContract.Result.Success
        )
    }

    @Test
    fun serviceFailureIsUnavailable() = runBlocking {

        val fake =
            object : KampalaJarvisGeoApiProvider.Transport {

                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult =
                    KampalaJarvisGeoApiProvider.HttpResult(
                        statusCode = 503,
                        body = ""
                    )
            }

        val provider = KampalaJarvisGeoApiProvider(
            "https://example.org",
            fake
        )

        assertTrue(
            provider.search(request) is
                KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun providerIdentityMustMatchSourceUrl() {

        val forged = sample().replace(
            """"providerId": "node/123"""",
            """"providerId": "node/999""""
        )

        val result = KampalaJarvisGeoApiProvider.parse(
            forged,
            request
        )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

}