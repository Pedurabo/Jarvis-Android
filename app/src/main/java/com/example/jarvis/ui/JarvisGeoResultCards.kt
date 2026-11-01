package com.example.jarvis.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * P1-B: Presentation only.
 *
 * No network access, GPS access, route calculation, or place verification.
 * Malformed responses fall back to the existing chat renderer.
 */
internal object JarvisGeoResultCardsModel {

    data class Place(
        val number: Int,
        val name: String,
        val category: String,
        val coordinates: String,
        val sourceUrl: String,
        val distanceLabel: String? = null
    )

    data class Result(
        val area: String,
        val provider: String,
        val places: List<Place>,
        val disclaimer: String,
        val displayedCount: Int? = null,
        val returnedCount: Int? = null
    )

    private val itemStart = Regex(
        """(?m)^(\d{1,2})\.\s+([^\r\n]+)\s*$"""
    )

    private val categoryPattern = Regex(
        """(?m)^Category:\s*([^\r\n]+)"""
    )

    private val distancePattern = Regex(
        """(?m)^Approx\. straight-line distance:\s*(\d+(?:\.\d+)?\s*(?:m|km))\s*$"""
    )

    private val coordinatePattern = Regex(
        """(?m)^Map coordinates:\s*([^\r\n]+)"""
    )

    private val sourcePattern = Regex(
        """Map record:\s*(https://www\.openstreetmap\.org/(?:node|way|relation)/\d+)"""
    )

    // JARVIS-PHASE36I-NATIVE-METRO-CARDS-V1
    //
    // Strictly parses the generated metropolitan text contract.
    // No network, GPS, navigation or invented coordinates.
    private fun parseMetropolitan(text: String): Result? {
        if (!text.startsWith("Map search: ")) {
            return null
        }

        val lines = text.lines()

        if (lines.size < 5) return null

        val heading = lines.first()
            .removePrefix("Map search: ")
            .trim()

        val headingMatch = Regex(
            """^(.{2,80}) around (.{2,100})$"""
        ).matchEntire(heading) ?: return null

        val category = headingMatch.groupValues[1].trim()
        val area = headingMatch.groupValues[2].trim()

        if (
            category.isBlank() ||
            area.isBlank() ||
            category.any { it.isISOControl() } ||
            area.any { it.isISOControl() }
        ) {
            return null
        }

        if (
            !text.contains(
                "Search radius: 5 km from the mapped locality."
            ) ||
            !text.contains(
                "Source: OpenStreetMap / Jarvis Uganda snapshot."
            ) ||
            !text.contains(
                "Map listings are not independently verified."
            ) ||
            !text.contains(
                "No navigation, booking or payment was initiated."
            )
        ) {
            return null
        }

        val pattern = Regex(
            """(?m)^([1-3])\. ([^\r\n]{1,120})\r?\nOSM: (https://www\.openstreetmap\.org/(?:node|way|relation)/[1-9][0-9]*)\s*$"""
        )

        val matches = pattern.findAll(text).toList()

        if (matches.isEmpty() || matches.size > 3) {
            return null
        }

        val places = matches.mapIndexed { index, match ->
            val number = match.groupValues[1].toIntOrNull()
                ?: return null

            val name = match.groupValues[2].trim()
            val source = match.groupValues[3]

            if (
                number != index + 1 ||
                name.isBlank() ||
                name.any { it.isISOControl() }
            ) {
                return null
            }

            Place(
                number = number,
                name = name,
                category = category,
                coordinates = "",
                sourceUrl = source
            )
        }

        if (
            places.map { it.sourceUrl }.distinct().size !=
            places.size
        ) {
            return null
        }

        return Result(
            area = area,
            provider = "OpenStreetMap / Jarvis Uganda snapshot",
            places = places,
            disclaimer =
                "Map-provider listings, not independently verified. " +
                "Opening hours, entrances and availability may " +
                "have changed. No navigation or booking was initiated."
        )
    }
    fun parse(text: String): Result? {
        parseMetropolitan(text)?.let { return it }

        val isNearbySearch = text.startsWith(
            "Nearby search centred on your device's current location."
        )

        if (
            !isNearbySearch &&
            !text.startsWith("Experimental map search: ")
        ) {
            return null
        }

        if (!text.contains("\nGeographic place discovery\n")) {
            return null
        }

        val area = if (isNearbySearch) {
            "your device location"
        } else {
            text.lineSequence().first()
                .removePrefix("Experimental map search: ")
                .trim()
        }

        if (area.isBlank() || area.length > 100) return null

        val provider = text.lineSequence()
            .firstOrNull { it.startsWith("Provider: ") }
            ?.removePrefix("Provider: ")
            ?.trim()
            ?: return null

        if (provider.isBlank() || provider.length > 200) return null

        val entries = itemStart.findAll(text).toList()

        if (entries.isEmpty() || entries.size > 10) return null

        val places = entries.mapIndexed { index, match ->
            val end = entries.getOrNull(index + 1)
                ?.range?.first
                ?: text.length

            val section = text.substring(match.range.last + 1, end)

            val name = match.groupValues[2].trim()
            val category = categoryPattern.find(section)
                ?.groupValues?.get(1)?.trim()
                ?: return null
            val coordinates = coordinatePattern.find(section)
                ?.groupValues?.get(1)?.trim()
                ?: return null
            val distanceLabel = if (isNearbySearch) {
                distancePattern.find(section)
                    ?.groupValues?.get(1)?.trim()
            } else {
                null
            }

            val source = sourcePattern.find(section)
                ?.groupValues?.get(1)
                ?: return null

            val number = match.groupValues[1].toIntOrNull()
                ?: return null

            if (
                number != index + 1 ||
                name.isBlank() ||
                name.length > 160 ||
                category.isBlank() ||
                category.length > 80
            ) {
                return null
            }

            val coordinateParts = coordinates.split(",")

            if (coordinateParts.size != 2) return null

            val latitude = coordinateParts[0].trim().toDoubleOrNull()
                ?: return null
            val longitude = coordinateParts[1].trim().toDoubleOrNull()
                ?: return null

            if (
                !latitude.isFinite() ||
                !longitude.isFinite() ||
                latitude !in -90.0..90.0 ||
                longitude !in -180.0..180.0
            ) {
                return null
            }

            Place(
                number = number,
                name = name,
                category = category,
                coordinates = coordinates,
                sourceUrl = source,
                distanceLabel = distanceLabel
            )
        }

        if (places.map { it.sourceUrl }.distinct().size != places.size) {
            return null
        }

        val counts = Regex(
            """(?m)^Displayed (\d+) of (\d+) returned map listings\r?$"""
        ).find(text)

        val displayedCount = counts?.groupValues?.get(1)?.toIntOrNull()
        val returnedCount = counts?.groupValues?.get(2)?.toIntOrNull()

        if (counts != null && (
                displayedCount != places.size ||
                returnedCount == null ||
                returnedCount < displayedCount
            )
        ) return null

        val disclaimer = text.substringAfter(
            "These are map-provider listings,",
            ""
        ).let {
            if (it.isBlank()) {
                "Map-provider listings are not independent verification " +
                    "of addresses, operations or opening hours."
            } else {
                "These are map-provider listings," + it.trimEnd()
            }
        }

        return Result(
            area = area,
            provider = provider,
            places = places,
            disclaimer = disclaimer.take(500),
            displayedCount = displayedCount,
            returnedCount = returnedCount
        )
    }
}

@Composable
internal fun JarvisGeoResultCards(
    result: JarvisGeoResultCardsModel.Result
) {
    val uriHandler = LocalUriHandler.current

    // UI state only. No GPS, network search or navigation mutation.
    val expandedSources = remember(result) {
        androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    }

    val countText = remember(result) {
        if (result.displayedCount != null && result.returnedCount != null) {
            "${result.displayedCount} of ${result.returnedCount} returned map listings"
        } else {
            "${result.places.size} map listings displayed"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Map listings in ${result.area}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "$countText · OpenStreetMap",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        result.places.forEach { place ->
            val expanded = expandedSources[place.sourceUrl] == true

            // JARVIS-GOOGLE-MAPS-CARD-ACTION-V1
            // Use the geographic provider's coordinates.
            // No route is calculated or started automatically.
            val googleMapsUrl = remember(place.coordinates) {
                val parts = place.coordinates.split(",")
                val latitude = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
                val longitude = parts.getOrNull(1)?.trim()?.toDoubleOrNull()

                if (
                    latitude != null &&
                    longitude != null &&
                    latitude.isFinite() &&
                    longitude.isFinite() &&
                    latitude in -90.0..90.0 &&
                    longitude in -180.0..180.0
                ) {
                    "https://www.google.com/maps/search/?api=1&query=" +
                        "$latitude,$longitude"
                } else {
                    null
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // JARVIS-GEO-CARD-LAYOUT-V2
                    // Full-width heading with a separate action row.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${place.number}. ${place.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = JarvisP2B8ListingPolicy.cardSubtitle(place.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (place.distanceLabel != null) {
                            Text(
                                text = "Approx. ${place.distanceLabel} away (straight line)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        if (googleMapsUrl != null) {
                            TextButton(
                                modifier = Modifier.heightIn(min = 48.dp),
                                onClick = {
                                    runCatching {
                                        uriHandler.openUri(googleMapsUrl)
                                    }
                                }
                            ) {
                                Text("Google Maps")
                            }
                        }

                        TextButton(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription = if (expanded) {
                                        "Hide map details for ${place.name}"
                                    } else {
                                        "Show map details for ${place.name}"
                                    }
                                },
                            onClick = {
                                expandedSources[place.sourceUrl] = !expanded
                            }
                        ) {
                            Text(if (expanded) "Hide" else "Details")
                        }
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(4.dp))

                        if (place.coordinates.isNotBlank()) {
                            Text(
                                text = "Map coordinates: ${place.coordinates}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription =
                                        "Open OpenStreetMap source for ${place.name}"
                                },
                            onClick = {
                                runCatching {
                                    uriHandler.openUri(place.sourceUrl)
                                }
                            },
                            contentPadding = PaddingValues(
                                horizontal = 0.dp,
                                vertical = 0.dp
                            )
                        ) {
                            Text("View map source")
                        }
                    }
                }
            }
        }

        Text(
            text = "Source: ${result.provider}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = result.disclaimer,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
