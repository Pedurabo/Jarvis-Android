package com.example.jarvis.skills

import android.annotation.SuppressLint
import com.example.jarvis.core.network.TrafficService
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject

/**
 * TrafficSkill: Integrates with Google Maps Directions API via TrafficService
 * to provide real-time traffic updates and travel estimates.
 *
 * It automatically uses the user's current location if an origin isn't specified.
 */
class TrafficSkill @Inject constructor(
    private val trafficService: TrafficService,
    private val fusedLocationClient: FusedLocationProviderClient
) : JarvisSkill {
    override val name = "Traffic"
    override val description = "Get real-time traffic, travel times, and route information. " +
            "Supports current location or specific start/end points."
    override val keywords = listOf(
        "traffic", "jam", "route", "roads", "how long to get to",
        "traffic to", "travel time", "directions"
    )

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("origin", JSONObject().apply {
                    put("type", "string")
                    put("description", "Starting point. Defaults to your current location if omitted.")
                })
                put("destination", JSONObject().apply {
                    put("type", "string")
                    put("description", "Where you want to go (e.g., 'Entebbe Airport').")
                    put("default", "Kampala")
                })
                put("location", JSONObject().apply {
                    put("type", "string")
                    put("description", "Alias for destination. Use this for general traffic queries.")
                    put("default", "Kampala")
                })
            })
            put("required", listOf("destination"))
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun execute(command: String): String {
        return try {
            val params = JSONObject(command)
            // Support both 'destination' and 'location' keys from LLM
            val destination = params.optString("destination").ifBlank {
                params.optString("location")
            }.ifBlank { "Kampala" }

            // Determine origin: use provided string OR fetch current GPS coordinates
            var origin = params.optString("origin")
            if (origin.isNullOrBlank()) {
                val location = try {
                    fusedLocationClient.lastLocation.await()
                } catch (e: Exception) {
                    null
                }
                origin = if (location != null) {
                    "${location.latitude},${location.longitude}"
                } else {
                    "Kampala" // Fallback city center if GPS fails
                }
            }

            // Fetch traffic details from the network service
            val result = trafficService.getTrafficDetails(origin, destination)

            result.fold(
                onSuccess = { info ->
                    val delay = info.delayMinutes

                    // Determine traffic level based on delay
                    val level = when {
                        delay <= 2 -> "light"
                        delay <= 10 -> "moderate"
                        else -> "heavy"
                    }

                    val delayText = if (delay > 0) " with delays of $delay minutes" else ""

                    // Voice-friendly sentence
                    "Traffic to $destination is $level$delayText. The trip will take about ${info.durationInTraffic} via ${info.summary}."
                },
                onFailure = {
                    handleFailure(origin, destination)
                }
            )

        } catch (e: Exception) {
            handleFailure()
        }
    }

    private fun handleFailure(origin: String? = null, destination: String? = null): String {
        return when {
            origin != null && destination != null ->
                "I'm having trouble reaching the traffic servers for the route to $destination. " +
                "Generally, traffic in Kampala can be heavy on main roads during peak hours."
            else ->
                "I couldn't retrieve traffic info right now. Please check your network connection."
        }
    }
}
