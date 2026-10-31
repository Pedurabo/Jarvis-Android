package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.7-B
 *
 * A short response when a user clarifies a destination
 * already identified from a published official source.
 *
 * No route, travel time, opening status or live map result
 * is inferred by this policy.
 */
internal object KampalaSourceClarificationResponsePolicy {

    fun respond(
        place: KampalaSourceBackedPlacePolicy.Place,
        startingPoint: String?
    ): String {
        val originNotice =
            if (startingPoint.isNullOrBlank()) {
                ""
            } else {
                "Your starting point is still " +
                    "${startingPoint.trim()}. "
            }

        return originNotice +
            "Yes, ${place.name} is the destination " +
            "we already identified. " +
            "Its published address is ${place.address}. " +
            "Official source: ${place.sourceUrl}. " +
            "I haven't calculated or checked a route; " +
            "please confirm directions in your maps app." +
            (KampalaMapsHandoffPolicy.urlOrNull(
                place,
                startingPoint
            )?.let { url ->
                " Review route in Google Maps (tap to open): $url"
            } ?: "")
    }
}