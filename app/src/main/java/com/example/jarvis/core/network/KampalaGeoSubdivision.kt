package com.example.jarvis.core.network

/**
 * D5K-D12-B.
 *
 * Bounded subdivision of provider-backed map searches.
 *
 * Never converts incomplete coverage into a successful result.
 * Never uses GPS, falls back to another provider, or invents places.
 */
internal object KampalaGeoSubdivision {

    private const val MAX_DEPTH = 3
    private const val MAX_REQUESTS = 100

    private data class Tile(
        val bounds: KampalaGeoDiscoveryContract.Bounds,
        val depth: Int
    )

    private fun split(
        b: KampalaGeoDiscoveryContract.Bounds
    ): List<KampalaGeoDiscoveryContract.Bounds> {
        val midLatitude = (b.south + b.north) / 2.0
        val midLongitude = (b.west + b.east) / 2.0

        return listOf(
            KampalaGeoDiscoveryContract.Bounds(
                b.south, b.west, midLatitude, midLongitude
            ),
            KampalaGeoDiscoveryContract.Bounds(
                b.south, midLongitude, midLatitude, b.east
            ),
            KampalaGeoDiscoveryContract.Bounds(
                midLatitude, b.west, b.north, midLongitude
            ),
            KampalaGeoDiscoveryContract.Bounds(
                midLatitude, midLongitude, b.north, b.east
            )
        )
    }

    suspend fun search(
        provider: KampalaGeoDiscoveryContract.Provider,
        request: KampalaGeoDiscoveryContract.SearchRequest
    ): KampalaGeoDiscoveryContract.Result {
        if (!KampalaGeoDiscoveryContract.validRequest(request)) {
            return KampalaGeoDiscoveryContract.Result.Unavailable(
                "Invalid geographic discovery request."
            )
        }

        val pending = java.util.ArrayDeque<Tile>()
        pending.addLast(Tile(request.bounds, 0))

        val collected =
            linkedMapOf<String, KampalaGeoDiscoveryContract.Place>()

        var requests = 0
        var providerName: String? = null

        while (!pending.isEmpty()) {
            if (requests >= MAX_REQUESTS) {
                return KampalaGeoDiscoveryContract.Result.Unavailable(
                    "The geographic search exceeded its safe request limit. " +
                        "Please narrow the search area."
                )
            }

            val tile = pending.removeFirst()
            val tileRequest = request.copy(
                bounds = tile.bounds,
                pageToken = null
            )

            requests++

            val raw = try {
                provider.search(tileRequest)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return KampalaGeoDiscoveryContract.Result.Unavailable(
                    "The map service could not complete every search area."
                )
            }

            val result = KampalaGeoDiscoveryContract.validate(
                tileRequest,
                raw
            )

            when (result) {
                is KampalaGeoDiscoveryContract.Result.Success -> {
                    val page = result.page

                    if (page.nextPageToken != null) {
                        return KampalaGeoDiscoveryContract.Result.Unavailable(
                            "The map service returned unprocessed pagination."
                        )
                    }

                    if (
                        providerName != null &&
                        providerName != page.provider
                    ) {
                        return KampalaGeoDiscoveryContract.Result.Unavailable(
                            "Map search results came from inconsistent providers."
                        )
                    }

                    providerName = page.provider

                    for (place in page.places) {
                        val previous = collected[place.providerId]

                        if (previous != null && previous != place) {
                            return KampalaGeoDiscoveryContract.Result.Unavailable(
                                "Overlapping map records were inconsistent."
                            )
                        }

                        collected[place.providerId] = place
                    }
                }

                is KampalaGeoDiscoveryContract.Result.Unavailable -> {
                    // The current HTTPS adapter uses "incomplete" for
                    // a non-SUCCESS response, including the backend's
                    // INCOMPLETE status. Never subdivide ordinary
                    // connectivity failures or invalid requests.
                    val maySubdivide = result.kind == KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE

                    if (!maySubdivide) {
                        return result
                    }

                    if (tile.depth >= MAX_DEPTH) {
                        return KampalaGeoDiscoveryContract.Result.Unavailable(
                            "Map listing coverage remains incomplete " +
                                "after safe geographic subdivision. " +
                                "Please narrow the search area."
                        )
                    }

                    for (child in split(tile.bounds)) {
                        pending.addLast(Tile(child, tile.depth + 1))
                    }
                }
            }
        }

        val providerLabel = providerName
            ?: return KampalaGeoDiscoveryContract.Result.Unavailable(
                "The geographic search returned no completed tiles."
            )

        // Every tile was completed and validated before this point.
        // The existing presentation layer intentionally displays
        // no more than the original page-size limit.
        val displayPlaces = collected.values
            .sortedWith(
                compareBy<KampalaGeoDiscoveryContract.Place> {
                    it.name.lowercase()
                }.thenBy { it.providerId }
            )
            .take(request.pageSize)

        return KampalaGeoDiscoveryContract.validate(
            request,
            KampalaGeoDiscoveryContract.Result.Success(
                KampalaGeoDiscoveryContract.Page(
                    provider = if (collected.size > displayPlaces.size) {
                        "$providerLabel (showing ${displayPlaces.size} " +
                            "of ${collected.size} map listings)"
                    } else {
                        providerLabel
                    },
                    places = displayPlaces,
                    nextPageToken = null
                )
            )
        )
    }
}