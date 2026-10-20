package com.example.jarvis.core.ai

/**
 * Selects prompt content, not tool authorization.
 *
 * Unknown and potentially action-oriented commands remain tool-aware.
 * SkillRegistry remains responsible for every tool execution.
 */
internal object PromptModePolicy {

    enum class Mode {
        CONVERSATION,
        TOOL_AWARE
    }

    // Social greetings require no device automation or tool contracts.
    // Commands such as "send", "open", and "call" still use existing routing.
    private val socialGreeting = Regex(
        """^(?:(?:hi|hello|hey|good\s+morning|good\s+afternoon|good\s+evening)(?:\s+jarvis)?[!?.]*|(?:how\s+are\s+you|how\s+are\s+you\s+doing|how's\s+it\s+going|how\s+is\s+it\s+going)(?:\s+today)?[!?.]*)$""",
        RegexOption.IGNORE_CASE
    )
    private val conversationalStart = Regex(
        """^(?:please\s+)?(?:explain|describe|define|introduce yourself|what is|what are|what's|how does|how do|tell me about)\b""",
        RegexOption.IGNORE_CASE
    )

    private val toolRelated = Regex(
        """\b(?:traffic|jams|road conditions|travel time|weather|forecast|temperature|open|launch|dial|call|send|text|sms|message|navigate|turn on|turn off|toggle|brightness|flashlight|location|current location)\b""",
        RegexOption.IGNORE_CASE
    )

    fun select(query: String): Mode {
        val normalized = query.trim()

        if (normalized.isEmpty()) return Mode.TOOL_AWARE

        if (socialGreeting.matches(normalized)) {
            return Mode.CONVERSATION
        }

        // Technical learning and code-writing requests are not
        // device commands merely because they contain action words.
        val educationalRequest = Regex(
            """^(?:please\s+)?(?:explain|describe|define|write|show me how|give me an example(?: of)?|how do i|how does|how do|what is|what are|what's|create|generate|implement|debug|fix|review)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(normalized)

        val programmingContext = Regex(
            """\b(?:kotlin|java|python|javascript|typescript|programming|code|function|functions|suspend|http|api|variable|variables|data class|android studio)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(normalized)

        val technicalRequest = educationalRequest && programmingContext

        if (technicalRequest) {
            return Mode.CONVERSATION
        }

        if (toolRelated.containsMatchIn(normalized)) {
            return Mode.TOOL_AWARE
        }

        return if (conversationalStart.containsMatchIn(normalized)) {
            Mode.CONVERSATION
        } else {
            Mode.TOOL_AWARE
        }
    }
}