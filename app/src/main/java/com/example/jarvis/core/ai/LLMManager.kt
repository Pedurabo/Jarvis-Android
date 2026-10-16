package com.example.jarvis.core.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseMemoryRepository: FirebaseMemoryRepository
) {
    private val TAG = "LLMManager"
    private var llmInference: LlmInference? = null

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val modelFileName = "llama3_2_1b.bin"
    private val modelAssetPath = "models/$modelFileName"
    private val modelInternalPath = "${context.filesDir}/$modelFileName"

    private val MAX_TOKENS = 2048

    init {
        initializeAsync()
    }

    private fun initializeAsync() {
        kotlin.concurrent.thread {
            try {
                val llmExists = ensureModelExists()
                if (llmExists) {
                    setupLLM()
                } else {
                    _error.value = "Model file missing in assets/models/"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
                _error.value = "Initialization error: ${e.message}"
            }
        }
    }

    private fun ensureModelExists(): Boolean {
        val file = File(modelInternalPath)
        if (file.exists() && file.length() > 100_000_000) return true

        return try {
            context.assets.open(modelAssetPath).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun setupLLM() {
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelInternalPath)
                .setMaxTokens(MAX_TOKENS)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            _isModelReady.value = true
            _error.value = null
            Log.d(TAG, "LLM initialized successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "GPU/Native initialization failed.", e)
            _error.value = "Device Incompatible: ${e.message}"
        }
    }

    /**
     * Executes raw inference. Prompt formatting should be handled by the caller.
     */
    suspend fun generateResponse(fullPrompt: String): String = withContext(Dispatchers.IO) {
        if (!_isModelReady.value) return@withContext "Jarvis is offline. ${_error.value ?: "Brain initializing..."}"

        // Never truncate from the beginning: doing so can sever chat-role delimiters
        // and discard instructions. Reject oversized inputs before inference instead.
        if (!LocalPromptPolicy.fits(fullPrompt, MAX_TOKENS)) {
            Log.w(TAG, "Local prompt exceeded conservative character budget")
            return@withContext "This request has too much context for the local model. Please try a shorter request."
        }

        return@withContext try {
            llmInference?.generateResponse(fullPrompt)?.trim() ?: "Inference failed"
        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
            "I encountered a technical limit. Could you try a shorter version?"
        }
    }

    /**
     * Helper to get a smart response with standard system prompt and memory context.
     * Used by smaller engines (Proactive, Personalization).
     */
    suspend fun getSmartResponse(prompt: String): String {
        val memories = firebaseMemoryRepository.getRelevantMemories(3)
        var memoryContext = memories.joinToString("; ") { it.text }
        if (memoryContext.length > 500) memoryContext = memoryContext.takeLast(500)

        val formattedPrompt = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n" +
                "You are Jarvis, a highly accurate AI assistant. Context: $memoryContext<|eot_id|>" +
                "<|start_header_id|>user<|end_header_id|>\n\n$prompt<|eot_id|>" +
                "<|start_header_id|>assistant<|end_header_id|>\n\n"

        return generateResponse(formattedPrompt)
    }
}
