package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalPromptAccuracyTest {

    private val builder = JarvisPromptBuilder()

    private fun prompt(
        query: String,
        tools: String = "",
        route: ModelRouter.Route = ModelRouter.Route.LOCAL
    ): String = builder.build(
        query = query,
        chatHistory = "",
        retrievedContext = "",
        deviceContext = "",
        tools = tools,
        stepHistory = emptyList(),
        route = route
    )

    @Test
    fun kotlinCodeRequestIncludesAccuracyGuidance() {
        val result = prompt("Write a Kotlin function to add items to a list")

        assertTrue(result.contains("KOTLIN CODE ACCURACY"))
        assertTrue(result.contains("MutableList"))
        assertTrue(result.contains("listOf"))
        assertTrue(result.contains("mutableListOf"))
    }

    @Test
    fun kotlinDebuggingRequestIncludesAccuracyGuidance() {
        val result = prompt("Fix this Kotlin compilation error")

        assertTrue(result.contains("KOTLIN CODE ACCURACY"))
        assertTrue(result.contains("Do not invent APIs"))
    }

    @Test
    fun ordinaryConversationRemainsFocused() {
        val result = prompt("Hello Jarvis")

        assertFalse(result.contains("KOTLIN CODE ACCURACY"))
        assertFalse(result.contains("PHONE AUTOMATION TOOL CONTRACT"))
    }

    @Test
    fun deviceCommandsDoNotReceiveKotlinGuidance() {
        val result = prompt(
            query = "Open WhatsApp",
            tools = "PhoneAutomation: requires approval"
        )

        assertFalse(result.contains("KOTLIN CODE ACCURACY"))
        assertTrue(result.contains("PhoneAutomation: requires approval"))
    }

    @Test
    fun cloudKotlinPromptReceivesSameGuidance() {
        val result = prompt(
            query = "Write a Kotlin function",
            route = ModelRouter.Route.CLOUD
        )

        assertTrue(result.contains("KOTLIN CODE ACCURACY"))
    }
}