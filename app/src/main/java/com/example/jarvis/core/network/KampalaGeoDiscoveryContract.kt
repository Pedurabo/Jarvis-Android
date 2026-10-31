package com.example.jarvis.core.network

import java.net.URI

/**
 * CDI.6.0-A6.9-D1
 *
 * Provider-neutral geographic place discovery foundation.
 *
 * Does not acquire GPS location, contact a provider,
 * verify business ownership, or calculate routes.
 */
internal object KampalaGeoDiscoveryContract {

    data class Coordinates(
        val latitude: Double,
        val longitude: Double
    )

    data class Bounds(
        val south: Double,
        val west: Double,
        val north: Double,
        val east: Double
    )

    data class SearchRequest(
        val query: String? = null,
        val category: String? = null,
        val bounds: Bounds,
        val pageSize: Int = 20,
        val pageToken: String? = null
    )

    data class Place(
        val providerId: String,
        val name: String,
        val category: String,
        val coordinates: Coordinates,
        val publicAddress: String?,
        val area: String?,
        val sourceUrl: String,
        val isPublicListing: Boolean,
        // JARVIS-OSM-RECORD-DATES-V1
        // Genuine source-element edit time, when supplied.
        val osmLastEditedAt: String? = null
    )

    data class Page(
        val provider: String,
        val places: List<Place>,
        val nextPageToken: String?
    )

    sealed class Result {
        data class Success(
            val page: Page
        ) : Result()

        enum class Kind {
            OTHER,
            INCOMPLETE
        }

        data class Unavailable(
            val reason: String,
            val kind: Kind = Kind.OTHER
        ) : Result()
    }

    /**
     * An implementation may use an approved external
     * mapping/places provider or a trusted local dataset.
     *
     * Provider errors must not be converted into an
     * invented empty successful response.
     */
    interface Provider {
        suspend fun search(
            request: SearchRequest
        ): Result
    }

    fun validCoordinates(
        coordinates: Coordinates
    ): Boolean =
        coordinates.latitude.isFinite() &&
            coordinates.longitude.isFinite() &&
            coordinates.latitude in -90.0..90.0 &&
            coordinates.longitude in -180.0..180.0

    /**
     * D1 deliberately supports ordinary non-wrapping bounds.
     * Antimeridian-crossing boxes require a future extension.
     */
    fun validBounds(bounds: Bounds): Boolean =
        validCoordinates(
            Coordinates(bounds.south, bounds.west)
        ) &&
            validCoordinates(
                Coordinates(bounds.north, bounds.east)
            ) &&
            bounds.south < bounds.north &&
            bounds.west < bounds.east

    fun validRequest(request: SearchRequest): Boolean {
        val query = request.query?.trim().orEmpty()
        val category = request.category?.trim().orEmpty()

        val hasSearchCriterion =
            query.isNotEmpty() || category.isNotEmpty()

        val queryValid =
            query.length <= 120 &&
                !query.any { it.isISOControl() }

        val categoryValid =
            category.length <= 80 &&
                !category.any { it.isISOControl() }

        val tokenValid =
            request.pageToken == null ||
                (
                    request.pageToken.length in 1..512 &&
                        !request.pageToken.any {
                            it.isWhitespace() || it.isISOControl()
                        }
                )

        return validBounds(request.bounds) &&
            hasSearchCriterion &&
            queryValid &&
            categoryValid &&
            request.pageSize in 1..50 &&
            tokenValid
    }

    fun withinBounds(
        coordinates: Coordinates,
        bounds: Bounds
    ): Boolean =
        validCoordinates(coordinates) &&
            validBounds(bounds) &&
            coordinates.latitude in bounds.south..bounds.north &&
            coordinates.longitude in bounds.west..bounds.east

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

    fun validPlace(place: Place): Boolean =
        place.providerId.isNotBlank() &&
            place.name.isNotBlank() &&
            place.category.isNotBlank() &&
            validCoordinates(place.coordinates) &&
            validSource(place.sourceUrl) &&
            place.publicAddress?.length?.let {
                it <= 250
            } != false &&
            place.area?.length?.let {
                it <= 120
            } != false &&
            place.isPublicListing

    /**
     * Successful provider responses are accepted only when
     * structurally valid and within the requested bounds.
     *
     * The result remains provider-listed information,
     * NOT independent verification of a real-world place.
     */
    fun validate(
        request: SearchRequest,
        result: Result
    ): Result {
        if (!validRequest(request)) {
            return Result.Unavailable(
                "Invalid geographic discovery request."
            )
        }

        if (result is Result.Unavailable) return result

        val page = (result as Result.Success).page

        if (page.provider.isBlank()) {
            return Result.Unavailable(
                "The places provider did not identify itself."
            )
        }

        if (page.places.size > request.pageSize) {
            return Result.Unavailable(
                "The places provider returned too many records."
            )
        }

        if (
            page.nextPageToken != null &&
            (
                page.nextPageToken.length !in 1..512 ||
                    page.nextPageToken.any {
                        it.isWhitespace() || it.isISOControl()
                    } ||
                    page.nextPageToken == request.pageToken
            )
        ) {
            return Result.Unavailable(
                "The places provider returned invalid pagination."
            )
        }

        if (page.places.any {
                !validPlace(it) ||
                    !withinBounds(it.coordinates, request.bounds)
            }
        ) {
            return Result.Unavailable(
                "The places provider returned invalid location data."
            )
        }

        if (
            page.places
                .map { it.providerId.trim() }
                .distinct()
                .size != page.places.size
        ) {
            return Result.Unavailable(
                "The places provider returned duplicate place IDs."
            )
        }

        return result
    }
}