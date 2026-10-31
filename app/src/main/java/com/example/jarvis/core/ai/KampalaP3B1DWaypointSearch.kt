package com.example.jarvis.core.ai

import com.example.jarvis.core.network.KampalaGeoAreaResolver
import com.example.jarvis.core.network.KampalaGeoDiscoveryContract
import com.example.jarvis.core.network.KampalaGeoDiscoveryGateway
import com.example.jarvis.core.network.KampalaMultiStopGeoResolver
import com.example.jarvis.core.network.KampalaP3B1CWaypointReport
import kotlinx.coroutines.CancellationException

/**
 * P3-B1D:
 * Explicit bounded multi-stop geographic discovery.
 *
 * Example:
 * map journey in Central Kampala: from Nsambya via Ntinda to Nakasero
 *
 * Only registered geographic areas are accepted.
 * Staging is deliberately separate from production.
 * No Maps handoff, directions, routing or ETA.
 */
internal class KampalaP3B1DWaypointSearch(
    private val search: suspend (
        KampalaGeoDiscoveryContract.SearchRequest
    ) -> KampalaGeoDiscoveryContract.Result
) {

    suspend fun answerOrNull(message: String): String? {
        val match = Regex(
            """^map journey in (.+?)\s*[:,]\s*(from .+)$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(message.trim()) ?: return null

        val areaName = match.groupValues[1].trim()
        val journeyText = match.groupValues[2].trim()

        val area = KampalaGeoAreaResolver.DEFAULT.resolve(areaName)

        if (area == null) {
            return "This geographic search area is not registered: " +
                areaName.take(100) +
                ". No place search or route was performed. " +
                "Choose a supported geographic search area."
        }

        val journey = KampalaJourneyIntentResolver.resolve(journeyText)

        if (journey == null || journey.stops.isEmpty()) {
            return "I could not recognize a complete multi-stop " +
                "journey. Use: map journey in ${area.label}: " +
                "from [origin] via [stop] to [destination]. " +
                "No geographic search or route was performed."
        }

        val result = try {
            KampalaMultiStopGeoResolver(search)
                .resolve(journey, area.bounds)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return "The geographic waypoint search could not " +
                "be completed. No route or location was confirmed."
        }

        return buildString {
            appendLine("Experimental geographic waypoint search")
            appendLine("Area: ${area.label}")
            appendLine("Bounds source: ${area.provenance}")
            appendLine(
                "Coverage: approximate registered search area, " +
                    "not live GPS or a verified neighborhood boundary."
            )
            appendLine()
            append(
                KampalaP3B1CWaypointReport.format(result)
            )
        }
    }

    companion object {
        fun createStaging(): KampalaP3B1DWaypointSearch {
            val gateway = KampalaGeoDiscoveryGateway.createBackend(
                "https://jarvis-uganda-geo-staging.onrender.com"
            )

            return KampalaP3B1DWaypointSearch(gateway::search)
        }
    }
}