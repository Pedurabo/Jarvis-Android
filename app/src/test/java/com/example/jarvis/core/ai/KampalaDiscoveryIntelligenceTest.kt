package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaDiscoveryIntelligenceTest {

    private fun search(input: String) =
        KampalaDiscoveryIntelligence.decide(input)
            as? KampalaDiscoveryIntelligence.Decision.Search

    private fun notice(input: String) =
        KampalaDiscoveryIntelligence.decide(input)
            as? KampalaDiscoveryIntelligence.Decision.Notice

    @Test
    fun naturalFoodPlacesUsesRestaurantDirectory() {
        assertEquals(
            "restaurant",
            search("what are the famous food points in kampala")
                ?.request?.category
        )
    }

    @Test
    fun bestRestaurantsUseVerifiedListingsNotGeneratedRankings() {
        assertEquals(
            "restaurant",
            search("What are the best restaurants in Kampala?")
                ?.request?.category
        )
    }

    @Test
    fun neighbourhoodIsPreserved() {
        val request = search(
            "Recommend restaurants in Kololo"
        )?.request

        assertEquals("restaurant", request?.category)
        assertEquals("Kololo", request?.area)
    }

    @Test
    fun hotelDiscoveryRemainsAvailable() {
        assertEquals(
            "accommodation",
            search("Find hotels in Kampala")?.request?.category
        )
    }

    @Test
    fun shoppingDiscoveryRemainsAvailable() {
        assertEquals(
            "shopping",
            search("What are popular shopping malls in Kampala?")
                ?.request?.category
        )
    }

    @Test
    fun cinemaRequestsDoNotGenerateRankings() {
        val result = notice(
            "what are the best cinema halls in Kampala"
        )

        assertTrue(result != null)
        assertTrue(result!!.message.contains("verified cinema"))
        assertFalse(result.message.contains("Silverbird"))
    }

    @Test
    fun footballGroundVariationsAreRecognized() {
        listOf(
            "football grounds",
            "what are different football grounds in kampala",
            "List football pitches in Kampala",
            "Show me playgrounds in Kampala"
        ).forEach {
            assertTrue(
                "Not recognized: $it",
                notice(it) != null
            )
        }
    }

    @Test
    fun roadsReceiveCoverageNotice() {
        assertTrue(
            notice("What are the major roads in Kampala?") != null
        )
    }

    @Test
    fun existingWorshipNoticeIsRetained() {
        assertTrue(
            notice(
                "tell me about the different worship places in kampala"
            ) != null
        )
    }

    @Test
    fun existingParliamentNoticeIsRetained() {
        assertTrue(
            notice(
                "where is the parliament found in kampala"
            ) != null
        )
    }

    @Test
    fun differentCityIsNotSilentlyReplaced() {
        assertTrue(
            KampalaDiscoveryIntelligence.decide(
                "Recommend restaurants in Nairobi"
            ) is KampalaDiscoveryIntelligence.Decision.Pass
        )
    }

    @Test
    fun programmingQuestionsRemainGeneralAi() {
        listOf(
            "Explain dependency injection",
            "What is a cinema?",
            "Write Kotlin code for a restaurant app",
            "Explain football history"
        ).forEach {
            assertTrue(
                KampalaDiscoveryIntelligence.decide(it)
                    is KampalaDiscoveryIntelligence.Decision.Pass
            )
        }
    }

    @Test
    fun deviceActionsAreNotIntercepted() {
        listOf(
            "Open Android Settings",
            "Call the Parliament office",
            "Send a message about football grounds",
            "Navigate to the stadium",
            "Book a hotel in Kampala"
        ).forEach {
            assertTrue(
                KampalaDiscoveryIntelligence.decide(it)
                    is KampalaDiscoveryIntelligence.Decision.Pass
            )
        }
    }
}
