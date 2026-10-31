package com.example.jarvis.core.ai

/**
 * P3-A2A: Classifies directory evidence for an ordered journey.
 *
 * No coordinates are inferred. No network requests or routing
 * calculations occur. An exact directory listing is NOT
 * automatically a geographically verified waypoint.
 */
internal object KampalaMultiStopResolutionPolicy {

    enum class Role {
        ORIGIN,
        STOP,
        DESTINATION
    }

    data class DirectoryEvidence(
        val available: Boolean,
        val entries: List<Pair<String, String>>
    )

    sealed class Status {
        data class ExactListing(
            val name: String,
            val description: String
        ) : Status()

        data class Ambiguous(
            val count: Int
        ) : Status()

        data class TooManyMatches(
            val count: Int
        ) : Status()

        object Unverified : Status()
        object DirectoryUnavailable : Status()
    }

    data class PlaceDecision(
        val role: Role,
        val requestedName: String,
        val status: Status
    )

    data class JourneyDecision(
        val places: List<PlaceDecision>
    ) {
        val allHaveUniqueDirectoryListings: Boolean
            get() = places.isNotEmpty() &&
                places.all { it.status is Status.ExactListing }

        // A unique textual listing does not establish precise
        // coordinates, entrance, campus, or a road route.
        val readyForRouting: Boolean
            get() = false
    }

    fun resolve(
        journey: KampalaJourneyIntentResolver.Journey,
        evidenceFor: (String) -> DirectoryEvidence
    ): JourneyDecision {
        val ordered = buildList {
            add(Role.ORIGIN to journey.origin)

            journey.stops.forEach { name ->
                add(Role.STOP to name)
            }

            add(Role.DESTINATION to journey.destination)
        }

        return JourneyDecision(
            places = ordered.map { (role, requestedName) ->
                val evidence = evidenceFor(requestedName)

                val confidence =
                    KampalaDestinationConfidencePolicy.resolve(
                        requestedName = requestedName,
                        entries = evidence.entries,
                        directoryAvailable = evidence.available
                    )

                val status = when (confidence) {
                    is KampalaDestinationConfidencePolicy.Resolution.ExactListing -> {
                        val entry = evidence.entries[confidence.index]
                        Status.ExactListing(
                            name = entry.first,
                            description = entry.second
                        )
                    }

                    is KampalaDestinationConfidencePolicy.Resolution.Ambiguous ->
                        Status.Ambiguous(confidence.indices.size)

                    is KampalaDestinationConfidencePolicy.Resolution.TooManyMatches ->
                        Status.TooManyMatches(confidence.count)

                    KampalaDestinationConfidencePolicy.Resolution.Unverified ->
                        Status.Unverified

                    KampalaDestinationConfidencePolicy.Resolution.DirectoryUnavailable ->
                        Status.DirectoryUnavailable
                }

                PlaceDecision(
                    role = role,
                    requestedName = requestedName,
                    status = status
                )
            }
        )
    }
}