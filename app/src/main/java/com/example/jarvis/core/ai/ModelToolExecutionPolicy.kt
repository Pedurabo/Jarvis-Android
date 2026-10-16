package com.example.jarvis.core.ai

/**
 * Final controller-side gate for model-generated tool calls.
 *
 * This only permits a candidate to reach SkillRegistry.
 * Actual execution authorization remains with SkillRegistry.
 */
internal object ModelToolExecutionPolicy {

    fun isAllowed(
        mode: PromptModePolicy.Mode,
        isToolCall: Boolean,
        toolName: String
    ): Boolean {
        return mode == PromptModePolicy.Mode.TOOL_AWARE &&
            isToolCall &&
            toolName.trim().isNotEmpty()
    }
}