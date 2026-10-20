package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaDiscoveryIntentExpansionTest {

    @Test
    fun famousFoodPointsUseVerifiedRestaurantDirectory() {
        val result = KampalaDiscoveryCoveragePolicy.searchRequest(
            "what are the famous food points in kampala"
        )
        assertEquals("restaurant", result?.category)
        assertNull(result?.query)
    }

    @Test
    fun commonRestaurantPhrasingsUseVerifiedDirectory() {
        listOf(
            "What are the best restaurants in Kampala?",
            "Show me food places in Kampala",
            "Recommend places to eat in Kampala",
            "List restaurants in Kampala"
        ).forEach {
            assertEquals(
                "restaurant",
                KampalaDiscoveryCoveragePolicy.searchRequest(it)?.category
            )
        }
    }

    @Test
    fun cinemaRankingDoesNotReachUngroundedModel() {
        val notice = KampalaDiscoveryCoveragePolicy.notice(
            "what are the best cinema halls in Kampala"
        )
        assertNotNull(notice)
        assertTrue(notice!!.contains("verified cinema listings"))
        assertFalse(notice.contains("most affordable"))
    }

    @Test
    fun otherCinemaRequestsAlsoReceiveCoverageNotice() {
        listOf(
            "Find cinemas in Kampala",
            "List movie theatres in Kampala",
            "Which are the popular cinema halls in Kampala?"
        ).forEach {
            assertNotNull(KampalaDiscoveryCoveragePolicy.notice(it))
        }
    }

    @Test
    fun differentFootballGroundsReceiveCoverageNotice() {
        val notice = KampalaDiscoveryCoveragePolicy.notice(
            "what are different football grounds in kampala"
        )
        assertNotNull(notice)
        assertTrue(notice!!.contains("don't exist"))
    }

    @Test
    fun shortFootballRequestReceivesCoverageNotice() {
        val notice = KampalaDiscoveryCoveragePolicy.notice(
            "football grounds"
        )
        assertNotNull(notice)
        assertTrue(notice!!.contains("verified directory"))
    }

    @Test
    fun originalSportsNoticeRemainsAvailable() {
        assertNotNull(
            KampalaDiscoveryCoveragePolicy.notice(
                "tell me a list of playground or football grounds in kampala"
            )
        )
    }

    @Test
    fun existingShoppingMappingRemainsAvailable() {
        assertEquals(
            "shopping",
            KampalaDiscoveryCoveragePolicy.searchRequest(
                "tell me about the different shopping places in kampala"
            )?.category
        )
    }

    @Test
    fun hotelAndHistoryRequestsAreUnchanged() {
        listOf(
            "Find hotels in Kampala",
            "Tell me about Kampala history"
        ).forEach {
            assertNull(KampalaDiscoveryCoveragePolicy.searchRequest(it))
            assertNull(KampalaDiscoveryCoveragePolicy.notice(it))
        }
    }

    @Test
    fun generalEducationalQuestionsAreNotIntercepted() {
        listOf(
            "Explain dependency injection",
            "What is a cinema?",
            "Explain the history of football in Uganda",
            "Write Kotlin code for a restaurant app"
        ).forEach {
            assertNull(KampalaDiscoveryCoveragePolicy.searchRequest(it))
            assertNull(KampalaDiscoveryCoveragePolicy.notice(it))
        }
    }

    @Test
    fun phoneActionsAreNotIntercepted() {
        listOf(
            "Open Android Settings",
            "Call the cinema",
            "Send a message about football grounds",
            "Navigate to the stadium"
        ).forEach {
            assertNull(KampalaDiscoveryCoveragePolicy.searchRequest(it))
            assertNull(KampalaDiscoveryCoveragePolicy.notice(it))
        }
    }

    @Test
    fun existingParliamentAndWorshipNoticesRemainAvailable() {
        assertNotNull(
            KampalaDiscoveryCoveragePolicy.notice(
                "where is the parliament found in kampala"
            )
        )
        assertNotNull(
            KampalaDiscoveryCoveragePolicy.notice(
                "tell me about the different worship places in kampala"
            )
        )
    }
}
