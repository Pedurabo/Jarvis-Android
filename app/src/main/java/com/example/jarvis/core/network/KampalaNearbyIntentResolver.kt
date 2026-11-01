package com.example.jarvis.core.network

data class KampalaNearbyRequest(
    val category: String
)

object KampalaNearbyIntentResolver {

    private val categoryPhrases = listOf(
        "convenience stores" to "convenience_shop",
        "convenience shops" to "convenience_shop",
        "convenience shop" to "convenience_shop",
        "department stores" to "department_store",
        "department store" to "department_store",
        "fitness centres" to "fitness_centre",
        "fitness centers" to "fitness_centre",
        "fitness centre" to "fitness_centre",
        "fitness center" to "fitness_centre",
        "shopping malls" to "shopping_mall",
        "shopping mall" to "shopping_mall",
        "fuel stations" to "fuel_station",
        "fuel station" to "fuel_station",
        "guest houses" to "guest_house",
        "guest house" to "guest_house",
        "police stations" to "police",
        "police station" to "police",
        "sports centres" to "sports_centre",
        "sports centers" to "sports_centre",
        "sports centre" to "sports_centre",
        "sports center" to "sports_centre",
        "universities" to "university",
        "university" to "university",
        "supermarkets" to "supermarket",
        "supermarket" to "supermarket",
        "hospitals" to "hospital",
        "hospital" to "hospital",
        "pharmacies" to "pharmacy",
        "pharmacy" to "pharmacy",
        "clinics" to "clinic",
        "clinic" to "clinic",
        "schools" to "school",
        "school" to "school",
        "banks" to "bank",
        "bank" to "bank",
        "hotels" to "hotel",
        "hotel" to "hotel",
        "embassies" to "embassy",
        "embassy" to "embassy",
        "apartments" to "apartment",
        "apartment" to "apartment",
        "attractions" to "attraction",
        "attraction" to "attraction",
        "cafes" to "cafe",
        "cafe" to "cafe",
        "bars" to "bar",
        "bar" to "bar",
        "fast food" to "fast_food",
        "hostels" to "hostel",
        "hostel" to "hostel",
        "motels" to "motel",
        "motel" to "motel",
        "nightclubs" to "nightclub",
        "nightclub" to "nightclub",
        "pubs" to "pub",
        "pub" to "pub",
        "restaurants" to "restaurant",
        "restaurant" to "restaurant"
    )

    private val proximityPatterns = listOf(
        Regex("""\bnear\s+me\b"""),
        Regex("""\bnearby\b"""),
        Regex("""\bclose\s+to\s+me\b"""),
        Regex("""\baround\s+me\b"""),
        Regex("""\bnear\s+my\s+current\s+location\b"""),
        Regex("""\bnearest\b""")
    )

    fun parseOrNull(query: String): KampalaNearbyRequest? {
        val normalized = query
            .lowercase()
            .replace('á', 'a')
            .replace('é', 'e')
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (normalized.isEmpty()) return null
        if (proximityPatterns.none { it.containsMatchIn(normalized) }) return null

        val category = categoryPhrases.firstNotNullOfOrNull { (phrase, value) ->
            val pattern = Regex(
                """(?<![\p{L}\p{N}])${Regex.escape(phrase)}(?![\p{L}\p{N}])"""
            )
            value.takeIf { pattern.containsMatchIn(normalized) }
        } ?: return null

        return KampalaNearbyRequest(category)
    }
}
