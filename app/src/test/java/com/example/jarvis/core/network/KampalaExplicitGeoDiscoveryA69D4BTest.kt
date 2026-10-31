package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KampalaExplicitGeoDiscoveryA69D4BTest {

    @Test
    fun ordinaryCentralKampalaHospitalRequestUsesGeoBounds() {
        val original = KampalaExplicitGeoDiscovery.parseOrNull(
            "map search hospitals in central kampala"
        )
        val ordinary = KampalaExplicitGeoDiscovery.parseOrNull(
            "find hospitals in central kampala"
        )

        assertNotNull(original)
        assertNotNull(ordinary)
        assertEquals("hospital", ordinary!!.category)
        assertEquals(original!!.bounds, ordinary.bounds)
        assertEquals("Central Kampala", ordinary.label)
        assertEquals(10, ordinary.toRequest().pageSize)
        assertNull(ordinary.toRequest().query)
        assertTrue(
            KampalaGeoDiscoveryContract.validRequest(
                ordinary.toRequest()
            )
        )
    }

    @Test
    fun unknownNeighborhoodIsNotMappedToCentralKampala() {
        assertNull(
            KampalaExplicitGeoDiscovery.parseOrNull(
                "find hospitals in Kololo"
            )
        )
    }

    @Test
    fun emergencyMessageDoesNotBecomeGeographicSearch() {
        assertNull(
            KampalaExplicitGeoDiscovery.parseOrNull(
                "I was bitten by a dog and need a hospital urgently"
            )
        )
    }
    private fun fake(
        response: KampalaGeoDiscoveryContract.Result,
        onCall: () -> Unit = {}
    ): KampalaGeoDiscoveryContract.Provider =
        object : KampalaGeoDiscoveryContract.Provider {
            override suspend fun search(
                request: KampalaGeoDiscoveryContract.SearchRequest
            ): KampalaGeoDiscoveryContract.Result {
                onCall()
                return response
            }
        }

    private fun emptyResult() =
        KampalaGeoDiscoveryContract.Result.Success(
            KampalaGeoDiscoveryContract.Page(
                provider = "OpenStreetMap/Overpass",
                places = emptyList(),
                nextPageToken = null
            )
        )

    @Test
    fun explicitHospitalCommandIsRecognized() {
        val command =
            KampalaExplicitGeoDiscovery.parseOrNull(
                "map search hospitals in central kampala"
            )

        assertNotNull(command)
        assertEquals("hospital", command!!.category)
        assertEquals(10, command.toRequest().pageSize)
        assertNull(command.toRequest().query)
    }

    @Test
    fun supportedCommandsHaveValidRequests() {
        val commands = listOf(
            "map search hospitals in central kampala",
            "map search pharmacies in central kampala",
            "map search banks in central kampala",
            "map search schools in central kampala"
        )

        commands.forEach { text ->
            val command =
                KampalaExplicitGeoDiscovery.parseOrNull(text)

            assertNotNull(command)
            assertTrue(
                KampalaGeoDiscoveryContract.validRequest(
                    command!!.toRequest()
                )
            )
        }
    }

    @Test
    fun normalNavigationDoesNotTriggerGeoSearch() {
        assertNull(
            KampalaExplicitGeoDiscovery.parseOrNull(
                "How do I get to Acacia Mall?"
            )
        )
    }

    @Test
    fun normalEmergencyDoesNotTriggerGeoSearch() {
        assertNull(
            KampalaExplicitGeoDiscovery.parseOrNull(
                "A dog has bitten me, what should I do?"
            )
        )
    }

    @Test
    fun unsupportedAreaDoesNotTriggerGeoSearch() {
        assertNull(
            KampalaExplicitGeoDiscovery.parseOrNull(
                "map search hospitals in Entebbe"
            )
        )
    }

    @Test
    fun freeTextCannotEnterProviderRequest() {
        assertNull(
            KampalaExplicitGeoDiscovery.parseOrNull(
                "map search hospitals; out; in central kampala"
            )
        )
    }

    @Test
    fun unrelatedMessagesDoNotCallProvider() = runBlocking {
        var calls = 0

        val handler = KampalaExplicitGeoDiscovery(
            fake(emptyResult()) { calls++ }
        )

        val answer = handler.answerOrNull(
            "How do I get to Watoto Church Downtown?"
        )

        assertNull(answer)
        assertEquals(0, calls)
    }

    @Test
    fun explicitCommandInvokesProviderOnce() = runBlocking {
        var calls = 0

        val handler = KampalaExplicitGeoDiscovery(
            fake(emptyResult()) { calls++ }
        )

        val answer = handler.answerOrNull(
            "map search hospitals in central kampala"
        )

        assertNotNull(answer)
        assertEquals(1, calls)
        assertTrue(answer!!.contains("predefined bounds"))
        assertTrue(answer.contains("does not mean"))
    }

    @Test
    fun providerFailureHasExplicitNotice() = runBlocking {
        val handler = KampalaExplicitGeoDiscovery(
            fake(
                KampalaGeoDiscoveryContract.Result.Unavailable(
                    "HTTP 502"
                )
            )
        )

        val answer = handler.answerOrNull(
            "map search hospitals in central kampala"
        )

        assertNotNull(answer)
        assertTrue(answer!!.contains("currently unavailable"))
        assertFalse(answer.contains("No hospital listings"))
    }

    @Test
    fun invalidProviderDataFailsClosed() = runBlocking {
        val handler = KampalaExplicitGeoDiscovery(
            fake(
                KampalaGeoDiscoveryContract.Result.Success(
                    KampalaGeoDiscoveryContract.Page(
                        provider = "",
                        places = emptyList(),
                        nextPageToken = null
                    )
                )
            )
        )

        val answer = handler.answerOrNull(
            "map search banks in central kampala"
        )

        assertNotNull(answer)
        assertTrue(answer!!.contains("currently unavailable"))
    }
}