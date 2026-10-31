package com.example.jarvis.core.network

import com.example.jarvis.core.ai.KampalaJourneyIntentResolver

/**
 * P3-B1C: Converts geographic waypoint findings into a
 * conservative, user-readable journey report.
 *
 * Bounds MUST come from an explicitly established search area.
 *
 * A map listing is not proof of a valid entrance, campus,
 * driving route, road distance or travel time.
 */
internal object KampalaP3B1CWaypointReport {

    suspend fun resolve(
        journey: KampalaJourneyIntentResolver.Journey,
        bounds: KampalaGeoDiscoveryContract.Bounds,
        gateway: KampalaGeoDiscoveryGateway
    ): String {
        val resolver = KampalaMultiStopGeoResolver.fromGateway(gateway)
        val result = resolver.resolve(journey, bounds)

        return format(result)
    }

    fun format(
        result: KampalaMultiStopGeoResolver.JourneyResult
    ): String = buildString {
        appendLine("Your requested journey")
        appendLine()

        result.waypoints.forEachIndexed { index, waypoint ->
            val label = when (waypoint.role) {
                KampalaMultiStopGeoResolver.Role.ORIGIN ->
                    "From"

                KampalaMultiStopGeoResolver.Role.STOP ->
                    "Stop ${result.waypoints.take(index + 1).count {
                        it.role == KampalaMultiStopGeoResolver.Role.STOP
                    }}"

                KampalaMultiStopGeoResolver.Role.DESTINATION ->
                    "To"
            }

            appendLine("$label: ${waypoint.requestedName}")

            when (val finding = waypoint.finding) {
                is KampalaMultiStopGeoResolver.Finding.UniqueListing -> {
                    appendLine("  Map listing: ${finding.place.name}")
                    appendLine("  Source: ${finding.place.sourceUrl}")
                    appendLine(
                        "  Status: public map listing only; " +
                            "exact entrance not confirmed"
                    )
                }

                is KampalaMultiStopGeoResolver.Finding.Ambiguous -> {
                    appendLine(
                        "  Status: multiple matching listings; " +
                            "please clarify the location"
                    )
                    appendLine(
                        "  Matching listings: ${finding.candidates.size}"
                    )
                }

                KampalaMultiStopGeoResolver.Finding.NotFound -> {
                    appendLine(
                        "  Status: no exact listing found " +
                            "within the searched area"
                    )
                }

                KampalaMultiStopGeoResolver.Finding.Incomplete -> {
                    appendLine(
                        "  Status: search incomplete; " +
                            "no location selected"
                    )
                }

                is KampalaMultiStopGeoResolver.Finding.Unavailable -> {
                    appendLine(
                        "  Status: geographic information unavailable"
                    )
                }
            }

            appendLine()
        }

        appendLine(
            "These results describe public map listings, " +
                "not independently verified road entrances."
        )

        appendLine(
            "A location missing from the searched area may " +
                "exist elsewhere."
        )

        appendLine(
            "No route, distance, travel time or navigation " +
                "handoff has been calculated."
        )

        if (!result.readyForRouting) {
            append(
                "Please confirm any unresolved locations " +
                    "and their exact entrances before travelling."
            )
        }
    }
}