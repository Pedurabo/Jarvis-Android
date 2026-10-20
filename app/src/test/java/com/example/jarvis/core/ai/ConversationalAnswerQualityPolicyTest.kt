package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationalAnswerQualityPolicyTest {

    @Test
    fun helloCannotTurnIntoUnrelatedArticle() {
        val result = ConversationalAnswerQualityPolicy.review(
            "Hello Jarvis",
            "What is the purpose of the internet? The internet is a vast network."
        )
        assertEquals("Hello! How can I help you today?", result)
    }

    @Test
    fun greetingsAreShortAndPredictable() {
        listOf("Hi", "Hello!", "Hey Jarvis", "Good morning").forEach {
            val result = ConversationalAnswerQualityPolicy.review(it, "irrelevant")
            assertEquals("Hello! How can I help you today?", result)
        }
    }

    @Test
    fun socialQuestionCannotExposeToolNarration() {
        val result = ConversationalAnswerQualityPolicy.review(
            "How are you today?",
            "Assistant takes a moment to analyze the context and suggests the appropriate tool."
        )
        assertEquals(
            "I'm here and ready to help. How can I assist you today?",
            result
        )
    }

    @Test
    fun socialAnswersRemainBrief() {
        val result = ConversationalAnswerQualityPolicy.review(
            "How are you?",
            "An unrelated answer ".repeat(200)
        )
        assertTrue(result.length < 100)
    }

    @Test
    fun unmistakableInternalNarrationIsRejected() {
        val result = ConversationalAnswerQualityPolicy.review(
            "Explain dependency injection",
            "The assistant will now select a tool based on the context."
        )
        assertTrue(result.contains("couldn't produce a relevant answer"))
    }

    @Test
    fun validProgrammingExplanationIsPreserved() {
        val answer =
            "In Kotlin, val prevents reference reassignment, while var allows reassignment."
        assertEquals(
            answer,
            ConversationalAnswerQualityPolicy.review(
                "Explain Kotlin val versus var", answer
            )
        )
    }

    @Test
    fun requestedProgrammingToolDiscussionIsPreserved() {
        val answer =
            "An Android application may use dependency injection to provide a service."
        assertEquals(
            answer,
            ConversationalAnswerQualityPolicy.review(
                "Explain dependency injection", answer
            )
        )
    }

    @Test
    fun ordinaryKampalaExplanationIsUnchanged() {
        val answer =
            "Kampala is the capital city of Uganda."
        assertEquals(
            answer,
            ConversationalAnswerQualityPolicy.review(
                "What is Kampala?", answer
            )
        )
    }

    @Test
    fun noDeviceAuthorizationIsAdded() {
        val answer = "A phone action requires approval."
        val result = ConversationalAnswerQualityPolicy.review(
            "Explain phone permissions", answer
        )
        assertEquals(answer, result)
        assertFalse(result.contains("Action completed"))
    }
}
