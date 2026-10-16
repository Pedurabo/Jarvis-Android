package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptIsolationTest {

    private val builder = JarvisPromptBuilder()

    private fun build(
        query: String = "Hello Jarvis, introduce yourself in two sentences.",
        history: String = "",
        steps: List<String> = emptyList(),
        route: ModelRouter.Route = ModelRouter.Route.LOCAL
    ): String = builder.build(
        query = query,
        chatHistory = history,
        retrievedContext = "",
        deviceContext = "",
        tools = "",
        stepHistory = steps,
        route = route
    )

    @Test
    fun ordinaryQuestionIsPreserved() {
        val prompt = build()

        assertTrue(
            prompt.contains(
                "Hello Jarvis, introduce yourself in two sentences."
            )
        )
    }

    @Test
    fun emptyToolProgressIsOmitted() {
        val prompt = build()

        assertFalse(
            prompt.contains("CURRENT PROGRESS (Tool Results)")
        )
    }

    @Test
    fun emptyConversationHistoryIsOmitted() {
        val prompt = build()

        assertFalse(
            Regex("""(?m)^CONTEXT:\s*$""").containsMatchIn(prompt)
        )
    }

    @Test
    fun userMessageDoesNotSimulateAnotherConversation() {
        val prompt = build()

        assertFalse(prompt.contains("User: Hello Jarvis"))
        assertFalse(prompt.contains("Assistant:\n"))
    }

    @Test
    fun localChatRoleMarkersRemainIntact() {
        val prompt = build()

        assertTrue(prompt.startsWith("<|begin_of_text|>"))

        assertEquals(
            1,
            Regex(
                Regex.escape("<|start_header_id|>system")
            ).findAll(prompt).count()
        )

        assertEquals(
            1,
            Regex(
                Regex.escape("<|start_header_id|>user")
            ).findAll(prompt).count()
        )

        assertTrue(
            prompt.endsWith(
                "<|start_header_id|>assistant<|end_header_id|>\n\n"
            )
        )
    }

    @Test
    fun realToolProgressIsPreserved() {
        val prompt = build(
            query = "How is traffic in Kampala?",
            steps = listOf("Tool traffic result: Heavy traffic")
        )

        assertTrue(prompt.contains("Heavy traffic"))
        assertTrue(prompt.contains("CURRENT PROGRESS"))
    }

    @Test
    fun existingConversationHistoryIsPreserved() {
        val prompt = build(
            history = "user: Good morning"
        )

        assertTrue(prompt.contains("user: Good morning"))
    }

    @Test
    fun cloudPromptAlsoOmitsEmptyProgress() {
        val prompt = build(route = ModelRouter.Route.CLOUD)

        assertFalse(
            prompt.contains("CURRENT PROGRESS (Tool Results)")
        )

        assertTrue(
            prompt.contains(
                "Hello Jarvis, introduce yourself in two sentences."
            )
        )
    }
}