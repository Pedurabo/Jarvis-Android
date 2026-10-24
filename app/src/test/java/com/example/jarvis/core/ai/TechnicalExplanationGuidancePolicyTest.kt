package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalExplanationGuidancePolicyTest {

    @Test
    fun kotlinExplanationGetsGuidance() {
        val result = TechnicalExplanationGuidancePolicy.guidance(
            "Explain dependency injection in Kotlin",
            false
        )

        assertTrue(result.contains("Distinguish what is essential"))
        assertTrue(result.contains("manual implementation"))
    }

    @Test
    fun generalTechnicalExplanationGetsGuidance() {
        val result = TechnicalExplanationGuidancePolicy.guidance(
            "What is a database transaction?",
            false
        )

        assertFalse(result.isBlank())
    }

    @Test
    fun androidTechnicalExplanationGetsGuidance() {
        val result = TechnicalExplanationGuidancePolicy.guidance(
            "Describe Android architecture",
            false
        )

        assertFalse(result.isBlank())
    }

    @Test
    fun socialGreetingIsUnchanged() {
        val result = TechnicalExplanationGuidancePolicy.guidance(
            "Hello Jarvis",
            false
        )

        assertTrue(result.isBlank())
    }

    @Test
    fun deviceCommandIsUnchanged() {
        val result = TechnicalExplanationGuidancePolicy.guidance(
            "Open Android Settings",
            true
        )

        assertTrue(result.isBlank())
    }

    @Test
    fun toolAwarePromptsAreUnchanged() {
        val result = TechnicalExplanationGuidancePolicy.guidance(
            "Explain dependency injection in Kotlin",
            true
        )

        assertTrue(result.isBlank())
    }

    @Test
    fun unrelatedKampalaDiscoveryIsUnchanged() {
        val result = TechnicalExplanationGuidancePolicy.guidance(
            "Find restaurants in Kololo",
            false
        )

        assertTrue(result.isBlank())
    }
}