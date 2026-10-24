package com.example.jarvis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.core.ai.AssistantController
import com.example.jarvis.core.ai.ConfidenceLevel
import com.example.jarvis.core.ai.ConversationRepository
import com.example.jarvis.core.chat.LocalChatRepository
import android.util.Log
import com.example.jarvis.core.ai.LLMManager
import com.example.jarvis.core.voice.VoiceManager
import com.example.jarvis.skills.SkillRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
    private val localChatRepository: LocalChatRepository,
    private val assistantController: AssistantController
) : ViewModel() {

    private val historyReady = CompletableDeferred<Unit>()

    // CDI.5.54-A: Cancel outstanding responses before deleting history.
    private val activeRequestJobs = mutableSetOf<Job>()
    private var clearingChat = false
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
        // Local-first restoration. Merge to avoid losing messages
        // submitted while history is loading.
        viewModelScope.launch {
            try {
                val saved = localChatRepository.load()

                val restored = if (saved.isNotEmpty()) {
                    saved.map {
                        Message(
                            text = it.text,
                            isUser = it.role == "user",
                            timestamp = it.timestamp,
                            origin = enumValueOrDefault(
                                it.origin,
                                MessageOrigin.AI_RESPONSE
                            ),
                            confidence = enumValueOrDefault(
                                it.confidence,
                                ConfidenceLevel.MEDIUM
                            )
                        )
                    }
                } else if (!localChatRepository.wasCleared()) {
                    val cloudHistory = conversationRepository.getLastMessages(20)
                    cloudHistory.map {
                        Message(
                            text = it.text,
                            isUser = it.role == "user",
                            timestamp = it.timestamp
                        )
                    }
                } else {
                    emptyList()
                }

                if (saved.isEmpty() && !localChatRepository.wasCleared()) {
                    restored.forEach { message ->
                        localChatRepository.save(
                            role = if (message.isUser) "user" else "assistant",
                            text = message.text,
                            timestamp = message.timestamp,
                            origin = message.origin.name,
                            confidence = message.confidence.name
                        )
                    }
                }

                val current = _uiState.value
                val merged = (restored + current.messages)
                    .distinctBy {
                        Triple(it.timestamp, it.isUser, it.text)
                    }
                    .sortedBy { it.timestamp }

                _uiState.value = current.copy(messages = merged)
            } catch (e: Exception) {
                Log.e("JarvisChatHistory", "Unable to restore history", e)
            } finally {
                historyReady.complete(Unit)
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
        if (clearingChat) return
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
        if (clearingChat) return
        _uiState.value = JarvisMessageReducer.started(_uiState.value, command)
        val userMessage = _uiState.value.messages.last()

        val requestJob = viewModelScope.launch {
            try {
                historyReady.await()
                saveLocalMessage(userMessage)
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
                    AssistantController.ResponseOrigin.CURATED_FACT ->
                        MessageOrigin.CURATED_FACT

                    AssistantController.ResponseOrigin.KNOWLEDGE_RESULT ->
                        MessageOrigin.KNOWLEDGE_RESULT

                    AssistantController.ResponseOrigin.KNOWLEDGE_NOTICE ->
                        MessageOrigin.KNOWLEDGE_NOTICE

                    AssistantController.ResponseOrigin.AI_RESPONSE ->
                        MessageOrigin.AI_RESPONSE
                }

                _uiState.value = JarvisMessageReducer.succeeded(
                    state = _uiState.value,
                    response = assistantResponse.text,
                    confidence = assistantResponse.confidence,
                    origin = messageOrigin
                )

                saveLocalMessage(_uiState.value.messages.last())

                // Text-first behavior: do not speak replies automatically.
                // Microphone input remains available through toggleListening().

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = JarvisMessageReducer.failed(_uiState.value)
                saveLocalMessage(_uiState.value.messages.last())
            }
        }

        activeRequestJobs.add(requestJob)
        requestJob.invokeOnCompletion {
            activeRequestJobs.remove(requestJob)
        }
    }

    fun clearChat() {
        if (clearingChat) return

        clearingChat = true
        _uiState.value = _uiState.value.copy(
            isClearingChat = true,
            clearChatError = null
        )

        viewModelScope.launch {
            try {
                historyReady.await()

                // Cancel all active chat requests, not just the last one.
                activeRequestJobs.toList().forEach { job ->
                    job.cancelAndJoin()
                }
                activeRequestJobs.clear()

                voiceManager.stopSpeaking()
                skillRegistry.rejectPendingToolAction()

                localChatRepository.clear()
                assistantController.resetNavigationContext()

                _uiState.value = JarvisUiState()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("JarvisChatHistory", "Unable to clear chat", e)

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processingStatus = null,
                    clearChatError = "Could not clear saved chat. Please retry."
                )
            } finally {
                clearingChat = false
                _uiState.value = _uiState.value.copy(
                    isClearingChat = false
                )
            }
        }
    }

    private suspend fun saveLocalMessage(message: Message) {
        try {
            localChatRepository.save(
                role = if (message.isUser) "user" else "assistant",
                text = message.text,
                timestamp = message.timestamp,
                origin = message.origin.name,
                confidence = message.confidence.name
            )
        } catch (e: Exception) {
            Log.e("JarvisChatHistory", "Unable to save local message", e)
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String,
        fallback: T
    ): T = enumValues<T>().firstOrNull { it.name == value } ?: fallback
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
    DIRECTORY_NOTICE,
    KNOWLEDGE_RESULT,
    KNOWLEDGE_NOTICE,
    CURATED_FACT
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
    val processingStatus: String? = null,
    val isClearingChat: Boolean = false,
    val clearChatError: String? = null
)
