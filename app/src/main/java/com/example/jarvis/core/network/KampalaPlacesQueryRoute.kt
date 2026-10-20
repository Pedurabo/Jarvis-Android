package com.example.jarvis.core.network

object KampalaPlacesQueryRoute {

    data class SearchRequest(
        val query: String? = null,
        val area: String? = null,
        val category: String? = null
    )

    private val requestPattern = Regex(
        """^(?:find|show me|search for|where can i find)\s+(?:a\s+|the\s+|some\s+)?(shopping places|shops|mosques?|places|acacia mall|gaddafi national mosque)(?:\s+in\s+(kisementi|old kampala))?\??$""",
        RegexOption.IGNORE_CASE
    )

    // M1F-I3C.120B.1: expanded, source-grounded discovery routing.
    // The route does not fabricate businesses or service availability.
    private val expandedPattern = Regex(
        """^(?:find|show me|search for|list|where can i find)\s+(?:a\s+|an\s+|the\s+|some\s+)?(restaurants?|cafes?|hotels?|guesthouses?|hospitals?|clinics?|markets?|supermarkets?|shopping centres?|shopping centers?|malls?|museums?|parks?|embass(?:y|ies)|government offices?|church(?:es)?|mosques?|places)(?:\s+in\s+(kampala|kisementi|old kampala|kololo|nakasero|ntinda|bugolobi|makindye))?\??$""",
        RegexOption.IGNORE_CASE
    )

    private fun resolveExpanded(input: String): SearchRequest? {
        val match = expandedPattern.matchEntire(input.trim())
            ?: return null

        val subject = match.groupValues[1].lowercase()
        val location = match.groupValues[2].lowercase()

        val area = when (location) {
            "kisementi" -> "Kisementi"
            "old kampala" -> "Old Kampala"
            // M1F-I3C.120L: additional Kampala neighbourhoods.
            "kololo" -> "Kololo"
            "nakasero" -> "Nakasero"
            "ntinda" -> "Ntinda"
            "bugolobi" -> "Bugolobi"
            "makindye" -> "Makindye"
            else -> null
        }

        val category = when (subject) {
            "restaurant", "restaurants" -> "restaurant"
            "cafe", "cafes" -> "cafe"
            "hotel", "hotels", "guesthouse", "guesthouses" ->
                "accommodation"

            "hospital", "hospitals", "clinic", "clinics" ->
                "healthcare"

            "market", "markets" -> "market"
            "supermarket", "supermarkets" -> "supermarket"

            "shopping centre", "shopping centres",
            "shopping center", "shopping centers",
            "mall", "malls" -> "shopping"

            "museum", "museums" -> "museum"
            "park", "parks" -> "park"
            "embassy", "embassies" -> "embassy"
            "government office", "government offices" ->
                "government"

            "church", "churches" -> "church"
            else -> null
        }

        return when {
            category != null ->
                SearchRequest(area = area, category = category)

            subject == "mosque" || subject == "mosques" ->
                SearchRequest(query = "mosque", area = area)

            subject == "places" && area != null ->
                SearchRequest(area = area)

            else -> null
        }
    }
    fun resolve(input: String): SearchRequest? {
        val match = requestPattern.matchEntire(input.trim()) ?: return resolveExpanded(input)

        val subject = match.groupValues[1].lowercase()
        val location = match.groupValues[2].lowercase()

        val area = when (location) {
            "kisementi" -> "Kisementi"
            "old kampala" -> "Old Kampala"
            else -> null
        }

        return when (subject) {
            "shopping places", "shops" ->
                SearchRequest(area = area, category = "shopping")

            "mosque", "mosques" ->
                SearchRequest(query = "mosque", area = area)

            "acacia mall" ->
                SearchRequest(query = "Acacia Mall", area = area)

            "gaddafi national mosque" ->
                SearchRequest(query = "Gaddafi National Mosque", area = area)

            "places" ->
                if (area != null) SearchRequest(area = area) else null

            else -> null
        }
    }
}
