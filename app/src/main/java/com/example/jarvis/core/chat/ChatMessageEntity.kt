package com.example.jarvis.core.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val text: String,
    val timestamp: Long,
    val origin: String = "AI_RESPONSE",
    val confidence: String = "MEDIUM"
)