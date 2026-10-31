package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.9-D4F-B
 *
 * Adds an existing, user-controlled Maps handoff to a
 * published destination response.
 *
 * No location fetching, device actions, route calculations,
 * GPS reading or assumptions about travel time.
 */
internal object KampalaPublishedJourneyAction {

    private const val PREFIX =
        "Review route in Google Maps (tap to open): "

    fun append(
        notice: String,
        journey: KampalaDestinationJourney?
    ): String {
        if (journey == null) return notice

        // Require the published destination, its HTTPS source
        // and a user-supplied origin through the existing policy.
        val url = KampalaMapsHandoffPolicy.urlOrNull(journey)
            ?: return notice

        if (notice.contains(PREFIX)) return notice

        return notice.trimEnd() +
            "\n\n" +
            PREFIX +
            url
    }
}