package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KampalaEvidenceAndRelevancePolicyTest {

    @Test
    fun sixHillsQuestionRequiresEvidence() {
        assertEquals(
            KampalaEvidenceAndRelevancePolicy.HILLS_NOTICE,
            KampalaEvidenceAndRelevancePolicy.notice(
                "What can you tell me about the 6 hills of Kampala?"
            )
        )
    }

    @Test
    fun sevenHillsQuestionRequiresEvidence() {
        assertEquals(
            KampalaEvidenceAndRelevancePolicy.HILLS_NOTICE,
            KampalaEvidenceAndRelevancePolicy.notice(
                "How about the 7 hills of Kampala?"
            )
        )
    }

    @Test
    fun hillNumberVariationRequiresEvidence() {
        assertEquals(
            KampalaEvidenceAndRelevancePolicy.HILLS_NOTICE,
            KampalaEvidenceAndRelevancePolicy.notice(
                "List 12 hills in Kampala"
            )
        )
    }

    @Test
    fun normalDirectoryQueryIsNotIntercepted() {
        assertNull(
            KampalaEvidenceAndRelevancePolicy.notice(
                "Find hotels in Kampala"
            )
        )
    }

    @Test
    fun ordinaryTechnicalQuestionIsNotIntercepted() {
        assertNull(
            KampalaEvidenceAndRelevancePolicy.notice(
                "Explain dependency injection in Kotlin"
            )
        )
    }

    @Test
    fun locationQuestionRejectsInternalToolDiscussion() {
        val response = "Questions: How can I call the traffic tool " +
            "using the JSON API?"

        assertEquals(
            KampalaEvidenceAndRelevancePolicy.LOCATION_FAILURE,
            KampalaEvidenceAndRelevancePolicy.review(
                "Where is Kampala?",
                response
            )
        )
    }

    @Test
    fun locationQuestionRejectsUnrelatedGeography() {
        assertEquals(
            KampalaEvidenceAndRelevancePolicy.LOCATION_FAILURE,
            KampalaEvidenceAndRelevancePolicy.review(
                "Where is Kampala?",
                "It is situated in Cambodia."
            )
        )
    }

    @Test
    fun correctLocationResponsePasses() {
        val answer = "Kampala is the capital city of Uganda."

        assertEquals(
            answer,
            KampalaEvidenceAndRelevancePolicy.review(
                "Where is Kampala?",
                answer
            )
        )
    }

    @Test
    fun unrelatedTechnicalResponseIsPreserved() {
        val answer = "Constructor injection supplies dependencies directly."

        assertEquals(
            answer,
            KampalaEvidenceAndRelevancePolicy.review(
                "Explain dependency injection in Kotlin",
                answer
            )
        )
    }
}