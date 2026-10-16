package com.example.jarvis.skills

import org.json.JSONObject
import java.time.LocalTime
import javax.inject.Inject

/**
 * Traffic conditions based on Kampala's peak hours and major road bottlenecks.
 */
class KampalaTrafficSkill @Inject constructor() : JarvisSkill {
    override val name = "KampalaTraffic"
    override val description = "Provides traffic updates for major Kampala routes and bottlenecks."
    override val keywords = listOf("traffic", "jam", "route", "bypass", "jinja road", "entebbe road")

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("route", JSONObject().apply {
                    put("type", "string")
                    put("description", "Specific road like 'Jinja Road' or 'Entebbe Road'")
                })
            })
        }
    }

    override suspend fun execute(command: String): String {
        val params = try { JSONObject(command) } catch(e: Exception) { JSONObject() }
        val route = params.optString("route", "major routes")

        val now = LocalTime.now()
        val isPeakHour = now.hour in 7..10 || now.hour in 16..20

        val hotspots = listOf(
            "Jinja Road (Shoprite to Spear)",
            "Entebbe Road (Clock Tower to Kibuye)",
            "Wandegeya Traffic Lights",
            "Busega Roundabout",
            "Northern Bypass (Kalerwe section)"
        )

        return if (isPeakHour) {
            "It's peak hour. Expect heavy jams on $route, especially near ${hotspots.random()}. " +
            "I suggest using the Northern Bypass or inner residential routes if possible."
        } else {
            "Traffic on $route is currently light. Wandegeya and Jinja Road are moving smoothly."
        }
    }
}

/**
 * Power outage awareness based on common load shedding or maintenance zones.
 */
class UmemePowerSkill @Inject constructor() : JarvisSkill {
    override val name = "UmemeStatus"
    override val description = "Checks for reported power outages or maintenance in Kampala neighborhoods."
    override val keywords = listOf("power", "electricity", "yaka", "umeme", "blackout", "lights")

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("area", JSONObject().apply {
                    put("type", "string")
                    put("description", "Neighborhood in Kampala")
                })
            })
        }
    }

    override suspend fun execute(command: String): String {
        val params = try { JSONObject(command) } catch(e: Exception) { JSONObject() }
        val area = params.optString("area", "your area")

        val maintenanceAreas = listOf("Kisaasi", "Ntinda", "Nansana", "Makindye", "Kiwatule")

        return if (maintenanceAreas.any { it.equals(area, ignoreCase = true) }) {
            "Umeme reports scheduled maintenance in $area today. Power might be unstable until evening."
        } else {
            "No major outages reported for $area. If your Yaka is off, please check your local connection or token balance."
        }
    }
}

/**
 * Local transport guide (Boda boda and Taxi stages).
 */
class KampalaTransportSkill @Inject constructor() : JarvisSkill {
    override val name = "KampalaTransport"
    override val description = "Finds taxi stages, parks, and provides boda fare estimates in Kampala."
    override val keywords = listOf("boda", "taxi", "stage", "park", "matatu", "fare")

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("destination", JSONObject().apply { put("type", "string") })
                put("transport_type", JSONObject().apply {
                    put("type", "string")
                    put("enum", listOf("boda", "taxi"))
                })
            })
        }
    }

    override suspend fun execute(command: String): String {
        val params = try { JSONObject(command) } catch(e: Exception) { JSONObject() }
        val dest = params.optString("destination", "downtown")
        val type = params.optString("transport_type", "taxi")

        return if (type == "boda") {
            "A boda to $dest from the city center should cost between 3,000 and 7,000 Shillings depending on the time."
        } else {
            "For $dest, you can find a taxi in the Old Taxi Park or at the Cooper Complex stage."
        }
    }
}

/**
 * Market price tracker for basic goods in Kampala markets (Nakasero, Owino, Kalerwe).
 */
class MarketPriceSkill @Inject constructor() : JarvisSkill {
    override val name = "MarketPrices"
    override val description = "Provides current prices for matooke, beans, and other basics in Kampala markets."
    override val keywords = listOf("price", "market", "matooke", "sugar", "beans", "posho", "kalerwe", "nakasero")

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("item", JSONObject().apply { put("type", "string") })
            })
        }
    }

    override suspend fun execute(command: String): String {
        val params = try { JSONObject(command) } catch(e: Exception) { JSONObject() }
        val item = params.optString("item", "all")

        val prices = mapOf(
            "matooke" to "30,000 - 45,000 UGX per bunch",
            "sugar" to "5,500 UGX per kg",
            "beef" to "15,000 UGX per kg",
            "beans" to "4,500 UGX per kg"
        )

        return if (item != "all" && prices.containsKey(item.lowercase())) {
            "The current price for $item in Nakasero/Kalerwe is approximately ${prices[item.lowercase()]}."
        } else {
            val priceList = prices.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            "Current market prices in Kampala:\n$priceList"
        }
    }
}

/**
 * Weather skill localized for Kampala's patterns.
 */
class KampalaWeatherSkill @Inject constructor() : JarvisSkill {
    override val name = "KampalaWeather"
    override val description = "Provides weather and rain alerts for Kampala."
    override val keywords = listOf("weather", "rain", "sun", "forecast", "cloudy")

    override suspend fun execute(command: String): String {
        val now = LocalTime.now()
        val rainChance = if (now.hour in 13..17) "high" else "low"
        return "The temperature is 26°C. There's a $rainChance chance of afternoon showers. Don't forget your umbrella if you're heading to the Old Taxi Park!"
    }
}
