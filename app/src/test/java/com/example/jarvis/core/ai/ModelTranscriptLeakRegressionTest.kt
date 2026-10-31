package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTranscriptLeakRegressionTest {

    @Test
    fun rejectsEndOfTranscript() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Help me get to Case Clinic",
                "I need directions. [End of transcript]"
            )
        )
    }

    @Test
    fun rejectsConversationTerminator() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Where is Kololo?",
                "Kololo is in Kampala. [End of conversation]"
            )
        )
    }

    @Test
    fun rejectsAssistantInstructions() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Where is Case Clinic?",
                "Instructions for the assistant:\n" +
                    "Find a route to the nearest clinic."
            )
        )
    }

    @Test
    fun permitsOrdinaryEmergencyGuidance() {
        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "My neighbour was bitten by a dog",
                "Wash the wound with soap and running water " +
                    "and seek urgent medical attention."
            )
        )
    }

    @Test
    fun permitsTechnicalExplanationOfTranscriptSyntax() {
        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "Explain transcript markers in code",
                "Some datasets contain [End of transcript] " +
                    "to indicate the end of a sample."
            )
        )
    }

    @Test
    fun permitsLegitimateInstructionsForUsers() {
        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "Give me first aid instructions",
                "Instructions:\n" +
                    "Wash the wound and seek medical assistance."
            )
        )
    }
}