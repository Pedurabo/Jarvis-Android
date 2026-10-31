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

    @Test
    fun ugandaCapitalQuestionUsesCuratedAnswer() {
        assertEquals(
            "Kampala is the capital city of Uganda, a country in East Africa.",
            KampalaTrustedFactPolicy.answer(
                "What is the capital city of Uganda?"
            )
        )
    }

    @Test
    fun irrelevantModelAnswerIsReplacedForUgandaCapitalQuestion() {
        assertEquals(
            "Kampala is the capital city of Uganda, a country in East Africa.",
            KampalaTrustedFactPolicy.review(
                "What is the capital city of Uganda?",
                "Uganda is a landlocked country in East Africa."
            )
        )
    }

    @Test
    fun alternativeUgandaCapitalQuestionIsRecognized() {
        assertEquals(
            "Kampala is the capital city of Uganda, a country in East Africa.",
            KampalaTrustedFactPolicy.answer("What is Uganda's capital?")
        )
    }

    @Test
    fun unrelatedUgandaQuestionIsNotIntercepted() {
        val question = "Why is Uganda landlocked?"
        val answer = "Uganda has no coastline and is surrounded by other countries."

        assertNull(KampalaTrustedFactPolicy.answer(question))
        assertEquals(answer, KampalaTrustedFactPolicy.review(question, answer))
    }
}
