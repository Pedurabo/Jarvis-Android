package com.example.jarvis.core.network

import retrofit2.http.GET
import retrofit2.http.Query

data class KampalaPlace(
    val id: String,
    val name: String,
    val category: String,
    val area: String,
    val address: String,
    val sourceUrl: String,
    val verifiedAt: String
)

data class KampalaPlacesResponse(
    val schemaVersion: Int,
    val count: Int,
    val places: List<KampalaPlace>
)

interface KampalaPlacesApiService {
    @GET("places")
    suspend fun searchPlaces(
        @Query("q") query: String? = null,
        @Query("area") area: String? = null,
        @Query("category") category: String? = null
    ): KampalaPlacesResponse
}