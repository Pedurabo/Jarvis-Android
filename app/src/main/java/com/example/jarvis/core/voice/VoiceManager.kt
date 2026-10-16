package com.example.jarvis.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VoiceManager: Refactored for strictly Push-to-Talk (PTT) behavior.
 * Manages one-shot Speech-to-Text and Text-to-Speech without background listening.
 */
@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) : RecognitionListener {

    private val TAG = "VoiceManager"

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        initializeTTS()
    }

    private fun initializeSpeechRecognizer() {
        if (speechRecognizer != null) return

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceManager)
            }
        } else {
            _error.value = "Speech recognition not available on this device"
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupTTSConfig()
            } else {
                _error.value = "TTS Initialization failed"
            }
        }
    }

    private fun setupTTSConfig() {
        tts?.apply {
            language = Locale.US
            setPitch(1.0f)
            setSpeechRate(1.1f)

            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    /**
     * Starts listening for a single utterance.
     * Stops any ongoing speech before starting.
     */
    fun startListening() {
        stopSpeaking()
        initializeSpeechRecognizer()

        if (_isListening.value) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            _transcribedText.value = ""
            _error.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Start listening failed", e)
            _isListening.value = false
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        speechRecognizer?.destroy()
        tts?.shutdown()
    }

    // --- RecognitionListener Overrides ---

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        _isListening.value = false
        // Silence ERROR_NO_MATCH as it's common in PTT
        if (error != SpeechRecognizer.ERROR_NO_MATCH) {
            _error.value = "Speech recognition error: $error"
        }
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.getOrNull(0)?.let {
            _transcribedText.value = it
        }
        _isListening.value = false
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
