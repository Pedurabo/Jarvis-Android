package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.6-D2
 *
 * Curated source-backed Kampala location.
 *
 * The published address is not a live routing result.
 * No GPS, opening-hours, or journey verification is implied.
 */
internal object KampalaSourceBackedPlacePolicy {

    data class Place(
        val name: String,
        val area: String,
        val address: String,
        val sourceUrl: String
    )

    private val watotoDowntown = Place(
        name = "Watoto Church Downtown",
        area = "Central Kampala",
        address = "Plot 87 Kampala Road, Kampala",
        sourceUrl = "https://watotochurch.com/locations/"
    )

    private fun words(text: String): String =
        text.lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    fun resolveOrNull(query: String): Place? =
        when (words(query)) {
            "watoto church downtown",
            "watoto downtown",
            "watoto church downtown branch" -> watotoDowntown
            else -> null
        }

    /**
     * Normalize known equivalent names and duplicated areas.
     * All unrelated destinations retain their supplied wording.
     */
    fun normalizeRequestedName(query: String): String {
        val normalized = words(query)

        return when (normalized) {
            "watoto church downtown downtown branch",
            "watoto church downtown the downtown branch",
            "watoto downtown downtown branch",
            "watoto church downtown branch",
            "watoto church downtown",
            "watoto downtown" -> watotoDowntown.name
            else -> query.trim()
        }
    }

    fun sourceNoticeOrNull(query: String): String? {
        val place = resolveOrNull(query) ?: return null

        return "${place.name} is listed by Watoto Church " +
            "at ${place.address}. " +
            "Area: ${place.area}. " +
            "Official source: ${place.sourceUrl}. " +
            "This is a published address, not a calculated " +
            "or independently verified journey. " +
            "Confirm the destination in your maps application " +
            "before travelling."
    }
}