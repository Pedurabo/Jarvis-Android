package com.example.jarvis.core.ai.agents

import com.example.jarvis.core.ai.ModelRouter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReasoningAgent @Inject constructor(
    private val modelRouter: ModelRouter
) : Agent {
    override val name: String = "ReasoningAgent"

    override suspend fun execute(input: String, context: String): AgentResult {
        val systemPrompt = """
            You are the Reasoning Agent. Your task is to provide deep analysis and logical steps for the query.
            Use the provided context to inform your reasoning.

            Context: $context
        """.trimIndent()

        val fullPrompt = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>" +
                "<|start_header_id|>user<|end_header_id|>\n\n$input<|eot_id|>" +
                "<|start_header_id|>assistant<|end_header_id|>\n\n"

        // Default to Cloud for ReasoningAgent if complexity warrants, otherwise use Router's default
        val route = modelRouter.route(input)
        val response = modelRouter.generateResponse(fullPrompt, route)

        return AgentResult(response)
    }
}
