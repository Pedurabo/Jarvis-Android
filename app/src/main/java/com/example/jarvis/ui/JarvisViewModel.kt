package com.example.jarvis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.core.ai.AssistantController
import com.example.jarvis.core.ai.ConfidenceLevel
import com.example.jarvis.core.ai.ConversationRepository
import com.example.jarvis.core.ai.LLMManager
import com.example.jarvis.core.voice.VoiceManager
import com.example.jarvis.skills.SkillRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JarvisViewModel @Inject constructor(
    private val llmManager: LLMManager,
    private val voiceManager: VoiceManager,
    private val skillRegistry: SkillRegistry,
    private val conversationRepository: ConversationRepository,
    private val assistantController: AssistantController
) : ViewModel() {

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()
    val pendingToolAction = skillRegistry.pendingToolAction

    fun approvePendingToolAction() {
        viewModelScope.launch {
            val result = skillRegistry.approvePendingToolAction()
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + Message(text = result, isUser = false)
            )
        }
    }

    fun rejectPendingToolAction() {
        skillRegistry.rejectPendingToolAction()
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + Message(text = "Action cancelled.", isUser = false)
        )
    }

    val isListening = voiceManager.isListening
    val isSpeaking = voiceManager.isSpeaking
    val error = voiceManager.error
    val isModelReady = llmManager.isModelReady
    val llmError = llmManager.error

    init {
        // Load recent chat history from Firestore
        viewModelScope.launch {
            val history = conversationRepository.getLastMessages(20)
            if (history.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    messages = history.map {
                        Message(text = it.text, isUser = it.role == "user", timestamp = it.timestamp)
                    }
                )
            }
        }

        viewModelScope.launch {
            voiceManager.transcribedText.collect { text ->
                if (text.isNotEmpty() && !voiceManager.isListening.value) {
                    processCommand(text)
                }
            }
        }
    }

    fun toggleListening() {
        if (voiceManager.isSpeaking.value) {
            voiceManager.stopSpeaking()
            voiceManager.startListening()
            return
        }

        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    fun onTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(currentInputText = text)
    }

    fun sendTextMessage() {
        val text = _uiState.value.currentInputText
        if (text.isNotBlank()) {
            if (voiceManager.isSpeaking.value) {
                voiceManager.stopSpeaking()
            }
            _uiState.value = _uiState.value.copy(currentInputText = "")
            processCommand(text)
        }
    }

    private fun processCommand(command: String) {
        _uiState.value = JarvisMessageReducer.started(_uiState.value, command)

        viewModelScope.launch {
            try {
                val assistantResponse = assistantController.onUserQuery(command) { status ->
                    _uiState.value = _uiState.value.copy(processingStatus = status)
                }

                if (ApprovalMessagePolicy.isPendingNotice(assistantResponse.text)) {
                    // Pending action is represented by the confirmation dialog.
                    // Do not add a stale reply after approval or rejection.
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        processingStatus = null
                    )
                    return@launch
                }

                val messageOrigin = when (assistantResponse.origin) {
                    AssistantController.ResponseOrigin.DIRECTORY_RESULT ->
                        MessageOrigin.DIRECTORY_RESULT

                    AssistantController.ResponseOrigin.DIRECTORY_NOTICE ->
                        MessageOrigin.DIRECTORY_NOTICE

                    AssistantController.ResponseOrigin.AI_RESPONSE ->
                        MessageOrigin.AI_RESPONSE
                }

                _uiState.value = JarvisMessageReducer.succeeded(
                    state = _uiState.value,
                    response = assistantResponse.text,
                    confidence = assistantResponse.confidence,
                    origin = messageOrigin
                )

                // Use voice-optimized text for TTS
                voiceManager.speak(assistantResponse.voiceOptimizedText)

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = JarvisMessageReducer.failed(_uiState.value)
            }
        }
    }

    fun submitFeedback(query: String, failedResponse: String, correction: String) {
        viewModelScope.launch {
            assistantController.provideFeedback(query, failedResponse, correction)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}

enum class MessageOrigin {
    AI_RESPONSE,
    DIRECTORY_RESULT,
    DIRECTORY_NOTICE
}

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: ConfidenceLevel = ConfidenceLevel.MEDIUM,
    val origin: MessageOrigin = MessageOrigin.AI_RESPONSE
)

data class JarvisUiState(
    val messages: List<Message> = emptyList(),
    val currentInputText: String = "",
    val isProcessing: Boolean = false,
    val processingStatus: String? = null
)
