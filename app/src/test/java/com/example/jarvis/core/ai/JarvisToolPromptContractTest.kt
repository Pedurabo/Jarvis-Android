package com.example.jarvis.core.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisToolPromptContractTest {

    private val builder = JarvisPromptBuilder()

    private fun prompt(route: ModelRouter.Route): String =
        builder.build(
            query = "Open Android Settings. Ask for approval first.",
            chatHistory = "",
            retrievedContext = "",
            deviceContext = "",
            tools = "PhoneAutomation",
            stepHistory = emptyList(),
            route = route
        )

    @Test
    fun cloudPromptExplainsDeviceActionRouting() {
        val text = prompt(ModelRouter.Route.CLOUD)

        assertTrue(text.contains("PHONE AUTOMATION TOOL CONTRACT:"))
        assertTrue(text.contains("\"tool\": \"PhoneAutomation\""))
        assertTrue(text.contains("\"action\": \"open_app\""))
        assertTrue(text.contains("\"target\": \"com.android.settings\""))
    }

    @Test
    fun localPromptIncludesTheSameToolContract() {
        val text = prompt(ModelRouter.Route.LOCAL)

        assertTrue(text.contains("PHONE AUTOMATION TOOL CONTRACT:"))
        assertTrue(text.contains("\"tool\": \"PhoneAutomation\""))
        assertTrue(text.contains("<|start_header_id|>assistant"))
    }

    @Test
    fun toolContractRequiresApplicationApproval() {
        val text = prompt(ModelRouter.Route.CLOUD)

        assertTrue(text.contains("application-controlled authorization"))
        assertTrue(text.contains("Never claim the user approved"))
        assertTrue(text.contains("return only the tool JSON"))
    }
}