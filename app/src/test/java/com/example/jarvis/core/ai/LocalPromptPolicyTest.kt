package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPromptPolicyTest {
    @Test fun acceptsNormalPrompt() {
        assertTrue(LocalPromptPolicy.fits("<|begin_of_text|>Hello", 2048))
    }

    @Test fun acceptsExactCharacterBudget() {
        assertTrue(LocalPromptPolicy.fits("a".repeat(7168), 2048))
    }

    @Test fun rejectsOversizedPromptRatherThanTruncating() {
        assertFalse(LocalPromptPolicy.fits("a".repeat(7169), 2048))
    }

    @Test fun rejectsInvalidTokenBudget() {
        assertFalse(LocalPromptPolicy.fits("hello", 0))
        assertFalse(LocalPromptPolicy.fits("hello", -1))
    }
}