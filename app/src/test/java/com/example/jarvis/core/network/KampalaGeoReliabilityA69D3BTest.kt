package com.example.jarvis.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KampalaGeoReliabilityA69D3BTest {

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

    private fun successful() =
        KampalaGeoDiscoveryContract.Result.Success(
            KampalaGeoDiscoveryContract.Page(
                provider = "test-map-provider",
                places = listOf(
                    KampalaGeoDiscoveryContract.Place(
                        providerId = "node/123",
                        name = "Example Hospital",
                        category = "hospital",
                        coordinates =
                            KampalaGeoDiscoveryContract.Coordinates(
                                0.32,
                                32.57
                            ),
                        publicAddress = null,
                        area = "Kampala",
                        sourceUrl =
                            "https://www.openstreetmap.org/node/123",
                        isPublicListing = true
                    )
                ),
                nextPageToken = null
            )
        )

    private fun emptySuccess() =
        KampalaGeoDiscoveryContract.Result.Success(
            KampalaGeoDiscoveryContract.Page(
                provider = "test-map-provider",
                places = emptyList(),
                nextPageToken = null
            )
        )

    private fun fake(
        behavior: suspend (
            KampalaGeoDiscoveryContract.SearchRequest
        ) -> KampalaGeoDiscoveryContract.Result
    ): KampalaGeoDiscoveryContract.Provider =
        object : KampalaGeoDiscoveryContract.Provider {
            override suspend fun search(
                request: KampalaGeoDiscoveryContract.SearchRequest
            ): KampalaGeoDiscoveryContract.Result =
                behavior(request)
        }

    @Test
    fun successfulSearchIsCached() = runBlocking {
        var calls = 0
        var time = 1000L

        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                calls++
                successful()
            },
            nowMillis = { time }
        )

        val first = wrapper.search(request)
        val second = wrapper.search(request)

        assertEquals(first, second)
        assertEquals(1, calls)
    }

    @Test
    fun expiredCacheFetchesAgain() = runBlocking {
        var calls = 0
        var time = 1000L

        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                calls++
                successful()
            },
            nowMillis = { time },
            cacheTtlMillis = 100L
        )

        wrapper.search(request)
        time += 101L
        wrapper.search(request)

        assertEquals(2, calls)
    }

    @Test
    fun unavailableProviderTriggersCooldown() = runBlocking {
        var calls = 0
        var time = 1000L

        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                calls++
                KampalaGeoDiscoveryContract.Result.Unavailable(
                    "HTTP 502"
                )
            },
            nowMillis = { time },
            cooldownMillis = 500L
        )

        val first = wrapper.search(request)
        val second = wrapper.search(request)

        assertTrue(
            first is KampalaGeoDiscoveryContract.Result.Unavailable
        )
        assertTrue(
            second is KampalaGeoDiscoveryContract.Result.Unavailable
        )

        assertEquals(1, calls)

        time += 501L
        wrapper.search(request)

        assertEquals(2, calls)
    }

    @Test
    fun cooldownDoesNotBecomeEmptySuccess() = runBlocking {
        var calls = 0

        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                calls++
                KampalaGeoDiscoveryContract.Result.Unavailable(
                    "Provider offline"
                )
            },
            nowMillis = { 1000L }
        )

        wrapper.search(request)
        val result = wrapper.search(request)

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
        assertEquals(1, calls)
    }

    @Test
    fun emptySuccessCanBeCached() = runBlocking {
        var calls = 0

        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                calls++
                emptySuccess()
            },
            nowMillis = { 1000L }
        )

        val first = wrapper.search(request)
        val second = wrapper.search(request)

        assertTrue(
            first is KampalaGeoDiscoveryContract.Result.Success
        )
        assertEquals(first, second)
        assertEquals(1, calls)
    }

    @Test
    fun invalidResponseFailsClosed() = runBlocking {
        var calls = 0

        val invalid =
            KampalaGeoDiscoveryContract.Result.Success(
                KampalaGeoDiscoveryContract.Page(
                    provider = "",
                    places = emptyList(),
                    nextPageToken = null
                )
            )

        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                calls++
                invalid
            },
            nowMillis = { 1000L }
        )

        val first = wrapper.search(request)
        val second = wrapper.search(request)

        assertTrue(
            first is KampalaGeoDiscoveryContract.Result.Unavailable
        )
        assertTrue(
            second is KampalaGeoDiscoveryContract.Result.Unavailable
        )
        assertEquals(1, calls)
    }

    @Test
    fun invalidRequestNeverCallsProvider() = runBlocking {
        var calls = 0

        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                calls++
                successful()
            }
        )

        val invalid =
            KampalaGeoDiscoveryContract.SearchRequest(
                bounds = bounds
            )

        val result = wrapper.search(invalid)

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
        assertEquals(0, calls)
    }

    @Test
    fun transportExceptionBecomesUnavailable() = runBlocking {
        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                throw IllegalStateException("Network failed")
            }
        )

        val result = wrapper.search(request)

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun cancellationIsNotSwallowed() = runBlocking {
        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                throw CancellationException("Cancelled")
            }
        )

        var propagated = false

        try {
            wrapper.search(request)
        } catch (_: CancellationException) {
            propagated = true
        }

        assertTrue(propagated)
    }

    @Test
    fun repeatedRequestAfterSuccessUsesCachedData() = runBlocking {
        var calls = 0

        val wrapper = KampalaGeoReliabilityProvider(
            delegate = fake {
                calls++
                successful()
            },
            nowMillis = { 1000L }
        )

        repeat(5) {
            wrapper.search(request)
        }

        assertEquals(1, calls)
    }
}