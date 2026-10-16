package com.example.jarvis.core.ai

import androidx.annotation.Keep
import org.json.JSONObject

@Keep
data class UserProfile(
    val preferences: UserPreferences = UserPreferences(),
    val learnedHabits: List<Habit> = emptyList(),
    val routines: List<Routine> = emptyList()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("preferences", preferences.toJsonObject())
            put("learnedHabits", org.json.JSONArray().apply {
                learnedHabits.forEach { put(it.toJsonObject()) }
            })
            put("routines", org.json.JSONArray().apply {
                routines.forEach { put(it.toJsonObject()) }
            })
        }
    }
}

@Keep
data class UserPreferences(
    val responseTone: String = "Helpful and concise",
    val language: String = "English",
    val preferredName: String = "User"
) {
    fun toJsonObject() = JSONObject().apply {
        put("responseTone", responseTone)
        put("language", language)
        put("preferredName", preferredName)
    }
}

@Keep
data class Habit(
    val activity: String,
    val frequency: String,
    val context: String
) {
    fun toJsonObject() = JSONObject().apply {
        put("activity", activity)
        put("frequency", frequency)
        put("context", context)
    }
}

@Keep
data class Routine(
    val name: String,
    val time: String,
    val action: String
) {
    fun toJsonObject() = JSONObject().apply {
        put("name", name)
        put("time", time)
        put("action", action)
    }
}
