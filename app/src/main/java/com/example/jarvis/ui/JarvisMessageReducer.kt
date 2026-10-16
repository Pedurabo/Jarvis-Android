package com.example.jarvis.ui

import com.example.jarvis.core.ai.ConfidenceLevel

/**
 * Pure state transitions for assistant conversations.
 *
 * No Android context, Firebase, network, or voice services.
 */
object JarvisMessageReducer {

    const val FAILURE_MESSAGE =
        "Sorry, I couldn't complete that request. Please try again."

    fun started(
        state: JarvisUiState,
        command: String
    ): JarvisUiState {
        return state.copy(
            messages = state.messages + Message(
                text = command,
                isUser = true
            ),
            isProcessing = true,
            processingStatus = "Thinking..."
        )
    }

    fun succeeded(
        state: JarvisUiState,
        response: String,
        confidence: ConfidenceLevel,
        origin: MessageOrigin = MessageOrigin.AI_RESPONSE
    ): JarvisUiState {
        return state.copy(
            messages = state.messages + Message(
                text = response,
                isUser = false,
                confidence = confidence,
                origin = origin
            ),
            isProcessing = false,
            processingStatus = null
        )
    }

    fun failed(state: JarvisUiState): JarvisUiState {
        return state.copy(
            messages = state.messages + Message(
                text = FAILURE_MESSAGE,
                isUser = false
            ),
            isProcessing = false,
            processingStatus = null
        )
    }
}