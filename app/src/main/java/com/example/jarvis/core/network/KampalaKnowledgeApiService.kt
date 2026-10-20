package com.example.jarvis.core.network

import retrofit2.http.GET
import retrofit2.http.Query

data class KampalaKnowledgeSource(
    val publisher: String,
    val url: String
)

data class KampalaKnowledgeEntry(
    val id: String,
    val topic: String,
    val city: String,
    val country: String,
    val title: String,
    val answer: String,
    val reviewedAt: String,
    val sources: List<KampalaKnowledgeSource>
)

data class KampalaKnowledgeResponse(
    val schemaVersion: Int,
    val count: Int,
    val entries: List<KampalaKnowledgeEntry>
)

interface KampalaKnowledgeApiService {

    @GET("knowledge")
    suspend fun getKnowledge(
        @Query("topic") topic: String
    ): KampalaKnowledgeResponse
}