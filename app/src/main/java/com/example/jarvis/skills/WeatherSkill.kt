package com.example.jarvis.skills

import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherSkill @Inject constructor() : JarvisSkill {
    override val name = "Weather"
    override val description = "Get current weather, temperature, and rain forecast."
    override val keywords = listOf("weather", "temperature", "rain", "sun", "forecast", "forecast")

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("location", JSONObject().apply {
                    put("type", "string")
                    put("description", "City or neighborhood name")
                })
            })
        }
    }

    override suspend fun execute(command: String): String {
        val location = try {
            JSONObject(command).optString("location", "Kampala")
        } catch (e: Exception) {
            "Kampala"
        }

        // Static response for now, but localized to Uganda
        return "In $location, it's currently 26°C with scattered clouds. There's a 20% chance of rain this afternoon."
    }
}
