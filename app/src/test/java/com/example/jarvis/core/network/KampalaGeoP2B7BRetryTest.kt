package com.example.jarvis.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

internal class KampalaGeoP2B7BRetryTest {

    private val request =
        KampalaGeoDiscoveryContract.SearchRequest(
            category = "hospital",
            bounds = KampalaGeoDiscoveryContract.Bounds(
                south = 0.333769,
                west = 32.608678,
                north = 0.357310,
                east = 32.625731
            ),
            pageSize = 10
        )

    private suspend fun runWith(
        fake: KampalaJarvisGeoApiProvider.Transport
    ): KampalaGeoDiscoveryContract.Result =
        KampalaJarvisGeoApiProvider(
            "https://example.org",
            fake
        ).search(request)

    @Test
    fun temporary503GetsExactlyTwoAttempts() = runBlocking {
        var calls = 0

        val result = runWith(
            object : KampalaJarvisGeoApiProvider.Transport {
                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult {
                    calls++
                    return KampalaJarvisGeoApiProvider.HttpResult(
                        503, ""
                    )
                }
            }
        )

        assertEquals(2, calls)
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun socketTimeoutGetsExactlyTwoAttempts() = runBlocking {
        var calls = 0

        val result = runWith(
            object : KampalaJarvisGeoApiProvider.Transport {
                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult {
                    calls++
                    throw SocketTimeoutException("Synthetic timeout")
                }
            }
        )

        assertEquals(2, calls)
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun badRequestDoesNotRetry() = runBlocking {
        var calls = 0

        val result = runWith(
            object : KampalaJarvisGeoApiProvider.Transport {
                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult {
                    calls++
                    return KampalaJarvisGeoApiProvider.HttpResult(
                        400, ""
                    )
                }
            }
        )

        assertEquals(1, calls)
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun rateLimitDoesNotRetry() = runBlocking {
        var calls = 0

        val result = runWith(
            object : KampalaJarvisGeoApiProvider.Transport {
                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult {
                    calls++
                    return KampalaJarvisGeoApiProvider.HttpResult(
                        429, ""
                    )
                }
            }
        )

        assertEquals(1, calls)
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun cancellationPropagatesWithoutRetry() = runBlocking {
        var calls = 0

        try {
            runWith(
                object : KampalaJarvisGeoApiProvider.Transport {
                    override suspend fun get(
                        url: String
                    ): KampalaJarvisGeoApiProvider.HttpResult {
                        calls++
                        throw CancellationException(
                            "Synthetic cancellation"
                        )
                    }
                }
            )

            fail("Cancellation must propagate")

        } catch (_: CancellationException) {
            assertEquals(1, calls)
        }
    }

    @Test
    fun ordinarySuccessIsNotRetried() = runBlocking {
        var calls = 0

        val response = """
            {
              "status": "SUCCESS",
              "snapshotGeneratedAt": "2026-10-25T21:21:21Z",
              "attribution": "OpenStreetMap contributors",
              "verification": "MAP_LISTINGS_ONLY",
              "places": []
            }
        """.trimIndent()

        val result = runWith(
            object : KampalaJarvisGeoApiProvider.Transport {
                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult {
                    calls++
                    return KampalaJarvisGeoApiProvider.HttpResult(
                        200, response
                    )
                }
            }
        )

        assertEquals(1, calls)
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Success
        )
    }
}