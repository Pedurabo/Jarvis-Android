package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelOutputWorkflowLeakRegressionTest {

    private val navigationQuery =
        "need to get to industrial area, street 8"

    @Test
    fun rejectsInternalToolExecutionInstructions() {
        val answer =
            "First, call the location tool to obtain the coordinates. " +
            "Then return the result as a JSON object and provide " +
            "the assistant's final answer."

        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(navigationQuery, answer)
        )
    }

    @Test
    fun rejectsMachineOrientedNavigationInstructions() {
        val answer =
            "Perform a tool call to look up this address. " +
            "Return the location data in JSON format before responding."

        assertFalse(
            ModelOutputIntegrityPolicy.isSafe(navigationQuery, answer)
        )
    }

    @Test
    fun acceptsNormalNavigationClarification() {
        val answer =
            "I can help you get to Industrial Area, Street 8. " +
            "Where will you be starting from?"

        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(navigationQuery, answer)
        )
    }

    @Test
    fun acceptsLegitimateTechnicalDiscussion() {
        val answer =
            "A JSON object contains key-value pairs, and a tool call " +
            "is a structured request made by an application."

        assertTrue(
            ModelOutputIntegrityPolicy.isSafe(
                "Explain JSON and tool calling",
                answer
            )
        )
    }
}