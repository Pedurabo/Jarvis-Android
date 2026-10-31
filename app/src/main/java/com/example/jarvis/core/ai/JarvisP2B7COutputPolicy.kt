package com.example.jarvis.core.ai

/**
 * Narrow final-stage safety guard for model-authored chat.
 *
 * Never execute, render, or silently reinterpret raw tool syntax.
 * This policy does not grant any tool permissions.
 */
internal object JarvisP2B7COutputPolicy {

    const val SAFE_RESPONSE =
        "I couldn't safely display that response. " +
            "Please try rephrasing your request."

    private val rawToolTag = Regex(
        """(?i)<\s*/?\s*tool_call\b"""
    )

    private val rawToolInvocation = Regex(
        """(?i)\bcall\s*:\s*[a-z_][a-z_0-9]*\s*(?:\{|\()"""
    )

    private val rawToolFence = Regex(
        """(?i)\[/?tool_call\]|\[/?tool_calls\]"""
    )

    private val emailStyleOpening = Regex(
        """(?i)^\s*i hope this email finds you well\b"""
    )

    private val greetingQuery = Regex(
        """(?i)^\s*(?:hello|hi|hey)(?:\s+jarvis)?\s*[,!.]?\s*(?:how are you\s*[?.!]?)?\s*$"""
    )

    fun containsRawToolSyntax(text: String): Boolean =
        rawToolTag.containsMatchIn(text) ||
            rawToolInvocation.containsMatchIn(text) ||
            rawToolFence.containsMatchIn(text)

    /**
     * Reject a generated transcript dump, not ordinary discussion
     * of context, prompts, JSON, or programming.
     */
    internal fun containsLeakedConversationContext(
        query: String,
        answer: String
    ): Boolean {
        val explicitlyTechnical = Regex(
            """(?i)\b(?:context|transcript|prompt|debug|code review|json)\b"""
        ).containsMatchIn(query)

        if (explicitlyTechnical) return false

        val contextHeading = Regex(
            """(?im)^\s*CONTEXT\s*:\s*$"""
        )

        val userLine = Regex(
            """(?im)^\s*user\s*:\s*\S"""
        )

        val assistantLine = Regex(
            """(?im)^\s*assistant\s*:\s*\S"""
        )

        return contextHeading.containsMatchIn(answer) &&
            userLine.containsMatchIn(answer) &&
            assistantLine.containsMatchIn(answer)
    }
    fun review(query: String, answer: String): String {
        if (
            containsRawToolSyntax(answer) ||
            containsLeakedConversationContext(query, answer)
        ) {
            return SAFE_RESPONSE
        }

        if (
            greetingQuery.matches(query) &&
            emailStyleOpening.containsMatchIn(answer)
        ) {
            return "Hello! I'm Jarvis. How can I help you today?"
        }

        return answer
    }
}