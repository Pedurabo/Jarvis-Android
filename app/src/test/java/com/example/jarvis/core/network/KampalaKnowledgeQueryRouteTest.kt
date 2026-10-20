package com.example.jarvis.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KampalaKnowledgeQueryRouteTest {

    @Test
    fun recognizesOriginalHistoryQuestion() {
        assertEquals(
            KampalaKnowledgeQueryRoute.Topic.HISTORY,
            KampalaKnowledgeQueryRoute.resolve(
                "Tell me about Kampala history"
            )
        )
    }

    @Test
    fun recognizesHistoryOfKampala() {
        assertEquals(
            KampalaKnowledgeQueryRoute.Topic.HISTORY,
            KampalaKnowledgeQueryRoute.resolve(
                "What is the history of Kampala?"
            )
        )
    }

    @Test
    fun recognizesShortHistoryRequest() {
        assertEquals(
            KampalaKnowledgeQueryRoute.Topic.HISTORY,
            KampalaKnowledgeQueryRoute.resolve(
                "Give me a brief history of Kampala"
            )
        )
    }

    @Test
    fun recognizesCaseAndWhitespaceVariations() {
        assertEquals(
            KampalaKnowledgeQueryRoute.Topic.HISTORY,
            KampalaKnowledgeQueryRoute.resolve(
                "  KAMPALA HISTORY!  "
            )
        )
    }

    @Test
    fun recognizesHistoricalBackground() {
        assertEquals(
            KampalaKnowledgeQueryRoute.Topic.HISTORY,
            KampalaKnowledgeQueryRoute.resolve(
                "Explain the historical background of Kampala"
            )
        )
    }

    @Test
    fun unrelatedUgandaHistoryDoesNotMatch() {
        assertNull(
            KampalaKnowledgeQueryRoute.resolve(
                "Tell me about Uganda's history"
            )
        )
    }

    @Test
    fun placesDirectoryRequestsDoNotMatch() {
        assertNull(
            KampalaKnowledgeQueryRoute.resolve(
                "Find a mosque in Old Kampala"
            )
        )
    }

    @Test
    fun unrelatedLocationQuestionsDoNotMatch() {
        assertNull(
            KampalaKnowledgeQueryRoute.resolve(
                "Where is Acacia Mall?"
            )
        )
    }

    @Test
    fun emptyRequestsDoNotMatch() {
        assertNull(KampalaKnowledgeQueryRoute.resolve("  "))
    }

    @Test
    fun arbitraryKampalaQuestionsDoNotMatch() {
        assertNull(
            KampalaKnowledgeQueryRoute.resolve(
                "Find hotels in Kampala"
            )
        )
    }
}