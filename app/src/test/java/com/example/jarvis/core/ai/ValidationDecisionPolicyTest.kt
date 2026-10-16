package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationDecisionPolicyTest {

    @Test
    fun acceptsExactValid() {
        assertEquals(
            ValidationDecisionPolicy.Decision.VALID,
            ValidationDecisionPolicy.parse("VALID")
        )
    }

    @Test
    fun acceptsExactUnsupported() {
        assertEquals(
            ValidationDecisionPolicy.Decision.UNSUPPORTED,
            ValidationDecisionPolicy.parse("UNSUPPORTED")
        )
    }

    @Test
    fun acceptsExactContradictory() {
        assertEquals(
            ValidationDecisionPolicy.Decision.CONTRADICTORY,
            ValidationDecisionPolicy.parse("CONTRADICTORY")
        )
    }

    @Test
    fun normalizesCaseAndOuterWhitespace() {
        assertEquals(
            ValidationDecisionPolicy.Decision.VALID,
            ValidationDecisionPolicy.parse("  valid  ")
        )
    }

    @Test
    fun invalidIsNotMistakenForValid() {
        assertEquals(
            ValidationDecisionPolicy.Decision.INCONCLUSIVE,
            ValidationDecisionPolicy.parse("INVALID")
        )
    }

    @Test
    fun rejectsExplanatoryOutput() {
        assertEquals(
            ValidationDecisionPolicy.Decision.INCONCLUSIVE,
            ValidationDecisionPolicy.parse("The answer is VALID.")
        )
    }

    @Test
    fun rejectsMultipleLabels() {
        assertEquals(
            ValidationDecisionPolicy.Decision.INCONCLUSIVE,
            ValidationDecisionPolicy.parse("VALID UNSUPPORTED")
        )
    }

    @Test
    fun rejectsBlankOutput() {
        assertEquals(
            ValidationDecisionPolicy.Decision.INCONCLUSIVE,
            ValidationDecisionPolicy.parse("   ")
        )
    }
}