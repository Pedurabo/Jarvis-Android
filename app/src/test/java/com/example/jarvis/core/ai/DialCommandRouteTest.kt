package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialCommandRouteTest {

    @Test
    fun recognizesInternationalNumber() {
        assertEquals(
            "+12025550100",
            DialCommandRoute.resolve("dial +12025550100")
        )
    }

    @Test
    fun recognizesPoliteCommand() {
        assertEquals(
            "+256700000000",
            DialCommandRoute.resolve("Please dial +256700000000.")
        )
    }

    @Test
    fun recognizesCaseInsensitiveCommand() {
        assertEquals(
            "+256700000000",
            DialCommandRoute.resolve("DIAL +256700000000")
        )
    }

    @Test
    fun rejectsNumbersThatAreTooShortOrLong() {
        assertNull(DialCommandRoute.resolve("Dial 12"))
        assertNull(
            DialCommandRoute.resolve("Dial 1234567890123456")
        )
    }

    @Test
    fun rejectsNonNumericTargets() {
        assertNull(DialCommandRoute.resolve("Dial John"))
        assertNull(DialCommandRoute.resolve("Dial +123abc"))
        assertNull(DialCommandRoute.resolve("Dial *123#"))
    }

    @Test
    fun rejectsNegativeAndCompoundInstructions() {
        assertNull(
            DialCommandRoute.resolve("Don't dial +12025550100")
        )
        assertNull(
            DialCommandRoute.resolve("Dial +12025550100 and send SMS")
        )
        assertNull(
            DialCommandRoute.resolve("Dial +12025550100 then call again")
        )
    }

    @Test
    fun rejectsQuestions() {
        assertNull(
            DialCommandRoute.resolve("Can you dial +12025550100?")
        )
        assertNull(
            DialCommandRoute.resolve("How do I dial +12025550100?")
        )
    }

    @Test
    fun doesNotCaptureOtherCommands() {
        assertNull(DialCommandRoute.resolve("Open WhatsApp"))
        assertNull(DialCommandRoute.resolve("Call my friend"))
        assertNull(DialCommandRoute.resolve("What is the traffic today?"))
    }
}