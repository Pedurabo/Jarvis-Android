package com.example.jarvis.core.ai

import com.example.jarvis.core.network.KampalaNamedDestinationRoute
import com.example.jarvis.core.network.KampalaPlacesQueryRoute

/**
 * Central decision point for Kampala place discovery.
 *
 * This component does not fetch network data, execute phone actions,
 * invent locations, make rankings, book services or process payments.
 *
 * Verified search requests are delegated to the existing Places repository.
 */
internal object KampalaDiscoveryIntelligence {

    sealed class Decision {
        data class Search(
            val request: KampalaPlacesQueryRoute.SearchRequest
        ) : Decision()

        data class Notice(val message: String) : Decision()

        object Pass : Decision()
    }

    private val actionPrefix = Regex(
        """^(?:open|launch|call|dial|send|text|message|navigate|set|turn\s+on|turn\s+off|book|reserve|order|buy|purchase|hire)\b""",
        RegexOption.IGNORE_CASE
    )

    private val discoveryPrefix = Regex(
        """^(?:what\s+are|which\s+are|where\s+can\s+i\s+find|tell\s+me\s+about|find|show(?:\s+me)?|search\s+for|list|recommend|suggest|name|give\s+me(?:\s+a\s+list\s+of)?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val supportedAreas = linkedMapOf(
        "old kampala" to "Old Kampala",
        "kisementi" to "Kisementi",
        "kololo" to "Kololo",
        "nakasero" to "Nakasero",
        "ntinda" to "Ntinda",
        "bugolobi" to "Bugolobi",
        "makindye" to "Makindye"
    )

    private val categoryPatterns = linkedMapOf(
        "restaurant" to Regex(
            """\b(restaurants?|food\s+points?|food\s+places?|places\s+to\s+eat|eateries|dining\s+places?)\b"""
        ),
        "accommodation" to Regex(
            """\b(hotels?|guesthouses?|lodges?)\b"""
        ),
        "shopping" to Regex(
            """\b(shopping\s+(?:places?|centres?|centers?|malls?)|malls?|shops?)\b"""
        ),
        "healthcare" to Regex(
            """\b(hospitals?|clinics?)\b"""
        ),
        "market" to Regex(
            """\b(markets?)\b"""
        ),
        "supermarket" to Regex(
            """\b(supermarkets?)\b"""
        ),
        "museum" to Regex(
            """\b(museums?)\b"""
        ),
        "park" to Regex(
            """\b(parks?)\b"""
        ),
        "embassy" to Regex(
            """\b(embassies|embassy|consulates?)\b"""
        ),
        "government" to Regex(
            """\b(government\s+offices?)\b"""
        ),
        "church" to Regex(
            """\b(churches?|cathedrals?)\b"""
        )
    )

    private val cinemaPattern = Regex(
        """\b(cinemas?|cinema\s+halls?|movie\s+theat(?:ers?|res?))\b"""
    )

    private val sportPattern = Regex(
        """\b(football\s+grounds?|football\s+pitches|playgrounds?|sports\s+grounds?|stadiums?)\b"""
    )

    private val roadPattern = Regex(
        """\b(roads?|road\s+networks?)\b"""
    )

    private val rankingWords = Regex(
        """\b(best|top|famous|popular|recommended)\b"""
    )

    fun decide(input: String): Decision {
        val text = input.trim().replace(Regex("""\s+"""), " ")

        if (text.isBlank()) return Decision.Pass
        if (actionPrefix.containsMatchIn(text)) return Decision.Pass

        // Preserve established Places routing and named destinations.
        KampalaPlacesQueryRoute.resolve(text)?.let {
            return Decision.Search(it)
        }

        KampalaNamedDestinationRoute.resolve(text)?.let {
            return Decision.Search(it)
        }

        val normalized = text.lowercase()
            .trimEnd('.', '!', '?')
            .trim()

        val shortSportsRequest =
            normalized in setOf(
                "football grounds",
                "football ground",
                "football pitches",
                "playgrounds",
                "sports grounds"
            )

        val isDiscovery =
            discoveryPrefix.containsMatchIn(normalized) ||
                shortSportsRequest

        if (isDiscovery) {
            // Do not silently reinterpret a request for a different city
            // or an unknown location as a general Kampala search.
            val explicitLocation = Regex(
                """\b(?:in|near|around)\s+([a-z][a-z\s-]*)$"""
            ).find(normalized)?.groupValues?.get(1)?.trim()

            if (explicitLocation != null) {
                val validLocations =
                    supportedAreas.keys + "kampala"

                if (explicitLocation !in validLocations) {
                    return Decision.Pass
                }
            }

            val area = explicitLocation?.let { supportedAreas[it] }

            val matchedCategories = categoryPatterns.filter { (_, pattern) ->
                pattern.containsMatchIn(normalized)
            }.keys

            if (matchedCategories.size == 1) {
                return Decision.Search(
                    KampalaPlacesQueryRoute.SearchRequest(
                        category = matchedCategories.single(),
                        area = area
                    )
                )
            }

            if (matchedCategories.isEmpty()) {
                if (cinemaPattern.containsMatchIn(normalized)) {
                    return Decision.Notice(
                        "I don't yet have sufficiently verified cinema " +
                            "listings in my Kampala directory. I can't " +
                            "reliably rank cinema halls or confirm current " +
                            "prices, facilities or showtimes. Please " +
                            "check official cinema information."
                    )
                }

                if (sportPattern.containsMatchIn(normalized)) {
                    return Decision.Notice(
                        "My current Kampala directory does not yet " +
                            "contain a reliable list of football grounds " +
                            "and sports facilities. This does not mean " +
                            "they don't exist. I won't invent names, " +
                            "addresses or availability."
                    )
                }

                if (roadPattern.containsMatchIn(normalized)) {
                    return Decision.Notice(
                        "I don't yet have an authoritative Kampala " +
                            "road-network dataset. I can't reliably list " +
                            "roads, road connections or current conditions. " +
                            "Please consult a trusted map or official " +
                            "transport information."
                    )
                }
            }
        }

        // Compatibility with existing safeguarded phrases.
        KampalaDiscoveryCoveragePolicy.searchRequest(text)?.let {
            return Decision.Search(it)
        }

        KampalaDiscoveryCoveragePolicy.notice(text)?.let {
            return Decision.Notice(it)
        }

        return Decision.Pass
    }
}
