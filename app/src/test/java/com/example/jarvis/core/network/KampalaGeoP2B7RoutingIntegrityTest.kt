package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KampalaGeoP2B7RoutingIntegrityTest {

    private class CountingProvider :
        KampalaGeoDiscoveryContract.Provider {

        var calls = 0

        override suspend fun search(
            request: KampalaGeoDiscoveryContract.SearchRequest
        ): KampalaGeoDiscoveryContract.Result {
            calls++

            return KampalaGeoDiscoveryContract.Result.Success(
                KampalaGeoDiscoveryContract.Page(
                    provider = "OpenStreetMap test fixture",
                    places = emptyList(),
                    nextPageToken = null
                )
            )
        }
    }

    @Test
    fun oldKampalaDoesNotFallThroughOrContactBackend() =
        runBlocking {
            val provider = CountingProvider()
            val handler = KampalaExplicitGeoDiscovery(provider)

            val answer = handler.answerOrNull(
                "find banks in old kampala"
            )

            assertNotNull(answer)
            assertTrue(
                answer!!.contains("not yet supported")
            )
            assertTrue(
                answer.contains("No map search was performed")
            )
            assertEquals(0, provider.calls)
        }

    @Test
    fun unsupportedAreasReturnDeterministicNotice() =
        runBlocking {
            val provider = CountingProvider()
            val handler = KampalaExplicitGeoDiscovery(provider)

            val requests = listOf(
                "find hospitals in Entebbe",
                "show me hotels in Kololo",
                "find banks in Mbarara",
                "find pharmacies in Old Kampala"
            )

            for (request in requests) {
                val answer = handler.answerOrNull(request)

                assertNotNull(request, answer)
                assertTrue(
                    request,
                    answer!!.contains("not yet supported")
                )
            }

            assertEquals(0, provider.calls)
        }

    @Test
    fun ordinaryConversationStillPassesThrough() =
        runBlocking {
            val provider = CountingProvider()
            val handler = KampalaExplicitGeoDiscovery(provider)

            val examples = listOf(
                "Explain Kotlin coroutines",
                "How do I get to Acacia Mall?",
                "What is dependency injection?"
            )

            for (message in examples) {
                assertNull(
                    message,
                    handler.answerOrNull(message)
                )
            }

            assertEquals(0, provider.calls)
        }

    @Test
    fun supportedNtindaSearchStillContactsBackend() =
        runBlocking {
            val provider = CountingProvider()
            val handler = KampalaExplicitGeoDiscovery(provider)

            val answer = handler.answerOrNull(
                "find banks in ntinda"
            )

            assertNotNull(answer)
            assertEquals(1, provider.calls)
            assertTrue(answer!!.contains("Ntinda"))
        }

    @Test
    fun centralKampalaSearchStillContactsBackend() =
        runBlocking {
            val provider = CountingProvider()
            val handler = KampalaExplicitGeoDiscovery(provider)

            val answer = handler.answerOrNull(
                "find hospitals in central kampala"
            )

            assertNotNull(answer)
            assertEquals(1, provider.calls)
        }

    @Test
    fun unsupportedOldKampalaOtherCategoriesAreBlocked() =
        runBlocking {
            val provider = CountingProvider()
            val handler = KampalaExplicitGeoDiscovery(provider)

            val categories = listOf(
                "banks",
                "hospitals",
                "hotels",
                "schools",
                "pharmacies",
                "embassies"
            )

            for (category in categories) {
                val answer = handler.answerOrNull(
                    "find $category in old kampala"
                )

                assertNotNull(category, answer)
                assertTrue(
                    category,
                    answer!!.contains("not yet supported")
                )
            }

            assertEquals(0, provider.calls)
        }

    @Test
    fun unrecognizedCategoryDoesNotTriggerUnsupportedAreaNotice() =
        runBlocking {
            val provider = CountingProvider()
            val handler = KampalaExplicitGeoDiscovery(provider)

            assertNull(
                handler.answerOrNull(
                    "find imaginary objects in central kampala"
                )
            )

            assertEquals(0, provider.calls)
        }
}