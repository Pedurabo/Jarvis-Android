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

    fun resolve(input: String): SearchRequest? {
        val match = requestPattern.matchEntire(input.trim()) ?: return null

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