package com.example.jarvis.core.network

import com.example.jarvis.core.network.models.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleMapsApiService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("departure_time") departureTime: String = "now",
        @Query("traffic_model") trafficModel: String = "best_guess",
        @Query("key") apiKey: String
    ): DirectionsResponse
}
