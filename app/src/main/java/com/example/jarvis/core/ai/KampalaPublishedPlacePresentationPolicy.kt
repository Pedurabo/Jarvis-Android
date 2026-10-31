package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.7-C
 *
 * Transitional presentation rule for a curated,
 * official-source place record.
 *
 * This class never verifies a route or a live location.
 * General typed provenance will replace this narrow rule.
 */
internal object KampalaPublishedPlacePresentationPolicy {

    private const val DIRECTORY_NOTICE =
        "Directory information unavailable or incomplete"

    private const val PUBLISHED_PLACE_NOTICE =
        "Official published address · Confirm map details"

    fun directoryNoticeLabel(messageText: String): String {
        val place =
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Watoto Church Downtown"
            ) ?: return DIRECTORY_NOTICE

        val hasCanonicalName = messageText.contains(
            place.name,
            ignoreCase = true
        )

        val hasAddress = messageText.contains(
            place.address,
            ignoreCase = true
        )

        val hasOfficialAttribution = messageText.contains(
            "Official source: ${place.sourceUrl}",
            ignoreCase = true
        )

        return if (
            hasCanonicalName &&
            hasAddress &&
            hasOfficialAttribution
        ) {
            PUBLISHED_PLACE_NOTICE
        } else {
            DIRECTORY_NOTICE
        }
    }
}