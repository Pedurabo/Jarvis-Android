package com.example.jarvis.core.network

import java.util.Locale

/**
 * CDI.6.0-A6.9-D4A
 *
 * Formats validated geographic-discovery outcomes.
 *
 * This policy never fetches locations, accesses GPS,
 * calculates routes, or triggers external applications.
 *
 * Callers must validate provider results before presentation.
 */
internal object KampalaGeoDiscoveryPresentation {

    private const val MAX_DISPLAYED_PLACES = 10

    private fun safeText(value: String, limit: Int = 140): String =
        value.replace(Regex("""[\p{Cc}\p{Cf}]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(limit)

    private fun coordinate(value: Double): String =
        String.format(Locale.US, "%.6f", value)

    fun format(
        result: KampalaGeoDiscoveryContract.Result,
        category: String
    ): String {
        val safeCategory = safeText(category, 80)
            .ifBlank { "places" }

        return when (result) {
            is KampalaGeoDiscoveryContract.Result.Unavailable -> {
                val reason = safeText(result.reason, 180)
                    .ifBlank { "The geographic provider did not supply a reason." }

                "Geographic discovery is currently unavailable " +
                    "for $safeCategory. " +
                    "I cannot confirm current map listings. " +
                    "Please try again later or provide a " +
                    "specific place name or address. " +
                    "No destination or route has been verified." +
                    "\nDiagnostic: $reason"
            }

            is KampalaGeoDiscoveryContract.Result.Success -> {
                val page = result.page
                val provider = safeText(page.provider, 100)
                    .ifBlank { "Unidentified provider" }

                if (page.places.isEmpty()) {
                    "No $safeCategory listings were returned by " +
                        "$provider for the requested search area. " +
                        "This does not mean such places are absent. " +
                        "Map coverage may be incomplete."
                } else {
                    buildString {
                        appendLine("Geographic place discovery")
                        appendLine("Provider: $provider")
                        appendLine()

                        page.places
                            .take(MAX_DISPLAYED_PLACES)
                            .forEachIndexed { index, place ->
                                appendLine(
                                    "${index + 1}. " +
                                        safeText(place.name)
                                )

                                appendLine(
                                    "Category: " +
                                        safeText(place.category, 80)
                                )

                                val area = place.area
                                    ?.let { safeText(it, 120) }
                                    ?.takeIf { it.isNotBlank() }

                                if (area != null) {
                                    appendLine("Area: $area")
                                }

                                val address = place.publicAddress
                                    ?.let { safeText(it, 250) }
                                    ?.takeIf { it.isNotBlank() }

                                if (address != null) {
                                    appendLine(
                                        "Listed address: $address"
                                    )
                                }

                                appendLine(
                                    "Map coordinates: " +
                                        coordinate(
                                            place.coordinates.latitude
                                        ) +
                                        ", " +
                                        coordinate(
                                            place.coordinates.longitude
                                        )
                                )

                                appendLine(
                                    "Map record: ${place.sourceUrl}"
                                )

                                appendLine()
                            }

                        if (
                            page.places.size > MAX_DISPLAYED_PLACES ||
                            page.nextPageToken != null
                        ) {
                            appendLine(
                                "Only part of the returned " +
                                    "result set is displayed."
                            )
                        }

                        appendLine(
                            "These are map-provider listings, " +
                                "not independently verified addresses, " +
                                "business operations, or opening hours."
                        )

                        if (
                            provider.contains(
                                "OpenStreetMap",
                                ignoreCase = true
                            )
                        ) {
                            appendLine(
                                "Map data: © OpenStreetMap " +
                                    "contributors (ODbL)."
                            )
                        }

                        append(
                            "Confirm the location with the " +
                                "relevant provider before travelling."
                        )
                    }.trim()
                }
            }
        }
    }
}