package com.example.jarvis.core.network

import com.example.jarvis.BuildConfig
import com.example.jarvis.core.network.models.DirectionsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TrafficService: A high-level service to interact with Google Maps Directions API.
 * Extracts travel duration, duration in traffic, and distance for smarter navigation context.
 */
@Singleton
class TrafficService @Inject constructor(
    private val apiService: GoogleMapsApiService
) {
    private val apiKey = BuildConfig.GOOGLE_MAPS_KEY

    /**
     * Fetches traffic details between two points.
     * @param origin Starting point (Address or Lat,Lng)
     * @param destination Ending point (Address or Lat,Lng)
     * @return A Result containing the directions response or an error
     */
    suspend fun getTrafficDetails(
        origin: String,
        destination: String
    ): Result<TrafficInfo> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE") {
                return@withContext Result.failure(Exception("Google Maps API Key is not configured."))
            }

            val response = apiService.getDirections(
                origin = origin,
                destination = destination,
                apiKey = apiKey
            )

            when (response.status) {
                "OK" -> {
                    if (response.routes.isNotEmpty()) {
                        val route = response.routes[0]
                        val leg = route.legs[0]

                        val info = TrafficInfo(
                            distance = leg.distance.text,
                            duration = leg.duration.text,
                            durationInTraffic = leg.durationInTraffic?.text ?: leg.duration.text,
                            durationValueSeconds = leg.duration.value,
                            durationInTrafficValueSeconds = leg.durationInTraffic?.value ?: leg.duration.value,
                            summary = route.summary.ifBlank { "main roads" }
                        )
                        Result.success(info)
                    } else {
                        Result.failure(Exception("No routes found between $origin and $destination"))
                    }
                }
                "ZERO_RESULTS" -> Result.failure(Exception("No routes found between these locations."))
                "OVER_QUERY_LIMIT" -> Result.failure(Exception("API quota exceeded. Please check billing."))
                "REQUEST_DENIED" -> Result.failure(Exception("API request denied: ${response.errorMessage ?: "Check API key permissions"}"))
                "INVALID_REQUEST" -> Result.failure(Exception("Invalid request parameters."))
                else -> Result.failure(Exception("Maps API Error: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper to get a human-readable summary of traffic status.
     */
    suspend fun getTrafficSummary(origin: String, destination: String): String {
        val result = getTrafficDetails(origin, destination)
        return result.fold(
            onSuccess = { info ->
                val delay = info.delayMinutes
                val delayText = if (delay > 0) " with a delay of $delay minutes due to traffic" else " with usual traffic"
                "The distance is ${info.distance}. It will take ${info.durationInTraffic} to reach $destination via ${info.summary}$delayText."
            },
            onFailure = { "Error: ${it.message}" }
        )
    }
}

/**
 * Clean data model for UI consumption
 */
data class TrafficInfo(
    val distance: String,
    val duration: String,
    val durationInTraffic: String,
    val durationValueSeconds: Int,
    val durationInTrafficValueSeconds: Int,
    val summary: String
) {
    val delayMinutes: Int
        get() = Math.max(0, (durationInTrafficValueSeconds - durationValueSeconds) / 60)
}
