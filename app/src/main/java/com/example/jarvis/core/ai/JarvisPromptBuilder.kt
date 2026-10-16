package com.example.jarvis.core.ai

/**
 * Constructs local and cloud prompts.
 *
 * Kept independent of Android APIs for JVM unit testing.
 */
class JarvisPromptBuilder {

    fun build(
        query: String,
        chatHistory: String,
        retrievedContext: String,
        deviceContext: String,
        tools: String,
        stepHistory: List<String>,
        route: ModelRouter.Route
    ): String {
        val steps = if (stepHistory.isEmpty()) "None" else stepHistory.joinToString("\n")

        val systemPrompt = """
            You are Jarvis, a highly intelligent and action-oriented AI assistant.

            MISSION:
            Help the user by either:
            1. Answering directly (if no external data is needed)
            2. Calling a tool (if external data or a device action is required)

            IMPORTANT RULES:

            1. TOOL CALLING & TRAFFIC:
            - If the user asks about "traffic", "jams", "travel time", or "road conditions":
              → YOU MUST CALL the "traffic" tool.
              → Extract the "location" or "destination" from user input.
              → If no location is mentioned, default "location" to "Kampala".
              → RETURN ONLY a JSON object.

            - Example:
              User: "How's the traffic?"
              Assistant: { "tool": "traffic", "params": { "location": "Kampala" } }

            PHONE AUTOMATION TOOL CONTRACT:
            - A request to open an installed Android app is a device action, not a general question.
            - If the user requests opening Android Settings, propose this tool call:
              { "tool": "PhoneAutomation", "params": { "action": "open_app", "target": "com.android.settings" } }
            - For other app launches, use an exact known Android package identifier.
            - If the package identifier is unknown, ask for clarification instead of inventing one.
            - For device actions, return only the tool JSON. Do not claim the action already happened.
            - All phone actions require application-controlled authorization.
            - Never claim the user approved an action. The application obtains approval separately.
            - Do not execute device actions by replying with instructions, code, or simulated tool results.

            2. JSON FORMAT:
            - When calling a tool, return ONLY the JSON object. Do not add any conversational text before or after the JSON.
            {
              "tool": "<tool_name>",
              "params": {
                "key": "value"
              }
            }

            3. AFTER TOOL RESULT:
            If you receive tool results in the "CURRENT PROGRESS" section, convert them into a natural, concise response.
            Example:
            Tool Result: "Traffic in Kampala is heavy near the city center."
            Final Answer: "Traffic is heavy today, especially near the city center."

            4. NORMAL RESPONSES:
            - If no tool is needed → respond normally.
            - Keep answers short (1–3 sentences).
            - Be clear and voice-friendly.

            5. PERSONALITY:
            - Smart, calm, helpful (Jarvis style).
            - No unnecessary explanations.

            AVAILABLE TOOLS:
            $tools

            DEVICE CONTEXT:
            $deviceContext

            RELEVANT MEMORIES:
            $retrievedContext
        """.trimIndent()

        // Tool-free prompts should not carry device automation contracts.
        // Tool-aware prompts preserve the existing instructions unchanged.
        val focusedSystemPrompt = if (tools.isBlank()) {
            """
            You are Jarvis, a helpful, accurate AI assistant.
            Answer the user's question directly.
            Follow the user's requested format and length.
            If uncertain, say so rather than inventing information.
            Do not claim to have performed device actions.
            Keep responses clear and relevant.
            """.trimIndent()
        } else {
            systemPrompt
        }
        // Add guidance only for explicit Kotlin code tasks.
        // Preserve the existing tool-aware and tool-free contracts.
        val kotlinCodeTask =
            TechnicalTaskPolicy.classify(query) == TechnicalTaskPolicy.Kind.CODE_TASK &&
                Regex("""\bkotlin\b""", RegexOption.IGNORE_CASE)
                    .containsMatchIn(query)

        val technicalGuidance = if (kotlinCodeTask) {
            """
            KOTLIN CODE ACCURACY:
            Use valid, idiomatic Kotlin syntax and real standard-library APIs.
            Distinguish read-only List from MutableList.
            listOf creates a read-only List; do not call add on it.
            Use mutableListOf when mutation with add is required.
            Make examples internally consistent and include relevant imports.
            Do not invent APIs or methods.
            If fixing code, explain the actual cause and give a corrected example.
            Do not claim code was compiled or tested unless it actually was.
            """.trimIndent()
        } else {
            ""
        }

        val effectiveSystemPrompt =
            if (technicalGuidance.isBlank()) {
                focusedSystemPrompt
            } else {
                "$focusedSystemPrompt\n\n$technicalGuidance"
            }
        val userContext = buildString {
            if (chatHistory.isNotBlank()) {
                append("CONTEXT:\n")
                append(chatHistory.trim())
                append("\n\n")
            }

            if (stepHistory.isNotEmpty()) {
                append("CURRENT PROGRESS (Tool Results):\n")
                append(steps)
                append("\n\n")
            }

            append(query)
        }

        return if (route == ModelRouter.Route.LOCAL) {
            "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$effectiveSystemPrompt<|eot_id|>" +
            "<|start_header_id|>user<|end_header_id|>\n\n$userContext<|eot_id|>" +
            "<|start_header_id|>assistant<|end_header_id|>\n\n"
        } else {
            "$effectiveSystemPrompt\n\n$userContext"
        }
    }
}