package com.example.jarvis.core.ai

import java.net.URLEncoder

/**
 * CDI.6.0-A6.8-A
 *
 * Builds a user-controlled Google Maps directions URL
 * from published destination information.
 *
 * It does not launch the URL, calculate a journey,
 * estimate travel times, or verify map coordinates.
 */
internal object KampalaMapsHandoffPolicy {

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    private fun buildUrl(
        origin: String?,
        destinationName: String,
        destinationAddress: String,
        sourceUrl: String
    ): String? {
        val cleanOrigin = origin?.trim().orEmpty()
        val cleanName = destinationName.trim()
        val cleanAddress = destinationAddress.trim()

        if (cleanOrigin.length !in 2..80) return null
        if (cleanName.isBlank()) return null
        if (cleanAddress.isBlank()) return null

        // Source attribution must exist before offering
        // this location handoff.
        if (!sourceUrl.startsWith("https://")) return null

        // Keep both the published place name and address
        // in the external Maps search destination.
        val destination = "$cleanName, $cleanAddress"

        return "https://www.google.com/maps/dir/" +
            "?api=1" +
            "&origin=${encode(cleanOrigin)}" +
            "&destination=${encode(destination)}"
    }

    fun urlOrNull(
        journey: KampalaDestinationJourney
    ): String? =
        buildUrl(
            origin = journey.origin,
            destinationName = journey.destinationName,
            destinationAddress = journey.address,
            sourceUrl = journey.sourceUrl
        )

    fun urlOrNull(
        place: KampalaSourceBackedPlacePolicy.Place,
        origin: String?
    ): String? =
        buildUrl(
            origin = origin,
            destinationName = place.name,
            destinationAddress = place.address,
            sourceUrl = place.sourceUrl
        )
}