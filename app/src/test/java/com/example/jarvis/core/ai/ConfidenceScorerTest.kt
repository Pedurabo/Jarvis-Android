package com.example.jarvis.core.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceScorerTest {

    private val scorer = ConfidenceScorer()

    @Test
    fun normalAnswerDoesNotNeedMemoriesOrTools() = runBlocking {
        val result = scorer.calculateConfidence(
            "What does Kotlin val mean?",
            "In Kotlin, val prevents reassignment after initialization.",
            0,
            0
        )

        assertEquals(ConfidenceLevel.MEDIUM, result.level)
    }

    @Test
    fun clearAdmissionOfUncertaintyIsLow() = runBlocking {
        val result = scorer.calculateConfidence(
            "Is this claim verified?",
            "I don't know whether this claim is correct.",
            0,
            0
        )

        assertEquals(ConfidenceLevel.LOW, result.level)
    }

    @Test
    fun blankAnswerIsLow() = runBlocking {
        val result = scorer.calculateConfidence(
            "Explain Jetpack Compose",
            "",
            0,
            0
        )

        assertEquals(ConfidenceLevel.LOW, result.level)
    }

    @Test
    fun toolsAloneDoNotGrantHighConfidence() = runBlocking {
        val result = scorer.calculateConfidence(
            "Explain Kotlin",
            "Kotlin is a programming language.",
            4,
            3
        )

        assertEquals(ConfidenceLevel.MEDIUM, result.level)
        assertTrue(result.score < 0.7f)
    }
}