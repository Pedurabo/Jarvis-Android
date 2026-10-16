package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class PromptModePolicyTest {

    private fun conversation(query: String) {
        assertEquals(
            PromptModePolicy.Mode.CONVERSATION,
            PromptModePolicy.select(query)
        )
    }

    private fun toolAware(query: String) {
        assertEquals(
            PromptModePolicy.Mode.TOOL_AWARE,
            PromptModePolicy.select(query)
        )
    }

    @Test
    fun introductionUsesConversation() {
        conversation("Introduce yourself in two sentences.")
    }

    @Test
    fun programmingExplanationsUseConversation() {
        conversation("Explain Jetpack Compose in three sentences.")
        conversation("Explain Kotlin val versus var.")
        conversation("What is dependency injection?")
    }

    @Test
    fun trafficAndWeatherStayToolAware() {
        toolAware("What's the traffic in Kampala?")
        toolAware("What's the weather today?")
        toolAware("Explain today's weather forecast.")
    }

    @Test
    fun deviceCommandsStayToolAware() {
        toolAware("Open WhatsApp")
        toolAware("Dial +12025550100")
        toolAware("Please launch YouTube")
        toolAware("Turn on the flashlight")
    }

    @Test
    fun potentiallySensitiveRequestsStayToolAware() {
        toolAware("Send a message")
        toolAware("What is my current location?")
        toolAware("Explain how to send an SMS")
    }

    @Test
    fun unknownRequestsDefaultToToolAware() {
        toolAware("")
        toolAware("Do something for me")
        toolAware("Kampala")
    }

    @Test
    fun simpleExplanationPreservesConversationMode() {
        conversation("Please explain dependency injection.")
        conversation("Tell me about Kotlin coroutines.")
    }
}