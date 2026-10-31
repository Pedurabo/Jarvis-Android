package com.example.jarvis.core.ai

import java.net.URI
import java.time.LocalDate

/**
 * CDI.6.0-A6.9-B
 *
 * Reusable place resolution contract.
 *
 * This component does not:
 * - fetch live map information
 * - read device GPS
 * - certify that a published address is accurate
 * - calculate directions
 * - launch an external app
 *
 * Providers and controller integration are separate layers.
 */
internal object KampalaPlaceResolutionPolicy {

    enum class SourceKind {
        DIRECTORY,
        OFFICIAL_PUBLICATION,
        MAP_PROVIDER
    }

    data class PlaceRecord(
        val provider: String,
        val providerId: String,
        val name: String,
        val category: String,
        val area: String,
        val address: String,
        val sourceUrl: String,
        val sourceKind: SourceKind,
        val sourceCheckedOn: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    )

    sealed class Resolution {
        data class Exact(
            val place: PlaceRecord
        ) : Resolution()

        data class Ambiguous(
            val places: List<PlaceRecord>
        ) : Resolution()

        object NotFound : Resolution()
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun validSource(url: String): Boolean =
        try {
            val uri = URI(url)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null
        } catch (_: Exception) {
            false
        }

    private fun validDate(value: String?): Boolean {
        if (value == null) return true

        return try {
            !LocalDate.parse(value).isAfter(LocalDate.now())
        } catch (_: Exception) {
            false
        }
    }

    private fun validCoordinates(
        latitude: Double?,
        longitude: Double?
    ): Boolean {
        if (latitude == null && longitude == null) {
            return true
        }

        if (latitude == null || longitude == null) {
            return false
        }

        return latitude.isFinite() &&
            longitude.isFinite() &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0
    }

    /**
     * Usable means structurally acceptable.
     * It does NOT mean independently verified.
     */
    fun isUsable(place: PlaceRecord): Boolean =
        place.provider.isNotBlank() &&
            place.providerId.isNotBlank() &&
            place.name.isNotBlank() &&
            place.category.isNotBlank() &&
            place.area.isNotBlank() &&
            place.address.isNotBlank() &&
            validSource(place.sourceUrl) &&
            validDate(place.sourceCheckedOn) &&
            validCoordinates(
                place.latitude,
                place.longitude
            )

    /**
     * Exact matching is intentionally conservative.
     *
     * Different provider records remain distinct.
     * Multiple matching branches are never silently
     * collapsed into a chosen destination.
     */
    fun resolveExact(
        requestedName: String,
        records: List<PlaceRecord>,
        requestedArea: String? = null
    ): Resolution {
        val name = normalize(requestedName)

        if (name.isBlank()) {
            return Resolution.NotFound
        }

        val area = requestedArea
            ?.let(::normalize)
            ?.takeIf { it.isNotBlank() }

        val matches = records
            .asSequence()
            .filter(::isUsable)
            .filter {
                normalize(it.name) == name &&
                    (
                        area == null ||
                            normalize(it.area) == area
                    )
            }
            .distinctBy {
                Pair(
                    normalize(it.provider),
                    it.providerId.trim()
                )
            }
            .toList()

        return when (matches.size) {
            0 -> Resolution.NotFound
            1 -> Resolution.Exact(matches.single())
            else -> Resolution.Ambiguous(matches)
        }
    }
}