package com.example.jarvis.core.context

import androidx.annotation.Keep
import org.json.JSONArray
import org.json.JSONObject

@Keep
data class AssistantContext(
    val timestamp: String,
    val timeOfDay: String,
    val location: LocationInfo?,
    val deviceState: DeviceState,
    val calendarEvents: List<CalendarEvent>,
    val userActivity: String? = "Idle"
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("timestamp", timestamp)
            put("timeOfDay", timeOfDay)
            put("location", location?.toJsonObject() ?: JSONObject.NULL)
            put("deviceState", deviceState.toJsonObject())
            put("calendarEvents", JSONArray().apply {
                calendarEvents.forEach { put(it.toJsonObject()) }
            })
            put("userActivity", userActivity)
        }
    }
}

@Keep
data class LocationInfo(
    val city: String?,
    val coordinates: String
) {
    fun toJsonObject() = JSONObject().apply {
        put("city", city ?: JSONObject.NULL)
        put("coordinates", coordinates)
    }
}

@Keep
data class DeviceState(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkType: String
) {
    fun toJsonObject() = JSONObject().apply {
        put("batteryLevel", batteryLevel)
        put("isCharging", isCharging)
        put("networkType", networkType)
    }
}

@Keep
data class CalendarEvent(
    val title: String,
    val startTime: String,
    val isAllDay: Boolean
) {
    fun toJsonObject() = JSONObject().apply {
        put("title", title)
        put("startTime", startTime)
        put("isAllDay", isAllDay)
    }
}
