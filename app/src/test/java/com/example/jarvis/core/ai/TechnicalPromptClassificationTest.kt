package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class TechnicalPromptClassificationTest {

    private fun conversational(query: String) {
        assertEquals(
            "Unexpected mode for: $query",
            PromptModePolicy.Mode.CONVERSATION,
            PromptModePolicy.select(query)
        )
    }

    private fun toolAware(query: String) {
        assertEquals(
            "Unexpected mode for: $query",
            PromptModePolicy.Mode.TOOL_AWARE,
            PromptModePolicy.select(query)
        )
    }

    @Test
    fun technicalLearningQuestionsStayConversational() {
        conversational("Explain Kotlin variables")
        conversational("What is a Kotlin data class?")
        conversational("How does Kotlin null safety work?")
    }

    @Test
    fun requestsForCodeStayConversational() {
        conversational("Write a Kotlin function")
        conversational("Show me how to create a list in Kotlin")
        conversational("Give me an example of a suspend function")
    }

    @Test
    fun technicalWordsDoNotAutomaticallyTriggerDeviceTools() {
        conversational("Explain how to open a file in Kotlin")
        conversational("Write a function to send an HTTP request")
        conversational("How do I call a suspend function?")
    }

    @Test
    fun actualDeviceActionsRemainToolAware() {
        toolAware("Call +12025550100")
        toolAware("Open WhatsApp")
        toolAware("Launch the calculator app")
    }

    @Test
    fun weatherRequestsRemainToolAware() {
        toolAware("What's the weather in Kampala?")
        toolAware("Show me today's weather forecast")
    }

    @Test
    fun ambiguousCommandsRemainConservative() {
        toolAware("")
        toolAware("Do something for me")
    }
}