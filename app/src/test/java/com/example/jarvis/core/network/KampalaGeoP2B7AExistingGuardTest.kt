package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * P2-B7A: Verify the already-existing unsupported-area guard.
 *
 * These tests must not change production routing code.
 */
class KampalaGeoP2B7AExistingGuardTest {

    private class CountingProvider :
        KampalaGeoDiscoveryContract.Provider {

        var calls: Int = 0

        override suspend fun search(
            request: KampalaGeoDiscoveryContract.SearchRequest
        ): KampalaGeoDiscoveryContract.Result {
            calls++

            return KampalaGeoDiscoveryContract.Result.Unavailable(
                "Synthetic test provider unavailable."
            )
        }
    }

    @Test
    fun oldKampalaReturnsCoverageNoticeWithoutBackendCall() = runBlocking {
        val provider = CountingProvider()
        val handler = KampalaExplicitGeoDiscovery(provider)

        val response = handler.answerOrNull(
            "find banks in old kampala"
        )

        assertNotNull(response)

        assertTrue(
            response!!.contains(
                "Geographic search area not yet supported:"
            )
        )

        assertTrue(
            response.contains("Old kampala")
        )

        assertTrue(
            response.contains("No map search was performed")
        )

        assertFalse(
            response.contains("MarketNews")
        )

        assertEquals(0, provider.calls)
    }

    @Test
    fun unsupportedAreasNeverCallGeographicProvider() = runBlocking {
        val provider = CountingProvider()
        val handler = KampalaExplicitGeoDiscovery(provider)

        val queries = listOf(
            "Find hospitals in Old Kampala?",
            "show me banks in Entebbe",
            "find pharmacies in imaginary town",
            "map search hotels in Mbarara"
        )

        for (query in queries) {
            val response = handler.answerOrNull(query)

            assertNotNull(query, response)

            assertTrue(
                query,
                response!!.contains(
                    "Geographic search area not yet supported:"
                )
            )
        }

        assertEquals(0, provider.calls)
    }

    @Test
    fun ordinaryConversationStillFallsThrough() = runBlocking {
        val provider = CountingProvider()
        val handler = KampalaExplicitGeoDiscovery(provider)

        val messages = listOf(
            "Hello Jarvis",
            "Explain Kotlin coroutines",
            "Call the police",
            "What is the weather?",
            "Find inspiration in life",
            "find private homes in central kampala"
        )

        for (message in messages) {
            assertNull(
                message,
                handler.answerOrNull(message)
            )
        }

        assertEquals(0, provider.calls)
    }

    @Test
    fun ntindaStillReachesGeographicProvider() = runBlocking {
        val provider = CountingProvider()
        val handler = KampalaExplicitGeoDiscovery(provider)

        val response = handler.answerOrNull(
            "find hospitals in ntinda"
        )

        assertNotNull(response)

        assertFalse(
            response!!.contains(
                "Geographic search area not yet supported:"
            )
        )

        assertEquals(1, provider.calls)
    }

    @Test
    fun centralKampalaStillReachesGeographicProvider() = runBlocking {
        val provider = CountingProvider()
        val handler = KampalaExplicitGeoDiscovery(provider)

        val response = handler.answerOrNull(
            "find hospitals in central kampala"
        )

        assertNotNull(response)

        assertFalse(
            response!!.contains(
                "Geographic search area not yet supported:"
            )
        )

        assertEquals(1, provider.calls)
    }

    @Test
    fun oldKampalaRemainsAbsentFromRegisteredAreas() {
        assertNull(
            KampalaGeoAreaResolver.DEFAULT.resolve("Old Kampala")
        )

        assertNotNull(
            KampalaGeoAreaResolver.DEFAULT.resolve("Ntinda")
        )

        assertNotNull(
            KampalaGeoAreaResolver.DEFAULT.resolve("Central Kampala")
        )
    }
}