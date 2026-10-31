package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.7-A2
 *
 * A source-backed destination and user-supplied origin.
 * Published location information is not a calculated route.
 */
internal data class KampalaDestinationJourney(
    val destinationName: String,
    val area: String,
    val address: String,
    val sourceUrl: String,
    val origin: String? = null,
    val routeCalculated: Boolean = false
) {

    init {
        require(destinationName.isNotBlank())
        require(address.isNotBlank())
        require(sourceUrl.startsWith("https://"))
    }

    fun withUserOrigin(userOrigin: String): KampalaDestinationJourney {
        val cleaned = userOrigin.trim()
        require(cleaned.length in 2..80)

        return copy(
            origin = cleaned,
            routeCalculated = false
        )
    }

    fun originFollowUpNotice(): String {
        val startingPoint = origin
            ?: return "Where will you be starting from?"

        return "You're starting from $startingPoint and heading to " +
            "$destinationName. The published destination address is " +
            "$address, in $area. " +
            "Official source: $sourceUrl. " +
            "Jarvis has not calculated or checked a route. " +
            "Confirm the destination and travel directions " +
            "in your maps application. " +
            (KampalaMapsHandoffPolicy.urlOrNull(this)?.let { url ->
                "Review route in Google Maps (tap to open): $url"
            } ?: "")
    }

    companion object {

        fun fromPublishedPlace(
            place: KampalaSourceBackedPlacePolicy.Place,
            previousOrigin: String? = null
        ): KampalaDestinationJourney {
            val initial = KampalaDestinationJourney(
                destinationName = place.name,
                area = place.area,
                address = place.address,
                sourceUrl = place.sourceUrl
            )

            return if (previousOrigin.isNullOrBlank()) {
                initial
            } else {
                initial.withUserOrigin(previousOrigin)
            }
        }
    }
}