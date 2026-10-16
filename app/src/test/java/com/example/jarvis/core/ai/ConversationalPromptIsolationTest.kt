package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class ConversationalPromptIsolationTest {

    private val builder = JarvisPromptBuilder()

    private fun conversationalPrompt(query: String): String =
        builder.build(
            query = query,
            chatHistory = "",
            retrievedContext = "",
            deviceContext = "",
            tools = "",
            stepHistory = emptyList(),
            route = ModelRouter.Route.LOCAL
        )

    @Test
    fun ordinaryQuestionIsPreserved() {
        val prompt = conversationalPrompt(
            "Explain the difference between Kotlin val and var."
        )

        assertTrue(
            prompt.contains(
                "Explain the difference between Kotlin val and var."
            )
        )
    }

    @Test
    fun ordinaryConversationDoesNotIncludePhoneToolContract() {
        val prompt = conversationalPrompt(
            "Introduce yourself in two sentences."
        )

        assertFalse(prompt.contains("PHONE AUTOMATION TOOL CONTRACT"))
        assertFalse(prompt.contains("\"tool\": \"PhoneAutomation\""))
    }

    @Test
    fun ordinaryConversationDoesNotIncludeTrafficToolInstructions() {
        val prompt = conversationalPrompt(
            "Explain Jetpack Compose in three sentences."
        )

        assertFalse(prompt.contains("TOOL CALLING & TRAFFIC"))
        assertFalse(prompt.contains("\"tool\": \"traffic\""))
    }

    @Test
    fun ordinaryConversationPreservesLocalChatTemplate() {
        val prompt = conversationalPrompt("Hello Jarvis.")

        assertTrue(prompt.contains("<|begin_of_text|>"))
        assertTrue(prompt.contains("<|start_header_id|>system"))
        assertTrue(prompt.contains("<|start_header_id|>user"))
        assertTrue(prompt.contains("<|start_header_id|>assistant"))
    }

    @Test
    fun ordinaryConversationPreservesHistory() {
        val prompt = builder.build(
            query = "What did I ask?",
            chatHistory = "user: Tell me about Kotlin.",
            retrievedContext = "",
            deviceContext = "",
            tools = "",
            stepHistory = emptyList(),
            route = ModelRouter.Route.LOCAL
        )

        assertTrue(prompt.contains("user: Tell me about Kotlin."))
        assertTrue(prompt.contains("What did I ask?"))
    }

    @Test
    fun toolAwarePromptRetainsToolDescriptions() {
        val prompt = builder.build(
            query = "What's the weather?",
            chatHistory = "",
            retrievedContext = "",
            deviceContext = "",
            tools = "Weather: fetch current weather information",
            stepHistory = emptyList(),
            route = ModelRouter.Route.LOCAL
        )

        assertTrue(
            prompt.contains("Weather: fetch current weather information")
        )
    }
}