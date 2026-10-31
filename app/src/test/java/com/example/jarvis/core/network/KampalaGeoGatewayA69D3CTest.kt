package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KampalaGeoGatewayA69D3CTest {

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

    private fun fake(
        action: suspend (
            KampalaGeoDiscoveryContract.SearchRequest
        ) -> KampalaGeoDiscoveryContract.Result
    ): KampalaGeoDiscoveryContract.Provider =
        object : KampalaGeoDiscoveryContract.Provider {
            override suspend fun search(
                request: KampalaGeoDiscoveryContract.SearchRequest
            ): KampalaGeoDiscoveryContract.Result =
                action(request)
        }

    private fun emptyPage() =
        KampalaGeoDiscoveryContract.Result.Success(
            KampalaGeoDiscoveryContract.Page(
                provider = "test-provider",
                places = emptyList(),
                nextPageToken = null
            )
        )

    @Test
    fun productionGatewayCanBeConstructedWithoutSearch() {
        val gateway =
            KampalaGeoDiscoveryGateway.createOverpass()

        assertNotNull(gateway)
    }

    @Test
    fun invalidRequestNeverReachesProvider() = runBlocking {
        var calls = 0

        val gateway =
            KampalaGeoDiscoveryGateway.forTesting(
                delegate = fake {
                    calls++
                    emptyPage()
                },
                nowMillis = { 1000L }
            )

        val invalid =
            KampalaGeoDiscoveryContract.SearchRequest(
                bounds = bounds
            )

        val result = gateway.search(invalid)

        assertEquals(0, calls)
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun successfulResultsAreCachedAcrossGatewayCalls() = runBlocking {
        var calls = 0

        val gateway =
            KampalaGeoDiscoveryGateway.forTesting(
                delegate = fake {
                    calls++
                    emptyPage()
                },
                nowMillis = { 1000L }
            )

        val first = gateway.search(request)
        val second = gateway.search(request)

        assertEquals(first, second)
        assertEquals(1, calls)
        assertTrue(
            second is KampalaGeoDiscoveryContract.Result.Success
        )
    }

    @Test
    fun failureActivatesGatewayCooldown() = runBlocking {
        var calls = 0

        val gateway =
            KampalaGeoDiscoveryGateway.forTesting(
                delegate = fake {
                    calls++
                    KampalaGeoDiscoveryContract.Result.Unavailable(
                        "HTTP 502"
                    )
                },
                nowMillis = { 1000L }
            )

        val first = gateway.search(request)
        val second = gateway.search(request)

        assertEquals(1, calls)

        assertTrue(
            first is KampalaGeoDiscoveryContract.Result.Unavailable
        )

        assertTrue(
            second is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun invalidProviderOutputCannotEscapeGateway() = runBlocking {
        val gateway =
            KampalaGeoDiscoveryGateway.forTesting(
                delegate = fake {
                    KampalaGeoDiscoveryContract.Result.Success(
                        KampalaGeoDiscoveryContract.Page(
                            provider = "",
                            places = emptyList(),
                            nextPageToken = null
                        )
                    )
                },
                nowMillis = { 1000L }
            )

        val result = gateway.search(request)

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun missingDataIsNotInvented() = runBlocking {
        val gateway =
            KampalaGeoDiscoveryGateway.forTesting(
                delegate = fake {
                    emptyPage()
                },
                nowMillis = { 1000L }
            )

        val result = gateway.search(request)

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Success
        )

        val page =
            (result as KampalaGeoDiscoveryContract.Result.Success)
                .page

        assertTrue(page.places.isEmpty())
        assertEquals("test-provider", page.provider)
    }
}