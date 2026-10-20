package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelOutputIntegrityPolicyTest {

    @Test
    fun ordinaryExplanationsPass() {
        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "Explain dependency injection",
                "Dependency injection supplies objects with their dependencies."
            )
        )
    }

    @Test
    fun protocolTokensFail() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Hello",
                "<|start_header_id|>assistant<|end_header_id|>Hello"
            )
        )
    }

    @Test
    fun internalInstructionsFail() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Where is the palace?",
                "Developer Instructions: use the internal tool."
            )
        )
    }

    @Test
    fun toolSchemasFail() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Kampala divisions",
                "Tool Schema: PhoneAutomation parameters"
            )
        )
    }

    @Test
    fun internalContextHeadingsFail() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Where is the palace?",
                "Provided Context: None"
            )
        )
    }

    @Test
    fun roleTranscriptFails() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Where is the palace?",
                "system:\nYou are Jarvis."
            )
        )
    }

    @Test
    fun unexpectedHtmlFails() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Where is Nakumatt?",
                "<html><body>Result</body></html>"
            )
        )
    }

    @Test
    fun unexpectedHtmlFragmentsFail() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Where is the palace?",
                "<div>Location</div>"
            )
        )
    }

    @Test
    fun requestedHtmlPasses() {
        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "Write HTML code for a webpage",
                "<html><body><h1>Hello</h1></body></html>"
            )
        )
    }

    @Test
    fun requestedKotlinPasses() {
        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "Write Kotlin code",
                "fun greet(): String = \"Hello\""
            )
        )
    }

    @Test
    fun programmingRequestsStillRejectToolLeaks() {
        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(
                "Explain Kotlin",
                "Available Tools: PhoneAutomation"
            )
        )
    }

    @Test
    fun ordinaryKampalaAnswerPassesStructuralCheck() {
        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "What is Kampala?",
                "Kampala is Uganda's capital city."
            )
        )
    }
}
