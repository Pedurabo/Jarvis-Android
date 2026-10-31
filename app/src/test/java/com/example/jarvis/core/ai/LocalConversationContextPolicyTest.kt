package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalConversationContextPolicyTest {

    @Test
    fun retainsEarlierTurnsForAnOfflineConversation() {
        val history = listOf(
            "user" to "Where is Hotel Africana?",
            "assistant" to "I cannot verify an exact address.",
            "user" to "How do I get there?"
        )

        val result = LocalConversationContextPolicy.prepare(
            history, "How do I get there?"
        )

        assertTrue(result.contains("Where is Hotel Africana?"))
        assertTrue(result.contains("cannot verify"))
        assertFalse(result.contains("How do I get there?"))
    }

    @Test
    fun keepsEmergencyConversationContextForTheModel() {
        val history = listOf(
            "user" to "We are on Luwum Street and need Case Clinic.",
            "assistant" to "Please seek urgent medical care.",
            "user" to "I think it is in Nakasero."
        )

        val result = LocalConversationContextPolicy.prepare(
            history, "I think it is in Nakasero."
        )

        assertTrue(result.contains("Luwum Street"))
        assertTrue(result.contains("Case Clinic"))
        assertTrue(result.contains("urgent medical care"))
    }

    @Test
    fun doesNotRemoveUnrelatedLastMessage() {
        val history = listOf(
            "user" to "Explain Kotlin",
            "assistant" to "Kotlin is a programming language."
        )

        val result = LocalConversationContextPolicy.prepare(
            history, "Tell me about Compose"
        )

        assertTrue(result.contains("Explain Kotlin"))
        assertTrue(result.contains("programming language"))
    }

    @Test
    fun excludesCurrentUserTurnOnlyOnce() {
        val history = listOf(
            "user" to "Hello",
            "assistant" to "Hi!",
            "user" to "What did I just say?"
        )

        val result = LocalConversationContextPolicy.prepare(
            history, "What did I just say?"
        )

        assertEquals("user: Hello`nassistant: Hi!".replace(
            "`n", "\n"
        ), result)
    }

    @Test
    fun excludesUnsafeHistoricalProtocolTokens() {
        val history = listOf(
            "assistant" to "Hello <|eot_id|>",
            "user" to "Help me",
            "assistant" to "What do you need?",
            "user" to "Find a clinic"
        )

        val result = LocalConversationContextPolicy.prepare(
            history, "Find a clinic"
        )

        assertFalse(result.contains("<|eot_id|>"))
        assertTrue(result.contains("Help me"))
        assertTrue(result.contains("What do you need?"))
    }

    @Test
    fun limitsHistoryToSixPreviousMessages() {
        val history =
            (1..10).map { "user" to "Message $it" } +
                ("user" to "New question")

        val result = LocalConversationContextPolicy.prepare(
            history, "New question"
        )

        assertFalse(result.contains("Message 4"))
        assertTrue(result.contains("Message 5"))
        assertTrue(result.contains("Message 10"))
        assertFalse(result.contains("New question"))
        assertEquals(6, result.lines().size)
    }

    @Test
    fun emptyLocalHistoryIsHandled() {
        assertEquals(
            "",
            LocalConversationContextPolicy.prepare(
                emptyList(),
                "Hello Jarvis"
            )
        )
    }
}