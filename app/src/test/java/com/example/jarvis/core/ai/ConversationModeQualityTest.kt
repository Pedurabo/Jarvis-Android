package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationModeQualityTest {

    private fun assertConversation(query: String) {
        assertEquals(
            PromptModePolicy.Mode.CONVERSATION,
            PromptModePolicy.select(query)
        )
    }

    private fun assertToolAware(query: String) {
        assertEquals(
            PromptModePolicy.Mode.TOOL_AWARE,
            PromptModePolicy.select(query)
        )
    }

    private fun prompt(query: String, tools: String): String {
        return JarvisPromptBuilder().build(
            query = query,
            chatHistory = "",
            retrievedContext = "",
            deviceContext = "{}",
            tools = tools,
            stepHistory = emptyList(),
            route = ModelRouter.Route.LOCAL
        )
    }

    @Test
    fun socialQuestionsUseConversationMode() {
        assertConversation("How are you today?")
        assertConversation("How are you?")
        assertConversation("How are you doing?")
        assertConversation("How's it going?")
    }

    @Test
    fun greetingsUseConversationMode() {
        assertConversation("Hello Jarvis")
        assertConversation("Hi!")
        assertConversation("Good morning")
        assertConversation("Good evening, Jarvis".replace(",", ""))
    }

    @Test
    fun ordinaryProgrammingQuestionsRemainConversation() {
        assertConversation("Explain Kotlin val versus var")
        assertConversation("Explain dependency injection")
    }

    @Test
    fun phoneActionsRemainToolAware() {
        assertToolAware("Open WhatsApp")
        assertToolAware("Dial +12025550100")
        assertToolAware("Send a message")
        assertToolAware("Turn on the flashlight")
    }

    @Test
    fun serviceRequestsRemainToolAware() {
        assertToolAware("What's the traffic in Kampala?")
        assertToolAware("What's the weather today?")
    }

    @Test
    fun conversationPromptRejectsInternalNarration() {
        val text = prompt("How are you today?", "")

        assertTrue(text.contains(
            "Do not narrate your internal reasoning"
        ))
        assertTrue(text.contains(
            "answer naturally in one or two sentences"
        ))
        assertFalse(text.contains("PHONE AUTOMATION TOOL CONTRACT"))
        assertFalse(text.contains("YOU MUST CALL"))
    }

    @Test
    fun toolAwarePromptPreservesDeviceContract() {
        val text = prompt(
            "Open Android Settings",
            "PhoneAutomation: authorized application controls"
        )

        assertTrue(text.contains("PHONE AUTOMATION TOOL CONTRACT"))
        assertTrue(text.contains("application-controlled authorization"))
    }
}
