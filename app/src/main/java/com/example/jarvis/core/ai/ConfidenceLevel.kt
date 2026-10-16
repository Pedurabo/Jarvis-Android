package com.example.jarvis.core.ai

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

data class ConfidenceResult(
    val level: ConfidenceLevel,
    val score: Float,
    val reasons: List<String>
)
