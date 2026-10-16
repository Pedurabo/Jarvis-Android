package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelOutputGuardTest {

    @Test
    fun acceptsOrdinaryAnswers() {
        assertTrue(ModelOutputGuard.isUsable("The answer is 42."))
    }

    @Test
    fun acceptsNormalToolJson() {
        assertTrue(
            ModelOutputGuard.isUsable(
                """{"tool":"FINAL_ANSWER","params":{"content":"Hello"}}"""
            )
        )
    }

    @Test
    fun rejectsEmptyAndWhitespaceOutput() {
        assertFalse(ModelOutputGuard.isUsable(""))
        assertFalse(ModelOutputGuard.isUsable("   "))
    }

    @Test
    fun rejectsHeaderTokens() {
        assertFalse(
            ModelOutputGuard.isUsable("<|start_header_id|>assistant")
        )
        assertFalse(
            ModelOutputGuard.isUsable("<|end_header_id|>")
        )
    }

    @Test
    fun rejectsEndOfTurnTokens() {
        assertFalse(
            ModelOutputGuard.isUsable("Hello <|eot_id|>")
        )
    }

    @Test
    fun rejectsTokensEmbeddedInOtherwiseValidText() {
        assertFalse(
            ModelOutputGuard.isUsable(
                "Hello. <|start_header_id|>assistant"
            )
        )
    }
}