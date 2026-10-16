package com.example.jarvis.core.ai

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponseCache @Inject constructor() {
    // Simple LRU cache for frequent queries. Key: Query, Value: AssistantResponse
    private val cache = LruCache<String, AssistantController.AssistantResponse>(50)

    fun get(query: String): AssistantController.AssistantResponse? {
        return cache.get(query.lowercase().trim())
    }

    fun put(query: String, response: AssistantController.AssistantResponse) {
        cache.put(query.lowercase().trim(), response)
    }
}
