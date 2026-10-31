package com.example.jarvis.core.ai

import com.example.jarvis.core.network.KampalaGeoDiscoveryContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class KampalaP3B1DWaypointSearchTest {

    private fun handler(
        requests: MutableList<KampalaGeoDiscoveryContract.SearchRequest>
    ): KampalaP3B1DWaypointSearch =
        KampalaP3B1DWaypointSearch { request ->
            requests += request

            KampalaGeoDiscoveryContract.Result.Success(
                KampalaGeoDiscoveryContract.Page(
                    provider = "Test OpenStreetMap listing source",
                    places = emptyList(),
                    nextPageToken = null
                )
            )
        }

    @Test
    fun explicitJourneySearchPreservesWaypointOrder() = runBlocking {
        val requests = mutableListOf<
            KampalaGeoDiscoveryContract.SearchRequest
        >()

        val response = handler(requests).answerOrNull(
            "map journey in Central Kampala: " +
                "from Marriott Nsambya via Green Hill Academy " +
                "to Kingdom Kampala"
        )

        assertNotNull(response)
        assertEquals(
            listOf(
                "Marriott Nsambya",
                "Green Hill Academy",
                "Kingdom Kampala"
            ),
            requests.map { it.query }
        )

        assertTrue(response!!.contains("From: Marriott Nsambya"))
        assertTrue(response.contains("Stop 1: Green Hill Academy"))
        assertTrue(response.contains("To: Kingdom Kampala"))
        assertTrue(response.contains("within the searched area"))
        assertFalse(response.contains("Open directions:"))
        assertFalse(response.contains("google.com/maps"))
    }

    @Test
    fun ordinaryJourneyDoesNotInvokeExperimentalSearch() =
        runBlocking {
            val requests = mutableListOf<
                KampalaGeoDiscoveryContract.SearchRequest
            >()

            val response = handler(requests).answerOrNull(
                "from Marriott Nsambya via Green Hill Academy " +
                    "to Kingdom Kampala"
            )

            assertNull(response)
            assertTrue(requests.isEmpty())
        }

    @Test
    fun unknownAreaFailsWithoutSearching() = runBlocking {
        val requests = mutableListOf<
            KampalaGeoDiscoveryContract.SearchRequest
        >()

        val response = handler(requests).answerOrNull(
            "map journey in Unregistered Location: " +
                "from Nsambya via Kololo to Nakasero"
        )

        assertNotNull(response)
        assertTrue(response!!.contains("not registered"))
        assertTrue(requests.isEmpty())
        assertFalse(response.contains("Open directions:"))
    }

    @Test
    fun incompleteJourneyFailsWithoutSearching() = runBlocking {
        val requests = mutableListOf<
            KampalaGeoDiscoveryContract.SearchRequest
        >()

        val response = handler(requests).answerOrNull(
            "map journey in Central Kampala: from Nsambya"
        )

        assertNotNull(response)
        assertTrue(requests.isEmpty())
        assertFalse(response!!.contains("Open directions:"))
    }

    @Test
    fun multipleStopsRetainRequestedSequence() = runBlocking {
        val requests = mutableListOf<
            KampalaGeoDiscoveryContract.SearchRequest
        >()

        handler(requests).answerOrNull(
            "map journey in Central Kampala: " +
                "from Nsambya via Ntinda via Kololo to Nakasero"
        )

        assertEquals(
            listOf("Nsambya", "Ntinda", "Kololo", "Nakasero"),
            requests.map { it.query }
        )
    }

    @Test
    fun providerFailureDoesNotGenerateRoute() = runBlocking {
        val broken = KampalaP3B1DWaypointSearch {
            throw IllegalStateException("Network unavailable")
        }

        val response = broken.answerOrNull(
            "map journey in Central Kampala: " +
                "from Nsambya via Kololo to Nakasero"
        )

        assertNotNull(response)
        assertFalse(response!!.contains("Open directions:"))
        assertFalse(response.contains("google.com/maps"))
    }

    @Test
    fun unrelatedConversationIsNotIntercepted() = runBlocking {
        val requests = mutableListOf<
            KampalaGeoDiscoveryContract.SearchRequest
        >()

        assertNull(
            handler(requests).answerOrNull(
                "How does Kotlin coroutine cancellation work?"
            )
        )

        assertTrue(requests.isEmpty())
    }
}