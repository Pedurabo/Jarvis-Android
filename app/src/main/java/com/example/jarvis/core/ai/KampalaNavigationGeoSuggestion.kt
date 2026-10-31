package com.example.jarvis.core.ai

import com.example.jarvis.core.network.KampalaGeoAreaResolver
import com.example.jarvis.core.network.KampalaGeoDiscoveryContract
import com.example.jarvis.core.network.KampalaGeoDiscoveryGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Navigation-facing geographic result interpretation.
 *
 * Results are public provider listings, not independent
 * verification of physical addresses or calculated routes.
 *
 * This class never selects a destination or launches Maps.
 */
internal class KampalaNavigationGeoSuggestion(
    private val gateway: KampalaGeoDiscoveryGateway
) {
    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .removePrefix("the ")
            .trim()

    private fun usableNameMatch(
        requested: String,
        candidate: String
    ): Boolean {
        val query = normalize(requested)
        val name = normalize(candidate)

        if (query.isBlank() || name.isBlank()) return false

        if (query == name) return true

        // Do not use substring matching for short acronyms.
        if (query.length < 5) return false

        return name.startsWith("$query ") ||
            name.endsWith(" $query")
    }

    suspend fun suggest(destination: String): String? {
        val query = destination.trim()

        if (query.length !in 2..120) return null

        val area = KampalaGeoAreaResolver.DEFAULT
            .resolve("Central Kampala")
            ?: return "Search area unavailable."

        val request = KampalaGeoDiscoveryContract.SearchRequest(
            query = query,
            bounds = area.bounds,
            pageSize = 20
        )

        if (!KampalaGeoDiscoveryContract.validRequest(request)) {
            return "Search request could not be validated."
        }

        val result = try {
            val raw = withTimeoutOrNull(7000L) {
                gateway.search(request)
            } ?: return "Map provider did not respond in time."

            KampalaGeoDiscoveryContract.validate(request, raw)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return "Map provider could not be reached."
        }

        when (result) {
            is KampalaGeoDiscoveryContract.Result.Unavailable -> {
                return if (
                    result.kind ==
                    KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE
                ) {
                    "Map-provider coverage is incomplete."
                } else {
                    "Map-provider results are currently unavailable."
                }
            }

            is KampalaGeoDiscoveryContract.Result.Success -> {
                val page = result.page

                if (page.nextPageToken != null) {
                    return "More map listings may exist. " +
                        "The search is incomplete; no destination " +
                        "has been selected."
                }

                val matching = page.places.filter {
                    usableNameMatch(query, it.name)
                }.distinctBy { it.providerId }

                if (matching.isEmpty()) {
                    return "No close name match in the current " +
                        "Central Kampala map search. " +
                        "This does not mean the place is absent."
                }

                fun clean(value: String): String =
                    value.replace('\n', ' ')
                        .replace('\r', ' ')
                        .trim()

                if (matching.size > 1) {
                    val names = matching
                        .take(3)
                        .map { clean(it.name).take(65) }

                    return "Multiple possible public listings: " +
                        names.joinToString("; ") +
                        ". Confirm the intended place in Google Maps."
                }

                val place = matching.single()

                val name = clean(place.name).take(100)
                val areaName = place.area
                    ?.let(::clean)
                    ?.take(70)
                    .orEmpty()

                val source = clean(place.sourceUrl).take(250)

                return buildString {
                    append("Possible public map listing: ")
                    append(name)

                    if (areaName.isNotBlank()) {
                        append(", ")
                        append(areaName)
                    }

                    append(". Source: ")
                    append(source)
                    append(". Not independently verified.")
                }
            }
        }
    }
}