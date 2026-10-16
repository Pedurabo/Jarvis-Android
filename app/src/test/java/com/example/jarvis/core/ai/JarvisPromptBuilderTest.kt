package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisPromptBuilderTest {

    private val builder = JarvisPromptBuilder()

    private fun build(route: ModelRouter.Route): String {
        return builder.build(
            query = "How is traffic in Kampala?",
            chatHistory = "user: Good morning",
            retrievedContext = "Prefers concise responses",
            deviceContext = """{"city":"Kampala"}""",
            tools = "traffic: Check road conditions",
            stepHistory = listOf("Tool traffic result: Heavy traffic"),
            route = route
        )
    }

    @Test
    fun cloudPromptIncludesAllContext() {
        val prompt = build(ModelRouter.Route.CLOUD)

        assertTrue(prompt.contains("How is traffic in Kampala?"))
        assertTrue(prompt.contains("user: Good morning"))
        assertTrue(prompt.contains("Prefers concise responses"))
        assertTrue(prompt.contains("Kampala"))
        assertTrue(prompt.contains("traffic: Check road conditions"))
        assertTrue(prompt.contains("Heavy traffic"))
    }

    @Test
    fun localPromptUsesExpectedRoleMarkers() {
        val prompt = build(ModelRouter.Route.LOCAL)

        assertTrue(prompt.contains("<|begin_of_text|>"))
        assertTrue(prompt.contains("<|start_header_id|>system"))
        assertTrue(prompt.contains("<|start_header_id|>user"))
        assertTrue(prompt.contains("How is traffic in Kampala?"))
        assertTrue(prompt.contains("traffic: Check road conditions"))
    }

    @Test
    fun promptsDoNotContainUnresolvedVariables() {
        for (route in ModelRouter.Route.values()) {
            val prompt = build(route)

            for (variable in listOf(
                "tools", "deviceContext", "retrievedContext",
                "chatHistory", "steps", "query",
                "systemPrompt", "userContext"
            )) {
                assertFalse(
                    "Unresolved variable: $variable",
                    prompt.contains("$" + variable)
                )
            }
        }
    }
}