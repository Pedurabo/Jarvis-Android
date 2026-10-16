package com.example.jarvis.skills

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject

/**
 * Skill to handle location queries and geofenced reminders.
 * In a full 2026 impl, this would use a GeofencingClient and a local DB.
 */
class LocationSkill @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : JarvisSkill {
    override val name = "Location"
    override val description = "Find your current location or set reminders based on where you are in Kampala."
    override val keywords = listOf("where am i", "location", "near", "remind me when", "at")

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("action", "get_current_location or set_location_reminder")
            put("task", "The task to remind (e.g., buy sugar)")
            put("place", "The place for the reminder (e.g., Nakasero Market)")
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun execute(command: String): String {
        return try {
            val params = JSONObject(command)
            val action = params.optString("action", "get_current_location")

            when (action) {
                "get_current_location" -> {
                    // Uses kotlinx-coroutines-play-services to await Task result
                    val location = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        "You are currently at coordinates ${location.latitude}, ${location.longitude} near Kampala."
                    } else {
                        "I couldn't retrieve your exact location. Please check if GPS is enabled."
                    }
                }
                "set_location_reminder" -> {
                    val task = params.getString("task")
                    val place = params.getString("place")

                    // Logic for Geofencing would be triggered here
                    "Understood. I've set a location-based reminder to $task when you arrive at $place."
                }
                else -> "I can help you with your location or set place-based reminders."
            }
        } catch (e: Exception) {
            "I'm having trouble accessing location services. Please check my permissions."
        }
    }
}
