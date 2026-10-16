package com.example.jarvis.skills

import org.json.JSONObject
import javax.inject.Inject

/**
 * Skill to provide local market prices and news for Kampala.
 * In a real-world app, this would fetch data from an API.
 */
class MarketNewsSkill @Inject constructor() : JarvisSkill {
    override val name = "MarketNews"
    override val description = "Get local market prices (e.g., Matooke, Beans) and Kampala news"
    override val keywords = listOf("price", "market", "cost", "news", "updates", "matooke")

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("item", "Name of the commodity (e.g., matooke, rice, sugar)")
            put("category", "Either 'market' or 'news'")
        }
    }

    override suspend fun execute(command: String): String {
        return try {
            val params = JSONObject(command)
            val category = params.optString("category", "market")
            val item = params.optString("item", "").lowercase()

            if (category == "news") {
                "Today's headlines: New infrastructure projects announced for the Kampala flyover, and local tech hubs are hosting a hackathon this weekend."
            } else {
                when {
                    item.contains("matooke") -> "A large bunch of Matooke is currently trading between 35,000 and 45,000 UGX at Nakasero Market."
                    item.contains("sugar") -> "Sugar prices remain stable at 5,000 UGX per kilogram in most Kampala supermarkets."
                    item.contains("charcoal") -> "A bag of charcoal has seen a slight increase, now at 90,000 UGX."
                    else -> "Market prices are stable today. Staples like beans are at 4,000 UGX per kilo. Is there a specific item you want to know about?"
                }
            }
        } catch (e: Exception) {
            "I couldn't fetch the latest market updates. Generally, prices in Kampala markets like Owino are stable today."
        }
    }
}
