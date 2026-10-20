package com.example.jarvis.core.ai

/**
 * Narrow relevance safeguard for normal conversation.
 *
 * Does not authorize or execute tools, verify factual claims,
 * or modify verified directory and knowledge answers.
 *
 * Caller must restrict usage to conversation mode without tool execution.
 */
internal object ConversationalAnswerQualityPolicy {

    private val greetings = Regex(
        """^(?:hi|hello|hey)(?:\s+jarvis)?[!?.]*$""",
        RegexOption.IGNORE_CASE
    )

    private val socialQuestions = Regex(
        """^(?:how\s+are\s+you|how\s+are\s+you\s+doing|how's\s+it\s+going|how\s+is\s+it\s+going)(?:\s+today)?[!?.]*$""",
        RegexOption.IGNORE_CASE
    )

    private val timeGreetings = Regex(
        """^good\s+(?:morning|afternoon|evening)(?:\s+jarvis)?[!?.]*$""",
        RegexOption.IGNORE_CASE
    )

    private val internalNarration = listOf(
        Regex(
            """\b(?:assistant|model)\s+(?:takes\s+a\s+moment\s+to|will\s+now|proceeds\s+to)\s+(?:analy[sz]e|check|select|decide|invoke|call|process)\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\b(?:suggests|selects|chooses)\s+the\s+appropriate\s+tool\s+based\s+on\b""",
            RegexOption.IGNORE_CASE
        )
    )

    fun review(query: String, answer: String): String {
        val request = query.trim().replace(Regex("""\s+"""), " ")

        if (greetings.matches(request) || timeGreetings.matches(request)) {
            return "Hello! How can I help you today?"
        }

        if (socialQuestions.matches(request)) {
            return "I'm here and ready to help. How can I assist you today?"
        }

        val raw = answer.trim()

        if (internalNarration.any { it.containsMatchIn(raw) }) {
            return "I couldn't produce a relevant answer. Could you try asking again?"
        }

        return answer
    }
}
