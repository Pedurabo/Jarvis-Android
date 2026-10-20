package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerPrecisionPolicyTest {

    private fun prompt(query: String, tools: String = ""): String {
        return JarvisPromptBuilder().build(
            query = query,
            chatHistory = "",
            retrievedContext = "",
            deviceContext = "{}",
            tools = tools,
            stepHistory = emptyList(),
            route = ModelRouter.Route.LOCAL
        )
    }

    @Test
    fun simpleExplanationRequestsConciseResponse() {
        val guidance = AnswerPrecisionPolicy.guidance(
            "Explain dependency injection"
        )

        assertTrue(guidance.contains("60 to 120 words"))
        assertTrue(guidance.contains("at most three useful points"))
    }

    @Test
    fun explicitlyDetailedRequestIsNotRestricted() {
        val guidance = AnswerPrecisionPolicy.guidance(
            "Explain dependency injection in detail"
        )

        assertTrue(guidance.contains("Provide the requested depth"))
        assertFalse(guidance.contains("60 to 120 words"))
    }

    @Test
    fun stepByStepRequestAllowsDetail() {
        val guidance = AnswerPrecisionPolicy.guidance(
            "Explain dependency injection step by step"
        )

        assertTrue(guidance.contains("requested depth"))
    }

    @Test
    fun codeRequestPreservesImplementationDetail() {
        val guidance = AnswerPrecisionPolicy.guidance(
            "Write Kotlin code for a mutable list"
        )

        assertTrue(guidance.contains("preserve necessary code"))
        assertFalse(guidance.contains("60 to 120 words"))
    }

    @Test
    fun kotlinValVarIncludesReferenceAccuracy() {
        val guidance = AnswerPrecisionPolicy.guidance(
            "Explain Kotlin val versus var"
        )

        assertTrue(guidance.contains("cannot be reassigned"))
        assertTrue(guidance.contains("referenced object immutable"))
        assertTrue(guidance.contains("compile-time constant"))
        assertTrue(guidance.contains("access speeds"))
    }

    @Test
    fun unrelatedExplanationDoesNotReceiveKotlinGuidance() {
        val guidance = AnswerPrecisionPolicy.guidance(
            "Explain dependency injection"
        )

        assertFalse(guidance.contains("KOTLIN REFERENCE PRECISION"))
    }

    @Test
    fun conversationPromptIncludesConciseGuidance() {
        val text = prompt("Explain dependency injection")

        assertTrue(text.contains("ANSWER LENGTH:"))
        assertTrue(text.contains("60 to 120 words"))
        assertFalse(text.contains("PHONE AUTOMATION TOOL CONTRACT"))
    }

    @Test
    fun detailedConversationPromptPreservesRequestedDepth() {
        val text = prompt(
            "Explain dependency injection in detail with code example"
        )

        assertTrue(text.contains("Provide the requested depth"))
        assertFalse(text.contains("60 to 120 words"))
    }

    @Test
    fun kotlinConversationPromptContainsAccuracyInstruction() {
        val text = prompt("Explain Kotlin val versus var")

        assertTrue(text.contains("KOTLIN REFERENCE PRECISION"))
        assertTrue(text.contains("referenced object immutable"))
    }

    @Test
    fun toolAwarePromptRemainsUnchangedByStylePolicy() {
        val text = prompt(
            "Open Android Settings",
            "PhoneAutomation: authorized application controls"
        )

        assertTrue(text.contains("PHONE AUTOMATION TOOL CONTRACT"))
        assertFalse(text.contains("ANSWER LENGTH:"))
        assertFalse(text.contains("KOTLIN REFERENCE PRECISION"))
    }

    @Test
    fun technicalCodeAccuracyGuidanceRemainsPresent() {
        val text = prompt("Write Kotlin code for a mutable list")

        assertTrue(text.contains("KOTLIN CODE ACCURACY"))
        assertTrue(text.contains("ANSWER LENGTH:"))
        assertTrue(text.contains("preserve necessary code"))
    }
}
