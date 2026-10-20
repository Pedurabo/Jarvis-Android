package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KampalaTrustedFactPolicyTest {

    private val trustedLocation =
        "Kampala is the capital city of Uganda, " +
            "a country in East Africa."

    @Test
    fun answersDirectLocationQuestion() {
        assertEquals(
            trustedLocation,
            KampalaTrustedFactPolicy.answer("Where is Kampala?")
        )
    }

    @Test
    fun answersLocationVariation() {
        assertEquals(
            trustedLocation,
            KampalaTrustedFactPolicy.answer(
                "Where is Kampala located?"
            )
        )
    }

    @Test
    fun answersCountryQuestion() {
        assertEquals(
            trustedLocation,
            KampalaTrustedFactPolicy.answer(
                "Which country is Kampala in?"
            )
        )
    }

    @Test
    fun correctsIrrelevantModelOutput() {
        val irrelevant =
            "How do I call the traffic tool using the JSON API?"

        assertEquals(
            trustedLocation,
            KampalaTrustedFactPolicy.review(
                "Where is Kampala?",
                irrelevant
            )
        )
    }

    @Test
    fun preservesHistoryForKnowledgeRoute() {
        assertNull(
            KampalaTrustedFactPolicy.answer(
                "Tell me about Kampala history"
            )
        )
    }

    @Test
    fun doesNotInventHillLists() {
        assertNull(
            KampalaTrustedFactPolicy.answer(
                "What are the 6 hills of Kampala?"
            )
        )
    }

    @Test
    fun doesNotInterceptRestaurantDiscovery() {
        assertNull(
            KampalaTrustedFactPolicy.answer(
                "Recommend restaurants in Kololo"
            )
        )
    }

    @Test
    fun doesNotInterceptDeviceActions() {
        assertNull(
            KampalaTrustedFactPolicy.answer(
                "Open Android Settings"
            )
        )
    }

    @Test
    fun preservesTechnicalExplanations() {
        val answer =
            "Constructor injection supplies an object with dependencies."

        assertEquals(
            answer,
            KampalaTrustedFactPolicy.review(
                "Explain dependency injection in Kotlin",
                answer
            )
        )
    }

    @Test
    fun doesNotMisinterpretLongerPlaceNames() {
        assertNull(
            KampalaTrustedFactPolicy.answer(
                "Where is Kampala Road?"
            )
        )
    }
}