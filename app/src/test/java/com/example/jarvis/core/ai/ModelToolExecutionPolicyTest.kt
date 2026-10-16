package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelToolExecutionPolicyTest {

    @Test
    fun conversationModeCannotInvokeTools() {
        assertFalse(
            ModelToolExecutionPolicy.isAllowed(
                PromptModePolicy.Mode.CONVERSATION,
                true,
                "PhoneAutomation"
            )
        )
    }

    @Test
    fun toolAwareModeAllowsValidCandidateToReachRegistry() {
        assertTrue(
            ModelToolExecutionPolicy.isAllowed(
                PromptModePolicy.Mode.TOOL_AWARE,
                true,
                "PhoneAutomation"
            )
        )
    }

    @Test
    fun normalAnswerCannotInvokeTools() {
        assertFalse(
            ModelToolExecutionPolicy.isAllowed(
                PromptModePolicy.Mode.TOOL_AWARE,
                false,
                "PhoneAutomation"
            )
        )
    }

    @Test
    fun emptyToolNameIsBlocked() {
        assertFalse(
            ModelToolExecutionPolicy.isAllowed(
                PromptModePolicy.Mode.TOOL_AWARE,
                true,
                ""
            )
        )
    }

    @Test
    fun whitespaceOnlyToolNameIsBlocked() {
        assertFalse(
            ModelToolExecutionPolicy.isAllowed(
                PromptModePolicy.Mode.TOOL_AWARE,
                true,
                "   "
            )
        )
    }
}