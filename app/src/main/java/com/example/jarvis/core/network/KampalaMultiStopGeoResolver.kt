package com.example.jarvis.core.network

import com.example.jarvis.core.ai.KampalaJourneyIntentResolver

/**
 * P3-A2B: Provider-backed resolution of ordered journey stops.
 *
 * Coordinates here are provider-listed, NOT independently verified
 * entrances, school campuses, or road-routing waypoints.
 *
 * This component never calculates directions or travel times.
 * A caller must explicitly supply geographic search bounds.
 */
internal class KampalaMultiStopGeoResolver(
    private val search: suspend (
        KampalaGeoDiscoveryContract.SearchRequest
    ) -> KampalaGeoDiscoveryContract.Result
) {

    enum class Role {
        ORIGIN,
        STOP,
        DESTINATION
    }

    sealed class Finding {
        data class UniqueListing(
            val place: KampalaGeoDiscoveryContract.Place,
            val provider: String
        ) : Finding()

        data class Ambiguous(
            val candidates: List<KampalaGeoDiscoveryContract.Place>
        ) : Finding()

        object NotFound : Finding()
        object Incomplete : Finding()

        data class Unavailable(
            val reason: String
        ) : Finding()
    }

    data class Waypoint(
        val role: Role,
        val requestedName: String,
        val finding: Finding
    )

    data class JourneyResult(
        val waypoints: List<Waypoint>
    ) {
        val allHaveUniqueProviderListings: Boolean
            get() = waypoints.isNotEmpty() &&
                waypoints.all { it.finding is Finding.UniqueListing }

        // Provider-listed coordinates alone cannot authorize
        // route calculation or an unconfirmed Maps handoff.
        val readyForRouting: Boolean
            get() = false
    }

    suspend fun resolve(
        journey: KampalaJourneyIntentResolver.Journey,
        bounds: KampalaGeoDiscoveryContract.Bounds
    ): JourneyResult {

        val names = buildList {
            add(Role.ORIGIN to journey.origin)

            journey.stops.forEach { stop ->
                add(Role.STOP to stop)
            }

            add(Role.DESTINATION to journey.destination)
        }

        if (!KampalaGeoDiscoveryContract.validBounds(bounds)) {
            return JourneyResult(
                names.map { (role, name) ->
                    Waypoint(
                        role,
                        name,
                        Finding.Unavailable("Invalid search bounds.")
                    )
                }
            )
        }

        val waypoints = mutableListOf<Waypoint>()

        for ((role, name) in names) {
            val request = KampalaGeoDiscoveryContract.SearchRequest(
                query = name,
                bounds = bounds,
                pageSize = 50
            )

            if (!KampalaGeoDiscoveryContract.validRequest(request)) {
                waypoints += Waypoint(
                    role,
                    name,
                    Finding.Unavailable("Invalid place search request.")
                )
                continue
            }

            val rawResult = try {
                search(request)
            } catch (_: Exception) {
                KampalaGeoDiscoveryContract.Result.Unavailable(
                    "The geographic provider request failed."
                )
            }

            val result = KampalaGeoDiscoveryContract.validate(
                request,
                rawResult
            )

            val finding = when (result) {
                is KampalaGeoDiscoveryContract.Result.Unavailable -> {
                    if (
                        result.kind ==
                            KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE
                    ) {
                        Finding.Incomplete
                    } else {
                        Finding.Unavailable(result.reason)
                    }
                }

                is KampalaGeoDiscoveryContract.Result.Success -> {
                    val page = result.page

                    // A truncated page cannot establish uniqueness.
                    // Even an exact match may have more candidates.
                    if (page.nextPageToken != null) {
                        Finding.Incomplete
                    } else {
                        val requested = name.trim()
                            .replace(Regex("""\s+"""), " ")
                            .lowercase()

                        val matching = page.places.filter { place ->
                            place.name.trim()
                                .replace(Regex("""\s+"""), " ")
                                .lowercase() == requested
                        }

                        when (matching.size) {
                            0 -> Finding.NotFound

                            1 -> Finding.UniqueListing(
                                place = matching.single(),
                                provider = page.provider
                            )

                            else -> Finding.Ambiguous(matching)
                        }
                    }
                }
            }

            waypoints += Waypoint(role, name, finding)
        }

        return JourneyResult(waypoints)
    }

    companion object {
        /**
         * Uses the application's existing gateway.
         * No network call occurs until resolve() is invoked.
         */
        fun fromGateway(
            gateway: KampalaGeoDiscoveryGateway
        ): KampalaMultiStopGeoResolver =
            KampalaMultiStopGeoResolver(gateway::search)
    }
}