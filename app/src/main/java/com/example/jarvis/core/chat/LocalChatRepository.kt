package com.example.jarvis.core.chat

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalChatRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val database = Room.databaseBuilder(
        context,
        ChatDatabase::class.java,
        "jarvis_conversations.db"
    ).build()

    private val dao = database.chatMessageDao()

    suspend fun save(
        role: String,
        text: String,
        timestamp: Long,
        origin: String = "AI_RESPONSE",
        confidence: String = "MEDIUM"
    ) {
        dao.insert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "main_chat",
                role = role,
                text = text,
                timestamp = timestamp,
                origin = origin,
                confidence = confidence
            )
        )
    }

    suspend fun load(): List<ChatMessageEntity> {
        return dao.getMessages("main_chat")
    }
}