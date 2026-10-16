package com.example.jarvis.core.ai

/** Deterministic voice fallback: preserve the verified answer without regeneration. */
internal object VoiceOutputPolicy {
    fun format(answer: String): String = answer
}