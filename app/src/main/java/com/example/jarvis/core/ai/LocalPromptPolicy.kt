package com.example.jarvis.core.ai

/** Conservative character budget, not a tokenizer or precise context-window measure. */
internal object LocalPromptPolicy {
    fun fits(prompt: String, maxTokens: Int): Boolean {
        if (maxTokens <= 0) return false
        val budget = (maxTokens.toLong() * 7L) / 2L
        return prompt.length.toLong() <= budget
    }
}