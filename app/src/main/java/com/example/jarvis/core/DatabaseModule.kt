package com.example.jarvis.core

import android.content.Context
import androidx.room.Room
import com.example.jarvis.core.ai.MemoryDao
import com.example.jarvis.core.ai.MemoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MemoryDatabase {
        return Room.databaseBuilder(
            context,
            MemoryDatabase::class.java,
            "jarvis_memory.db"
        ).build()
    }

    @Provides
    fun provideMemoryDao(database: MemoryDatabase): MemoryDao {
        return database.memoryDao()
    }
}
