package com.example.jarvis.ui

import com.example.jarvis.core.ai.ConfidenceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JarvisMessageOriginTest {

    @Test
    fun normalResponseKeepsDefaultOrigin() {
        val result = JarvisMessageReducer.succeeded(
            JarvisUiState(),
            "Hello",
            ConfidenceLevel.MEDIUM
        )

        assertEquals(
            MessageOrigin.AI_RESPONSE,
            result.messages.last().origin
        )
    }

    @Test
    fun directoryResultOriginIsPreserved() {
        val result = JarvisMessageReducer.succeeded(
            JarvisUiState(),
            "Acacia Mall",
            ConfidenceLevel.MEDIUM,
            MessageOrigin.DIRECTORY_RESULT
        )

        assertEquals(
            MessageOrigin.DIRECTORY_RESULT,
            result.messages.last().origin
        )

        assertEquals(
            ConfidenceLevel.MEDIUM,
            result.messages.last().confidence
        )

        assertFalse(result.isProcessing)
    }

    @Test
    fun directoryNoticeOriginIsPreserved() {
        val result = JarvisMessageReducer.succeeded(
            JarvisUiState(),
            "Service unavailable",
            ConfidenceLevel.LOW,
            MessageOrigin.DIRECTORY_NOTICE
        )

        assertEquals(
            MessageOrigin.DIRECTORY_NOTICE,
            result.messages.last().origin
        )
    }

    @Test
    fun failureKeepsDefaultOrigin() {
        val result = JarvisMessageReducer.failed(
            JarvisUiState()
        )

        assertEquals(
            MessageOrigin.AI_RESPONSE,
            result.messages.last().origin
        )
    }
}