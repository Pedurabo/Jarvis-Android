package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaDiscoveryCoveragePolicyTest {

    @Test
    fun shoppingDescriptionUsesVerifiedDirectoryCategory() {
        val route = KampalaDiscoveryCoveragePolicy.searchRequest(
            "tell me about the different shopping places in kampala"
        )

        assertNotNull(route)
        assertEquals("shopping", route?.category)
        assertNull(route?.query)
    }

    @Test
    fun shoppingPhraseAcceptsPunctuation() {
        val route = KampalaDiscoveryCoveragePolicy.searchRequest(
            "Tell me about shopping places in Kampala?"
        )
        assertEquals("shopping", route?.category)
    }

    @Test
    fun parliamentLocationDoesNotFallThroughToModel() {
        val notice = KampalaDiscoveryCoveragePolicy.notice(
            "where is the parliament found in kampala"
        )

        assertNotNull(notice)
        assertTrue(notice!!.contains("can't currently confirm"))
        assertFalse(notice.contains("Plot "))
    }

    @Test
    fun worshipDirectoryCoverageIsDescribedAccurately() {
        val notice = KampalaDiscoveryCoveragePolicy.notice(
            "tell me about the different worship places in kampala"
        )

        assertNotNull(notice)
        assertTrue(notice!!.contains("verified combined directory"))
    }

    @Test
    fun sportsMissingDataDoesNotMeanMissingFacilities() {
        val notice = KampalaDiscoveryCoveragePolicy.notice(
            "tell me a list of playground or football grounds in kampala"
        )

        assertNotNull(notice)
        assertTrue(notice!!.contains("don't exist"))
    }

    @Test
    fun roadRequestDoesNotGenerateUnverifiedNetworkDetails() {
        val notice = KampalaDiscoveryCoveragePolicy.notice(
            "tell me about the different roads in kampala"
        )

        assertNotNull(notice)
        assertTrue(notice!!.contains("won't guess"))
    }

    @Test
    fun existingHotelDiscoveryRemainsUnaffected() {
        assertNull(
            KampalaDiscoveryCoveragePolicy.searchRequest(
                "Find hotels in Kampala"
            )
        )
        assertNull(
            KampalaDiscoveryCoveragePolicy.notice(
                "Find hotels in Kampala"
            )
        )
    }

    @Test
    fun existingHistoryRequestRemainsUnaffected() {
        assertNull(
            KampalaDiscoveryCoveragePolicy.notice(
                "Tell me about Kampala history"
            )
        )
    }

    @Test
    fun phoneCommandsAreNotIntercepted() {
        listOf(
            "Open Android Settings",
            "Call the Parliament office",
            "Send a message about Kampala roads"
        ).forEach {
            assertNull(KampalaDiscoveryCoveragePolicy.notice(it))
            assertNull(KampalaDiscoveryCoveragePolicy.searchRequest(it))
        }
    }

    @Test
    fun unrelatedProgrammingQuestionsAreNotIntercepted() {
        assertNull(
            KampalaDiscoveryCoveragePolicy.notice(
                "Explain Kotlin dependency injection"
            )
        )
    }
}
