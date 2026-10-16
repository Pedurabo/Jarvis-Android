package com.example.jarvis.core.ai

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("SELECT * FROM memories WHERE isSynced = 0")
    suspend fun getUnsyncedMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE isDeleted = 1")
    suspend fun getDeletedMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories")
    suspend fun getAllMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE firestoreId = :firestoreId")
    suspend fun getByFirestoreId(firestoreId: String): MemoryEntity?

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("SELECT MAX(timestamp) FROM memories")
    suspend fun getLastSyncTimestamp(): Long?
}
