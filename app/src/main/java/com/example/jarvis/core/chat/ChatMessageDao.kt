package com.example.jarvis.core.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("""
        SELECT * FROM chat_messages
        WHERE conversationId = :conversationId
        ORDER BY timestamp ASC, id ASC
    """)
    suspend fun getMessages(conversationId: String): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessages(conversationId: String)
}