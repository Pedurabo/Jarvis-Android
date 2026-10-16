package com.example.jarvis.core.ai

import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(
    val messages: List<ChatMessage>,
    val model: String = "gpt-4-turbo"
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val success: Boolean,
    val data: ChatMessage,
    val usage: TokenUsage?
)

data class TokenUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

interface JarvisApiService {
    @POST("/chat")
    suspend fun getChatCompletion(@Body request: ChatRequest): ChatResponse
}
