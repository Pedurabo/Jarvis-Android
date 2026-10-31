package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class JarvisConversationStabilityPolicyTest {

    @Test
    fun nakawaFollowUpWithDoubleQuestionMark() {
        assertEquals(
            "find banks in nakawa",
            JarvisConversationStabilityPolicy.normalizeGeographicRequest(
                "how about banks in nakawa, can you find those??"
            )
        )
    }

    @Test
    fun nakawaQuestionMarkFollowUp() {
        assertEquals(
            "find banks in nakawa",
            JarvisConversationStabilityPolicy.normalizeGeographicRequest(
                "how about banks in nakawa? can you find those?"
            )
        )
    }

    @Test
    fun nakawaPeriodFollowUp() {
        assertEquals(
            "find banks in nakawa",
            JarvisConversationStabilityPolicy.normalizeGeographicRequest(
                "how about banks in nakawa. can you find those?"
            )
        )
    }

    @Test
    fun oldKampalaDepositRequest() {
        assertEquals(
            "find banks in old kampala",
            JarvisConversationStabilityPolicy.normalizeGeographicRequest(
                "am trying to deposit some money but the arms aren't working, " +
                    "can you find me banks in old kampala?"
            )
        )
    }

    @Test
    fun oldKampalaAtmRequest() {
        assertEquals(
            "find banks in old kampala",
            JarvisConversationStabilityPolicy.normalizeGeographicRequest(
                "trying to deposit some money but the atms aren't working, " +
                    "can you find me banks in old kampala?"
            )
        )
    }

    @Test
    fun knownGoodCommandsRemainUnchanged() {
        val commands = listOf(
            "find hospitals in ntinda",
            "find banks in nakawa?",
            "find banks in old kampala"
        )

        for (command in commands) {
            assertEquals(
                command,
                JarvisConversationStabilityPolicy
                    .normalizeGeographicRequest(command)
            )
        }
    }

    @Test
    fun ordinaryConversationRemainsUnchanged() {
        val question = "Explain Kotlin coroutines"

        assertEquals(
            question,
            JarvisConversationStabilityPolicy
                .normalizeGeographicRequest(question)
        )
    }

    @Test
    fun fabricatedTravelTimeIsBlocked() {
        assertEquals(
            JarvisConversationStabilityPolicy.ROUTE_SAFETY_RESPONSE,
            JarvisConversationStabilityPolicy.reviewModelAnswer(
                "Get me to Green Hill Academy then Kingdom Kampala",
                "The journey will take around 30 minutes.",
                0
            )
        )
    }

    @Test
    fun unsolicitedNavigationCodeIsBlocked() {
        assertEquals(
            JarvisConversationStabilityPolicy.OUTPUT_SAFETY_RESPONSE,
            JarvisConversationStabilityPolicy.reviewModelAnswer(
                "Let's proceed with the journey",
                "Note: This code snippet provides a basic implementation " +
                    "of AI-powered navigation.",
                0
            )
        )
    }

    @Test
    fun requestedCodeExplanationIsPreserved() {
        val answer = "This code snippet demonstrates Kotlin coroutines."

        assertEquals(
            answer,
            JarvisConversationStabilityPolicy.reviewModelAnswer(
                "Explain this Kotlin code snippet",
                answer,
                0
            )
        )
    }

    @Test
    fun regularAnswersRemainUnchanged() {
        val answer = "Hello! What can I help you with?"

        assertEquals(
            answer,
            JarvisConversationStabilityPolicy.reviewModelAnswer(
                "Hello Jarvis",
                answer,
                0
            )
        )
    }
}