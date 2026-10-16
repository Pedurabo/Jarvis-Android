package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceOutputPolicyTest {
    @Test fun preservesOrdinaryAnswer() {
        val answer = "Here is your answer."
        assertEquals(answer, VoiceOutputPolicy.format(answer))
    }

    @Test fun preservesApprovalNotice() {
        val notice = "I've requested your approval in the app. No phone action has been performed."
        assertEquals(notice, VoiceOutputPolicy.format(notice))
    }

    @Test fun preservesLongAnswersWithoutCompression() {
        val answer = "An important fact. ".repeat(200)
        assertEquals(answer, VoiceOutputPolicy.format(answer))
    }

    @Test fun preservesNumbersUrlsAndMultilineContent() {
        val answer = "Dial +12025550100\nSee https://example.org/test?x=2&y=3"
        assertEquals(answer, VoiceOutputPolicy.format(answer))
    }
}