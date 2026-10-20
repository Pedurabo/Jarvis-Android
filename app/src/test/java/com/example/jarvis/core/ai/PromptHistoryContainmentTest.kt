package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptHistoryContainmentTest {

    @Test
    fun keepsNormalShortConversation() {
        val result = PromptHistoryPolicy.format(
            listOf(
                "user" to "Hello Jarvis",
                "assistant" to "Hello!",
                "user" to "What can you do?"
            )
        )

        assertEquals(
            "user: Hello Jarvis\nassistant: Hello!\nuser: What can you do?",
            result
        )
    }

    @Test
    fun removesExcessivelyLongAssistantReplies() {
        val result = PromptHistoryPolicy.format(
            listOf(
                "user" to "Where is City Square?",
                "assistant" to "Technical overview. ".repeat(100),
                "user" to "Where is Clock Tower?"
            )
        )

        assertTrue(result.contains("Where is City Square?"))
        assertTrue(result.contains("Where is Clock Tower?"))
        assertFalse(result.contains("Technical overview"))
    }

    @Test
    fun retainsAssistantReplyAtCharacterLimit() {
        val reply = "a".repeat(1200)

        val result = PromptHistoryPolicy.format(
            listOf("assistant" to reply)
        )

        assertEquals("assistant: $reply", result)
    }

    @Test
    fun excludesAssistantReplyAboveCharacterLimit() {
        val reply = "a".repeat(1201)

        val result = PromptHistoryPolicy.format(
            listOf("assistant" to reply)
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun retainsOnlySixMostRecentMessages() {
        val messages = (1..10).map {
            "user" to "Message $it"
        }

        val result = PromptHistoryPolicy.format(messages)

        assertFalse(result.contains("Message 1\n"))
        assertFalse(result.contains("Message 4\n"))
        assertTrue(result.contains("Message 5"))
        assertTrue(result.contains("Message 10"))
        assertEquals(6, result.lines().size)
    }

    @Test
    fun longUserMessageIsNotSilentlyRewritten() {
        val message = "u".repeat(1300)

        val result = PromptHistoryPolicy.format(
            listOf("user" to message)
        )

        assertEquals("user: $message", result)
    }

    @Test
    fun rejectsProtocolTokensAsBefore() {
        val result = PromptHistoryPolicy.format(
            listOf(
                "assistant" to "Hello <|eot_id|>",
                "user" to "Where is Kampala?"
            )
        )

        assertEquals("user: Where is Kampala?", result)
    }

    @Test
    fun preservesShortProgrammingAnswersForRealQuestions() {
        val result = PromptHistoryPolicy.format(
            listOf(
                "user" to "Explain Kotlin val",
                "assistant" to "Val prevents reassignment."
            )
        )

        assertTrue(result.contains("Val prevents reassignment"))
    }
}
