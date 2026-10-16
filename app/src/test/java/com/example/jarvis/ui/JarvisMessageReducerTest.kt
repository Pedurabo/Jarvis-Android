package com.example.jarvis.ui

import com.example.jarvis.core.ai.ConfidenceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisMessageReducerTest {

    @Test
    fun startingRequestAddsUserMessage() {
        val initial = JarvisUiState()

        val result = JarvisMessageReducer.started(
            initial,
            "Hello Jarvis"
        )

        assertEquals(1, result.messages.size)
        assertEquals("Hello Jarvis", result.messages.last().text)
        assertTrue(result.messages.last().isUser)
        assertTrue(result.isProcessing)
        assertEquals("Thinking...", result.processingStatus)

        // Original state remains unchanged.
        assertTrue(initial.messages.isEmpty())
        assertFalse(initial.isProcessing)
    }

    @Test
    fun successfulRequestAddsAssistantResponse() {
        val started = JarvisMessageReducer.started(
            JarvisUiState(),
            "Hello"
        )

        val result = JarvisMessageReducer.succeeded(
            started,
            "Hello! How can I help?",
            ConfidenceLevel.MEDIUM
        )

        assertEquals(2, result.messages.size)
        assertEquals(
            "Hello! How can I help?",
            result.messages.last().text
        )
        assertFalse(result.messages.last().isUser)
        assertEquals(
            ConfidenceLevel.MEDIUM,
            result.messages.last().confidence
        )
        assertFalse(result.isProcessing)
        assertEquals(null, result.processingStatus)
    }

    @Test
    fun failedRequestShowsFriendlyMessageAndStopsProcessing() {
        val started = JarvisMessageReducer.started(
            JarvisUiState(),
            "Check the weather"
        )

        val result = JarvisMessageReducer.failed(started)

        assertEquals(2, result.messages.size)
        assertEquals(
            JarvisMessageReducer.FAILURE_MESSAGE,
            result.messages.last().text
        )
        assertFalse(result.messages.last().isUser)
        assertFalse(result.isProcessing)
        assertEquals(null, result.processingStatus)
    }
}