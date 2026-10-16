package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfidencePolicyTest {

    @Test
    fun ordinaryUsableAnswerCanBeMedium() {
        assertEquals(
            ConfidenceLevel.MEDIUM,
            ConfidencePolicy.classify(
                usable = true,
                explicitlyUncertain = false
            )
        )
    }

    @Test
    fun unusableAnswerIsLow() {
        assertEquals(
            ConfidenceLevel.LOW,
            ConfidencePolicy.classify(
                usable = false,
                explicitlyUncertain = false
            )
        )
    }

    @Test
    fun explicitlyUncertainAnswerIsLow() {
        assertEquals(
            ConfidenceLevel.LOW,
            ConfidencePolicy.classify(
                usable = true,
                explicitlyUncertain = true
            )
        )
    }

    @Test
    fun unusableUncertainAnswerRemainsLow() {
        assertEquals(
            ConfidenceLevel.LOW,
            ConfidencePolicy.classify(
                usable = false,
                explicitlyUncertain = true
            )
        )
    }

    @Test
    fun policyNeverClaimsHighConfidenceWithoutVerification() {
        for (usable in listOf(false, true)) {
            for (uncertain in listOf(false, true)) {
                val level = ConfidencePolicy.classify(
                    usable = usable,
                    explicitlyUncertain = uncertain
                )

                assertEquals(
                    false,
                    level == ConfidenceLevel.HIGH
                )
            }
        }
    }
}