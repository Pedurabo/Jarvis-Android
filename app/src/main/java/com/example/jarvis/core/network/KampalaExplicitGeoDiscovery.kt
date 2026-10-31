package com.example.jarvis.core.network

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * CDI.6.0-A6.9-D4B
 *
 * An experimental, explicit opt-in geographic query.
 *
 * Only the documented commands are recognized.
 * No ordinary chat message can accidentally match.
 *
 * No GPS access, background polling or route calculation.
 *
 * Constructing this handler does not contact the network.
 */
internal class KampalaExplicitGeoDiscovery(
    private val provider: KampalaGeoDiscoveryContract.Provider
) {

    // JARVIS-GEO-FOLLOWUPS-V1
    // In-memory search scope, not location or GPS history.
    @Volatile
    private var lastMultiAreaSearch = false

    internal data class Command(
        val category: String,
        val bounds: KampalaGeoDiscoveryContract.Bounds,
        val label: String
    ) {
        fun toRequest() =
            KampalaGeoDiscoveryContract.SearchRequest(
                query = null,
                category = category,
                bounds = bounds,
                pageSize = 10,
                pageToken = null
            )
    }

    companion object {

        /**
         * Development-only geographic area, not device GPS.
         * These bounds are approximate and intentionally small.
         */
        /**
         * Parse a category and geographic area independently.
         * Only registered areas can produce search requests.
         */
        fun parseOrNull(message: String): Command? =
            parseWithResolver(
                message,
                KampalaGeoAreaResolver.DEFAULT
            )

        internal fun parseWithResolver(
            message: String,
            resolver: KampalaGeoAreaResolver
        ): Command? {
            val normalized = message
                .trim()
                .lowercase(java.util.Locale.ROOT)
                .replace(Regex("""\s+"""), " ")
                .trimEnd('?', '.', '!')
                .trim()

            val match = Regex(
                """^(?:map search|find(?: me)?|show me|show|are there) (.+?) in (.+)$"""
            ).matchEntire(normalized) ?: return null

            val categoryPhrase = match.groupValues[1].trim()
            val areaPhrase = match.groupValues[2].trim()

            val categories = mapOf(
                "hospitals" to "hospital",
                "clinics" to "clinic",
                "pharmacies" to "pharmacy",
                "schools" to "school",
                "universities" to "university",
                "banks" to "bank",
                "fuel stations" to "fuel_station",
                "hotels" to "hotel",
                "guesthouses" to "guest_house",
                "guest houses" to "guest_house",
                "police stations" to "police",
                "embassies" to "embassy",
                // JARVIS-GEO27-INTEGRATION-V5
                "apartment" to "apartment",
                "apartments" to "apartment",
                "attraction" to "attraction",
                "attractions" to "attraction",
                "bar" to "bar",
                "bars" to "bar",
                "cafe" to "cafe",
                "cafes" to "cafe",
                "café" to "cafe",
                "cafés" to "cafe",
                "convenience shop" to "convenience_shop",
                "convenience shops" to "convenience_shop",
                "convenience store" to "convenience_shop",
                "convenience stores" to "convenience_shop",
                "department store" to "department_store",
                "department stores" to "department_store",
                "fast food" to "fast_food",
                "fitness centre" to "fitness_centre",
                "fitness centres" to "fitness_centre",
                "fitness center" to "fitness_centre",
                "fitness centers" to "fitness_centre",
                "hostel" to "hostel",
                "hostels" to "hostel",
                "motel" to "motel",
                "motels" to "motel",
                "nightclub" to "nightclub",
                "nightclubs" to "nightclub",
                "night club" to "nightclub",
                "night clubs" to "nightclub",
                "pub" to "pub",
                "pubs" to "pub",
                "restaurant" to "restaurant",
                "restaurants" to "restaurant",
                "shopping mall" to "shopping_mall",
                "shopping malls" to "shopping_mall",
                "sports centre" to "sports_centre",
                "sports centres" to "sports_centre",
                "sports center" to "sports_centre",
                "sports centers" to "sports_centre",
                "supermarket" to "supermarket",
                "supermarkets" to "supermarket"
            )

            val category = categories[categoryPhrase]
                ?: return null

            val area = resolver.resolve(areaPhrase)
                ?: return null

            return Command(
                category = category,
                bounds = area.bounds,
                label = area.label
            )
        }

        /**
         * Deliberately explicit construction.
         * Caller must decide when an external search is allowed.
         */
                /**
         * D5K-C: explicit backend composition.
         *
         * A missing deployment never falls back to Overpass.
         * No network request occurs during construction.
         */
        fun createConfiguredBackend(): KampalaExplicitGeoDiscovery {
            val origin = KampalaGeoBackendConfig.HTTPS_ORIGIN.trim()

            val selected: KampalaGeoDiscoveryContract.Provider =
                if (origin.isBlank()) {
                    object : KampalaGeoDiscoveryContract.Provider {
                        override suspend fun search(
                            request: KampalaGeoDiscoveryContract.SearchRequest
                        ): KampalaGeoDiscoveryContract.Result =
                            KampalaGeoDiscoveryContract.Result.Unavailable(
                                "The Uganda geographic backend is not configured."
                            )
                    }
                } else {
                    try {
                        val gateway =
                            KampalaGeoDiscoveryGateway.createBackend(origin)

                        object : KampalaGeoDiscoveryContract.Provider {
                            override suspend fun search(
                                request: KampalaGeoDiscoveryContract.SearchRequest
                            ): KampalaGeoDiscoveryContract.Result =
                                gateway.search(request)
                        }
                    } catch (_: IllegalArgumentException) {
                        object : KampalaGeoDiscoveryContract.Provider {
                            override suspend fun search(
                                request: KampalaGeoDiscoveryContract.SearchRequest
                            ): KampalaGeoDiscoveryContract.Result =
                                KampalaGeoDiscoveryContract.Result.Unavailable(
                                    "The Uganda geographic backend configuration is invalid."
                                )
                        }
                    } catch (_: java.net.URISyntaxException) {
                        object : KampalaGeoDiscoveryContract.Provider {
                            override suspend fun search(
                                request: KampalaGeoDiscoveryContract.SearchRequest
                            ): KampalaGeoDiscoveryContract.Result =
                                KampalaGeoDiscoveryContract.Result.Unavailable(
                                    "The Uganda geographic backend configuration is invalid."
                                )
                        }
                    }
                }

            return KampalaExplicitGeoDiscovery(selected)
        }
fun createOverpass(): KampalaExplicitGeoDiscovery =
            KampalaExplicitGeoDiscovery(
                KampalaGeoDiscoveryGateway.createOverpass()
                    .let { gateway ->
                        object : KampalaGeoDiscoveryContract.Provider {
                            override suspend fun search(
                                request: KampalaGeoDiscoveryContract.SearchRequest
                            ): KampalaGeoDiscoveryContract.Result =
                                gateway.search(request)
                        }
                    }
            )
    }

    /**
     * Returns null for all unrelated messages.
     * This allows existing assistant routes to remain authoritative.
     */
    // JARVIS-GEO-MULTI-AREA-V1
    // Searches eight experimental rectangles, including Central Kampala.
    // No live GPS, invented coordinates or automatic routing.
    private suspend fun answerAcrossAreasOrNull(
        message: String
    ): String? {
        val normalized = message
            .trim()
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("""\s+"""), " ")
            .trimEnd('?', '.', '!')
            .trim()

        val command = Regex(
            """^(?:map search|find(?: me)?|show me|show) (.+?) (?:across|around|throughout) (?:supported )?kampala(?: areas)?$"""
        ).matchEntire(normalized) ?: return null

        val categories = mapOf(
            "hospitals" to "hospital",
            "clinics" to "clinic",
            "pharmacies" to "pharmacy",
            "schools" to "school",
            "universities" to "university",
            "banks" to "bank",
            "fuel stations" to "fuel_station",
            "hotels" to "hotel",
            "guesthouses" to "guest_house",
            "guest houses" to "guest_house",
            "police stations" to "police",
            "embassies" to "embassy",
            // JARVIS-GEO27-INTEGRATION-V5
            "apartment" to "apartment",
            "apartments" to "apartment",
            "attraction" to "attraction",
            "attractions" to "attraction",
            "bar" to "bar",
            "bars" to "bar",
            "cafe" to "cafe",
            "cafes" to "cafe",
            "café" to "cafe",
            "cafés" to "cafe",
            "convenience shop" to "convenience_shop",
            "convenience shops" to "convenience_shop",
            "convenience store" to "convenience_shop",
            "convenience stores" to "convenience_shop",
            "department store" to "department_store",
            "department stores" to "department_store",
            "fast food" to "fast_food",
            "fitness centre" to "fitness_centre",
            "fitness centres" to "fitness_centre",
            "fitness center" to "fitness_centre",
            "fitness centers" to "fitness_centre",
            "hostel" to "hostel",
            "hostels" to "hostel",
            "motel" to "motel",
            "motels" to "motel",
            "nightclub" to "nightclub",
            "nightclubs" to "nightclub",
            "night club" to "nightclub",
            "night clubs" to "nightclub",
            "pub" to "pub",
            "pubs" to "pub",
            "restaurant" to "restaurant",
            "restaurants" to "restaurant",
            "shopping mall" to "shopping_mall",
            "shopping malls" to "shopping_mall",
            "sports centre" to "sports_centre",
            "sports centres" to "sports_centre",
            "sports center" to "sports_centre",
            "sports centers" to "sports_centre",
            "supermarket" to "supermarket",
            "supermarkets" to "supermarket"
        )

        val requestedCategory =
            command.groupValues[1].trim()

        val category =
            categories[requestedCategory] ?: return null

        // JARVIS-GEO-CENTRAL-AREA-V1
        // Add the existing Central Kampala development rectangle
        // without changing the seven experimental parish areas.
        // This rectangle is approximate, not a KCCA boundary.
        val centralArea =
            KampalaGeoAreaResolver.DEFAULT.resolve(
                "Central Kampala"
            )

        val areas =
            listOfNotNull(centralArea) +
                KampalaGeoP2ApprovedAreas.areas

        if (areas.isEmpty()) {
            return "No experimental Kampala search areas " +
                "are enabled. No provider search was made."
        }

        val found = mutableListOf<
            Pair<
                KampalaGeoAreaResolver.Area,
                KampalaGeoDiscoveryContract.Place
            >
        >()

        val unavailableAreas = mutableListOf<String>()
        val incompleteAreas = mutableListOf<String>()
        val emptyAreas = mutableListOf<String>()

        // JARVIS-GEO-SNAPSHOT-V1
        // Snapshot generation is NOT an individual
        // map record's last-edited or business verification date.
        val snapshotBySource = mutableMapOf<String, String>()

        // Limit concurrency to two geographic requests.
        // The backend retains its existing reliability policy.
        for (batch in areas.chunked(2)) {
            val outcomes = coroutineScope {
                batch.map { area ->
                    async {
                        val request =
                            KampalaGeoDiscoveryContract.SearchRequest(
                                category = category,
                                bounds = area.bounds,
                                pageSize = 20
                            )

                        val result =
                            if (!KampalaGeoDiscoveryContract.validRequest(
                                    request
                                )
                            ) {
                                KampalaGeoDiscoveryContract.Result.Unavailable(
                                    "Invalid geographic search request."
                                )
                            } else {
                                try {
                                    val raw = withTimeoutOrNull(4000L) {
                                        provider.search(request)
                                    }

                                    if (raw == null) {
                                        KampalaGeoDiscoveryContract
                                            .Result.Unavailable(
                                                "Provider request timed out."
                                            )
                                    } else {
                                        KampalaGeoDiscoveryContract.validate(
                                            request,
                                            raw
                                        )
                                    }
                                } catch (
                                    cancelled:
                                        kotlinx.coroutines.CancellationException
                                ) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    KampalaGeoDiscoveryContract
                                        .Result.Unavailable(
                                            "Provider request failed."
                                        )
                                }
                            }

                        area to result
                    }
                }.map { it.await() }
            }

            for ((area, result) in outcomes) {
                when (result) {
                    is KampalaGeoDiscoveryContract.Result.Success -> {
                        if (result.page.places.isEmpty()) {
                            emptyAreas.add(area.label)
                        }

                        if (result.page.nextPageToken != null) {
                            incompleteAreas.add(area.label)
                        }

                        val snapshotCandidate = Regex(
                            """snapshot \(([^)]+)\)"""
                        ).find(result.page.provider)
                            ?.groupValues?.get(1)

                        val validSnapshot =
                            snapshotCandidate?.takeIf { candidate ->
                                runCatching {
                                    java.time.Instant.parse(candidate)
                                }.isSuccess
                            }

                        for (place in result.page.places) {
                            found.add(area to place)

                            if (validSnapshot != null) {
                                snapshotBySource[place.sourceUrl] =
                                    validSnapshot
                            }
                        }
                    }

                    is KampalaGeoDiscoveryContract.Result.Unavailable -> {
                        unavailableAreas.add(area.label)
                        if (
                            result.kind ==
                                KampalaGeoDiscoveryContract
                                    .Result.Kind.INCOMPLETE
                        ) {
                            incompleteAreas.add(area.label)
                        }
                    }
                }
            }
        }

        // Overlapping experimental rectangles can return
        // the same public map listing more than once.
        // JARVIS-GEO-RELIABILITY-V1
        // Remove exact source duplicates and near-identical
        // names/coordinates from overlapping search areas.
        // Do not merge branches at different locations.
        fun normalizedPlaceName(value: String): String =
            value.lowercase(java.util.Locale.ROOT)
                .replace(Regex("""[^a-z0-9]+"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()

        fun coordinateCell(value: Double): String =
            java.lang.String.format(
                java.util.Locale.US,
                "%.4f",
                value
            )

        val seenSources = mutableSetOf<String>()
        val seenLocations = mutableSetOf<String>()

        val distinctPlaces = found.filter { (_, place) ->
            val sourceKey = place.sourceUrl
                .trim()
                .lowercase(java.util.Locale.ROOT)

            val locationKey = listOf(
                normalizedPlaceName(place.name),
                coordinateCell(place.coordinates.latitude),
                coordinateCell(place.coordinates.longitude)
            ).joinToString("|")

            if (
                sourceKey in seenSources ||
                locationKey in seenLocations
            ) {
                false
            } else {
                seenSources.add(sourceKey)
                seenLocations.add(locationKey)
                true
            }
        }

        // JARVIS-GEO-USEFUL-RESULTS-V1
        // Exclude obvious category/area placeholders BEFORE
        // limiting the response to six places.
        // Do not infer whether an actual business is operating.
        fun normalizedLabel(value: String): String =
            value.lowercase(java.util.Locale.ROOT)
                .replace(Regex("""[^a-z0-9]+"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()

        val genericLabels = setOf(
            "bank", "banks",
            "hospital", "hospitals",
            "school", "schools",
            "hotel", "hotels",
            "clinic", "clinics",
            "pharmacy", "pharmacies",
            "university", "universities",
            "guesthouse", "guesthouses",
            "guest house", "guest houses",
            "embassy", "embassies",
            "police", "police station", "police stations",
            "fuel station", "fuel stations"
        )

        val usefulPlaces = distinctPlaces.filter { (area, place) ->
            val name = normalizedLabel(place.name)
            val areaLabel = normalizedLabel(area.label)
            val providerArea = place.area
                ?.let { normalizedLabel(it) }

            name.isNotBlank() &&
                name !in genericLabels &&
                name != areaLabel &&
                (providerArea == null || name != providerArea)
        }

        val displayed = usefulPlaces.take(6)

        fun clean(value: String, maxLength: Int = 140): String =
            value.replace(Regex("""[\p{Cc}\p{Cf}]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .take(maxLength)

        return buildString {
            appendLine("Kampala multi-area map discovery")
            appendLine(
                "Category: ${clean(requestedCategory)}"
            )
            appendLine(
                "Areas searched: " +
                    areas.joinToString(", ") { it.label }
            )
            appendLine()

            if (displayed.isEmpty()) {
                appendLine(
                    "No usable public map listings were " +
                        "returned from the successful searches."
                )
                appendLine(
                    "This does not mean the places are absent."
                )
            } else {
                displayed.forEachIndexed { index, (area, place) ->
                    appendLine(
                        "${index + 1}. ${clean(place.name)}"
                    )
                    appendLine(
                        "Search area: ${clean(area.label)}"
                    )
                    appendLine(
                        "Category: ${clean(place.category, 80)}"
                    )

                    place.publicAddress
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            appendLine(
                                "Listed address: ${clean(it, 250)}"
                            )
                        }

                    place.area
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            appendLine(
                                "Provider area: ${clean(it, 120)}"
                            )
                        }

                    // JARVIS-GEO-GOOGLE-MAPS-V1
                    // Coordinates come from the validated provider
                    // record. They are not device GPS coordinates.
                    val latitude = java.lang.String.format(
                        java.util.Locale.US,
                        "%.7f",
                        place.coordinates.latitude
                    )
                    val longitude = java.lang.String.format(
                        java.util.Locale.US,
                        "%.7f",
                        place.coordinates.longitude
                    )

                    val coordinateQuery =
                        java.net.URLEncoder.encode(
                            "$latitude,$longitude",
                            "UTF-8"
                        )

                    val googleMapsUrl =
                        "https://www.google.com/maps/search/?api=1" +
                            "&query=$coordinateQuery"

                    appendLine(
                        "Open in Google Maps: $googleMapsUrl"
                    )
                    appendLine(
                        "Source record: ${place.sourceUrl}"
                    )

                    snapshotBySource[place.sourceUrl]?.let {
                        appendLine("Snapshot generated: $it")
                    }

                    // JARVIS-OSM-RECORD-DATES-V1
                    place.osmLastEditedAt?.let {
                        appendLine("OSM last edited: $it")
                    }

                    appendLine()
                }
            }

            if (usefulPlaces.size > displayed.size) {
                appendLine(
                    "Additional listings were omitted " +
                        "from this short response."
                )
            }

            if (emptyAreas.isNotEmpty()) {
                appendLine(
                    "No listings returned in: " +
                        emptyAreas.joinToString(", ")
                )
            }

            if (unavailableAreas.isNotEmpty()) {
                appendLine(
                    "Unavailable searches: " +
                        unavailableAreas.joinToString(", ")
                )
            }

            if (incompleteAreas.isNotEmpty()) {
                appendLine(
                    "Incomplete coverage: " +
                        incompleteAreas.distinct()
                            .joinToString(", ")
                )
            }

            appendLine()
            appendLine(
                "Coverage is limited to eight experimental " +
                    "search rectangles: Central Kampala and " +
                    "seven previously enabled areas."
            )
            appendLine(
                "Central Kampala uses a predefined development " +
                    "rectangle, not an official KCCA boundary. " +
                    "Old Kampala remains inactive."
            )
            appendLine(
                "The search rectangles are approximate, " +
                    "not official neighborhood boundaries."
            )
            appendLine(
                "These are public map-data listings, " +
                    "not independently verified businesses, " +
                    "addresses, opening hours or routes."
            )
            appendLine(
                "The geographic provider does not currently " +
                    "supply a last-edited date for each place. " +
                    "Listings may be old, duplicated in other " +
                    "datasets, renamed, relocated or closed."
            )
            appendLine(
                "A matching map coordinate does not confirm " +
                    "that the named organization operates there."
            )
            appendLine(
                "Check each map record before travelling."
            )
            append(
                "Map data may include OpenStreetMap " +
                    "contributors (ODbL), depending on provider."
            )
        }.trim()
    }
        // JARVIS-GEO-NAMED-CONVERSATION-V1
    // Explicit named-place request within an already approved
    // geographic search rectangle. No geocoding or device GPS.
    private suspend fun namedPlaceAnswerOrNull(
        message: String
    ): String? {
        val normalized = message
            .trim()
            .replace(Regex("""\s+"""), " ")
            .trimEnd('?', '.', '!')
            .trim()

        // JARVIS-NATIONAL-ISOLATED-V2
        // Existing area-specific requests remain bounded.
        // An omitted area enables national name lookup.
        val match = Regex(
            """^(?:find(?: me)?|locate|show me|where is|search for)\s+(.+?)(?:\s+(?:in|near|around)\s+(.+))?$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(normalized) ?: return null

        val originalName = match.groupValues[1].trim()
        val areaName = match.groupValues[2].trim()

        // Existing category discovery is authoritative.
        // Do not intercept queries such as "find hospitals in Ntinda".
        val categoryOnly = setOf(
            "hospital", "hospitals",
            "clinic", "clinics",
            "pharmacy", "pharmacies",
            "school", "schools",
            "university", "universities",
            "bank", "banks",
            "fuel station", "fuel stations",
            "hotel", "hotels",
            "guesthouse", "guesthouses",
            "guest house", "guest houses",
            "police station", "police stations",
            "embassy", "embassies",
            "apartment",
            "apartments",
            "attraction",
            "attractions",
            "bar",
            "bars",
            "cafe",
            "cafes",
            "café",
            "cafés",
            "convenience shop",
            "convenience shops",
            "convenience store",
            "convenience stores",
            "department store",
            "department stores",
            "fast food",
            "fitness centre",
            "fitness centres",
            "fitness center",
            "fitness centers",
            "hostel",
            "hostels",
            "motel",
            "motels",
            "nightclub",
            "nightclubs",
            "night club",
            "night clubs",
            "pub",
            "pubs",
            "restaurant",
            "restaurants",
            "shopping mall",
            "shopping malls",
            "sports centre",
            "sports centres",
            "sports center",
            "sports centers",
            "supermarket",
            "supermarkets"
        )

        if (
            originalName.lowercase(
                java.util.Locale.ROOT
            ) in categoryOnly
        ) return null

        // JARVIS-GEO-QUERY-PARSER-V2
        // Longest and plural suffixes first, so business names
        // are separated from their requested map category.
        val suffixes = listOf(
            "fuel stations" to "fuel_station",
            "fuel station" to "fuel_station",
            "guest houses" to "guest_house",
            "guest house" to "guest_house",
            "guesthouses" to "guest_house",
            "guesthouse" to "guest_house",
            "police stations" to "police",
            "police station" to "police",
            "universities" to "university",
            "university" to "university",
            "pharmacies" to "pharmacy",
            "pharmacy" to "pharmacy",
            "hospitals" to "hospital",
            "hospital" to "hospital",
            "clinics" to "clinic",
            "clinic" to "clinic",
            "schools" to "school",
            "school" to "school",
            "hotels" to "hotel",
            "hotel" to "hotel",
            "banks" to "bank",
            "bank" to "bank",
            "embassies" to "embassy",
            "embassy" to "embassy"
        )

        val nameLower = originalName.lowercase(
            java.util.Locale.ROOT
        )

        val suffix = suffixes.firstOrNull { (word, _) ->
            nameLower.endsWith(" $word")
        }

        val searchName = if (suffix != null) {
            originalName.dropLast(
                suffix.first.length
            ).trim()
        } else {
            originalName
        }

        if (
            searchName.length !in 2..120 ||
            searchName.any { it.isISOControl() }
        ) return null

        // JARVIS-NATIONAL-EXPLICIT-SCOPE-V1
        // Nationwide scope must be explicitly requested or omitted.
        // An unknown neighborhood never becomes Uganda-wide silently.
        val nationalScope = areaName.isBlank() ||
            areaName.equals(
                "Uganda",
                ignoreCase = true
            ) ||
            areaName.equals(
                "anywhere in Uganda",
                ignoreCase = true
            ) ||
            areaName.equals(
                "all of Uganda",
                ignoreCase = true
            )

        val area = if (nationalScope) {
            null
        } else {
            KampalaGeoAreaResolver.DEFAULT.resolve(areaName)
                ?: return (
                    "Named-place map search is not configured for " +
                    "$areaName. No location has been assumed. " +
                    "Try searching for the specific place " +
                    "without an area or explicitly in Uganda. " +
                    "Nationwide matches cannot confirm " +
                    "the requested neighborhood."
                )
        }

        val searchScope = area?.label ?: "Uganda (national index)"

        val category = suffix?.second

        // Bounded searches preserve the existing provider route.
        // National names use the separate Jarvis API operation.
        val request = area?.let {
            KampalaGeoDiscoveryContract.SearchRequest(
                query = searchName,
                category = category,
                bounds = it.bounds,
                pageSize = 20,
                pageToken = null
            )
        }

        val result = try {
            if (request != null) {
                provider.search(request)
            } else {
                val origin = KampalaGeoBackendConfig.HTTPS_ORIGIN.trim()

                if (origin.isBlank()) {
                    KampalaGeoDiscoveryContract.Result.Unavailable(
                        "The Uganda geographic backend is not configured."
                    )
                } else {
                    KampalaJarvisGeoApiProvider(origin)
                        .searchNationalName(
                            name = searchName,
                            category = category,
                            limit = 20
                        )
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "The named-place provider request failed."
            )
        }

        val validated = if (request != null) {
            KampalaGeoDiscoveryContract.validate(request, result)
        } else {
            result
        }

        if (validated is KampalaGeoDiscoveryContract.Result.Unavailable) {
            return (
                "Named-place search for $searchName in $searchScope " +
                "could not be completed. " +
                "The map results are unavailable or incomplete. " +
                "No destination has been verified."
            )
        }

        val page = (
            validated as KampalaGeoDiscoveryContract.Result.Success
        ).page

        if (page.places.isEmpty()) {
            return (
                "No map listings matched $searchName in " +
                "$searchScope. This does not prove the place " +
                "is absent or closed."
            )
        }

        fun safe(value: String): String =
            value.replace(
                Regex("""[\p{Cc}\p{Cf}]"""),
                " "
            ).replace(
                Regex("""\s+"""),
                " "
            ).trim().take(180)

        fun coordinate(value: Double): String =
            String.format(
                java.util.Locale.US,
                "%.6f",
                value
            )

        return buildString {
            appendLine(
                "Named map search: ${safe(searchName)}"
            )
            appendLine("Search scope: $searchScope")
            appendLine(
                if (area == null) {
                    "National results are not ranked by distance " +
                        "and do not establish which place is nearby."
                } else {
                    "Search bounds are approximate, not live GPS."
                }
            )
            appendLine()

            page.places.take(6).forEachIndexed { index, place ->
                val lat = coordinate(
                    place.coordinates.latitude
                )
                val lon = coordinate(
                    place.coordinates.longitude
                )

                appendLine("${index + 1}. ${safe(place.name)}")
                appendLine("Category: ${safe(place.category)}")

                place.publicAddress?.takeIf {
                    it.isNotBlank()
                }?.let {
                    appendLine("Listed address: ${safe(it)}")
                }

                appendLine("Map coordinates: $lat, $lon")

                place.osmLastEditedAt?.let {
                    appendLine("OSM last edited: $it")
                }

                appendLine(
                    "Open in Google Maps: " +
                    "https://www.google.com/maps/search/" +
                    "?api=1&query=$lat,$lon"
                )

                appendLine("Source record: ${place.sourceUrl}")
                appendLine()
            }

            if (page.places.size > 6) {
                appendLine(
                    "Additional named matches were returned " +
                    "but are not displayed."
                )
            }

            appendLine(
                "Map listings are not independently verified " +
                "business operations or navigation entrances."
            )
            append(
                "Map data: OpenStreetMap contributors (ODbL)."
            )
        }.trim()
    }
    // JARVIS-PHASE36E-METRO-CHAT-V2
    // JARVIS-PHASE36G-METRO-CORRECTIONS-V1
    // JARVIS-PHASE36H-PRESENTATION-V1
    //
    // Explicit named-locality searches only.
    // No GPS, automatic navigation, bookings or payments.
    // Existing geographic discovery remains the fallback.

    private suspend fun metropolitanChatAnswerOrNull(
        message: String
    ): String? {
        val normalized = message.trim()
            .replace(Regex("""\s+"""), " ")
            .trimEnd('?', '.', '!')
            .trim()

        val command = Regex(
            """(?i)^(?:find(?: me)?|show(?: me)?|map search)\s+(?:the\s+)?(.+?)\s+(?:in|near|around)\s+(.+)$"""
        ).matchEntire(normalized) ?: return null

        val requestedCategory = command.groupValues[1]
            .trim()
            .lowercase(java.util.Locale.ROOT)

        val categories = mapOf(
            "restaurant" to "restaurant",
            "restaurants" to "restaurant",
            "cafe" to "cafe",
            "cafes" to "cafe",
            "cafés" to "cafe",
            "hotel" to "hotel",
            "hotels" to "hotel",
            "hospital" to "hospital",
            "hospitals" to "hospital",
            "clinic" to "clinic",
            "clinics" to "clinic",
            "pharmacy" to "pharmacy",
            "pharmacies" to "pharmacy",
            "bank" to "bank",
            "banks" to "bank",
            "school" to "school",
            "schools" to "school",
            "university" to "university",
            "universities" to "university",
            "supermarket" to "supermarket",
            "supermarkets" to "supermarket",
            "fuel station" to "fuel_station",
            "fuel stations" to "fuel_station",
            "guesthouse" to "guest_house",
            "guesthouses" to "guest_house",
            "guest houses" to "guest_house",
            "shopping mall" to "shopping_mall",
            "shopping malls" to "shopping_mall",
            "apartment" to "apartment",
            "apartments" to "apartment",
            "hostel" to "hostel",
            "hostels" to "hostel",
            "attraction" to "attraction",
            "attractions" to "attraction",
            "embassy" to "embassy",
            "embassies" to "embassy",
            "bar" to "bar",
            "bars" to "bar",
            "pub" to "pub",
            "pubs" to "pub",
            "nightclub" to "nightclub",
            "nightclubs" to "nightclub",
            "night club" to "nightclub",
            "night clubs" to "nightclub",
            "motel" to "motel",
            "motels" to "motel",
            "fast food" to "fast_food",
            "convenience shop" to "convenience_shop",
            "convenience shops" to "convenience_shop",
            "convenience store" to "convenience_shop",
            "convenience stores" to "convenience_shop",
            "department store" to "department_store",
            "department stores" to "department_store",
            "fitness centre" to "fitness_centre",
            "fitness centres" to "fitness_centre",
            "fitness center" to "fitness_centre",
            "fitness centers" to "fitness_centre",
            "sports centre" to "sports_centre",
            "sports centres" to "sports_centre",
            "sports center" to "sports_centre",
            "sports centers" to "sports_centre",
            "police station" to "police",
            "police stations" to "police"
        )

        val category = categories[requestedCategory]
            ?: return null

        val areaText = command.groupValues[2].trim()

        if (
            areaText.length !in 2..120 ||
            areaText.any { it.isISOControl() }
        ) {
            return null
        }

        if (
            areaText.lowercase(java.util.Locale.ROOT) in setOf(
                "me",
                "here",
                "my location",
                "my current location"
            )
        ) {
            return "Please specify a named locality, such as " +
                "Ntinda or Kajjansi. This search does not use GPS."
        }

        val areaParts = areaText.split(",", limit = 2)
            .map { it.trim() }

        val locality = areaParts[0]

        val district = areaParts.getOrNull(1)
            ?.takeIf { it.isNotBlank() }

        if (
            locality.length !in 2..120 ||
            district?.let {
                it.length !in 2..120 ||
                    it.any { char -> char.isISOControl() }
            } == true
        ) {
            return null
        }

        val origin = KampalaGeoBackendConfig.HTTPS_ORIGIN.trim()

        if (origin.isBlank()) {
            return "The Uganda geographic service is not configured."
        }

        val api = try {
            KampalaJarvisGeoApiProvider(origin)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return "The Uganda geographic service configuration is invalid."
        }

        val resolution = api.resolveMetropolitanLocation(
            name = locality,
            district = district
        )

        when (resolution.status) {
            "NEEDS_GEOGRAPHIC_DISAMBIGUATION" -> {
                val candidateDescriptions = try {
                    val root = com.google.gson.JsonParser
                        .parseString(resolution.payload ?: "")
                        .asJsonObject

                    val candidateArrays = sequenceOf(
                        "candidates",
                        "matches",
                        "locations"
                    ).mapNotNull { key ->
                        root.get(key)
                            ?.takeIf { it.isJsonArray }
                            ?.asJsonArray
                    }

                    val candidates = candidateArrays.firstOrNull()

                    candidates?.mapNotNull { element ->
                        if (!element.isJsonObject) {
                            null
                        } else {
                            val item = element.asJsonObject

                            fun field(key: String): String? = try {
                                item.get(key)
                                    ?.takeUnless { it.isJsonNull }
                                    ?.takeIf { it.isJsonPrimitive }
                                    ?.asString
                                    ?.replace(
                                        Regex("""[\p{Cc}\p{Cf}]"""),
                                        " "
                                    )
                                    ?.trim()
                                    ?.take(90)
                                    ?.takeIf { it.isNotBlank() }
                            } catch (_: Exception) {
                                null
                            }

                            val name = field("name")
                                ?: field("label")

                            val districtName = field("district")
                            val divisionName = field("division")

                            val identity = field("identity")
                                ?: field("providerId")

                            val validIdentity = identity?.takeIf {
                                Regex(
                                    """^(node|way|relation)/[1-9][0-9]*$"""
                                ).matches(it)
                            }

                            if (name == null) {
                                null
                            } else {
                                buildString {
                                    append(name)

                                    val context = listOfNotNull(
                                        divisionName,
                                        districtName
                                    ).distinct()

                                    if (context.isNotEmpty()) {
                                        append(" — ")
                                        append(context.joinToString(", "))
                                    }

                                    if (validIdentity != null) {
                                        append(" [")
                                        append(validIdentity)
                                        append("]")
                                    }
                                }
                            }
                        }
                    }?.distinct()?.take(5).orEmpty()
                } catch (_: Exception) {
                    emptyList()
                }

                return buildString {
                    appendLine(
                        "Several mapped localities match $locality."
                    )

                    if (candidateDescriptions.isNotEmpty()) {
                        appendLine()
                        appendLine("Possible matches:")

                        candidateDescriptions.forEachIndexed {
                                index, description ->
                            appendLine(
                                "${index + 1}. $description"
                            )
                        }
                    }

                    appendLine()
                    appendLine(
                        "Please resend your search with the " +
                            "correct district or more specific locality."
                    )
                    append(
                        "No location has been selected."
                    )
                }
            }

            "ADMINISTRATIVE_CONTEXT_UNVERIFIED" -> {
                return "The available map data cannot confirm " +
                    "the requested administrative location for " +
                    "$locality. Please refine your request."
            }

            "LOCATION_NOT_INDEXED" -> {
                return "I couldn't find $locality in the available " +
                    "metropolitan locality index. That does not " +
                    "mean the place does not exist."
            }

            "RESOLVED" -> Unit

            else -> {
                val safeStatus = resolution.status
                    .takeIf {
                        it.matches(Regex("""[A-Z_]{2,64}"""))
                    } ?: "UNKNOWN_STATUS"

                return "I couldn't resolve $locality with enough " +
                    "confidence to search for $requestedCategory. " +
                    "Geographic service status: $safeStatus. " +
                    "Please specify a district or try again. " +
                    "No destination was selected."
            }
        }

        val identity = try {
            val root = com.google.gson.JsonParser
                .parseString(resolution.payload ?: "")
                .asJsonObject

            root.getAsJsonObject("selected")
                ?.get("identity")
                ?.asString
        } catch (_: Exception) {
            null
        }

        if (
            identity == null ||
            !Regex("""^(node|way|relation)/[1-9][0-9]*$""")
                .matches(identity)
        ) {
            return "The map service resolved the locality but did " +
                "not provide a usable public map identity. " +
                "No service listings were accepted and " +
                "no destination was selected."
        }

        val search = api.searchMetropolitanLocality(
            name = locality,
            category = category,
            district = district,
            selectedIdentity = identity,
            radiusKm = 5.0,
            limit = 10
        )

        if (search.status == "ADMINISTRATIVE_CONTEXT_UNVERIFIED") {
            return "The requested administrative location " +
                "could not be verified. No destination was selected."
        }

        if (search.status != "SEARCH_COMPLETED") {
            return "The map search for $requestedCategory around " +
                "$locality could not be completed reliably. " +
                "Please try again."
        }

        val root = try {
            com.google.gson.JsonParser
                .parseString(search.payload ?: "")
                .asJsonObject
        } catch (_: Exception) {
            return "The geographic service returned an unreadable result."
        }

        fun cleanText(value: String): String =
            value
                .replace(Regex("""[\p{Cc}\p{Cf}]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .take(120)

        val records = sequenceOf(
            "places",
            "results",
            "services",
            "matches"
        ).mapNotNull { key ->
            root.get(key)
                ?.takeIf { it.isJsonArray }
                ?.asJsonArray
        }.firstOrNull()

        val displayed = mutableListOf<String>()

        records?.take(3)?.forEach { element ->
            try {
                if (!element.isJsonObject) {
                    return@forEach
                }

                val entry = element.asJsonObject

                val name = entry.get("name")
                    ?.takeUnless { it.isJsonNull }
                    ?.asString
                    ?.let(::cleanText)
                    ?.takeIf { it.isNotBlank() }
                    ?: return@forEach

                val recordIdentity = sequenceOf(
                    "providerId",
                    "identity",
                    "osmIdentity"
                ).mapNotNull { key ->
                    entry.get(key)
                        ?.takeUnless { it.isJsonNull }
                        ?.asString
                }.firstOrNull() ?: return@forEach

                if (
                    !Regex("""^(node|way|relation)/[1-9][0-9]*$""")
                        .matches(recordIdentity)
                ) {
                    return@forEach
                }

                displayed.add(
                    "${displayed.size + 1}. $name\n" +
                        "OSM: https://www.openstreetmap.org/$recordIdentity"
                )
            } catch (_: Exception) {
                // Do not display malformed listing records.
            }
        }

        return buildString {
            appendLine(
                "Map search: $requestedCategory around $locality"
            )
            appendLine(
                "Search radius: 5 km from the mapped locality."
            )
            appendLine(
                "Source: OpenStreetMap / Jarvis Uganda snapshot."
            )
            appendLine()

            if (displayed.isEmpty()) {
                appendLine(
                    "The search completed, but no individual " +
                        "public listings could be safely displayed. " +
                        "This does not establish that services are absent."
                )
            } else {
                appendLine(displayed.joinToString("\n\n"))

                if (records != null && records.size() > 3) {
                    appendLine()
                    appendLine(
                        "Showing the first 3 map listings from " +
                            "this response. More listings may be available."
                    )
                }
            }

            appendLine()
            appendLine(
                "Map listings are not independently verified. " +
                    "Opening hours, entrances and availability " +
                    "may have changed."
            )
            append(
                "No navigation, booking or payment was initiated."
            )
        }.trim()
    }
suspend fun answerOrNull(message: String): String? {
        val normalized = message
            .trim()
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("""\s+"""), " ")
            .trimEnd('?', '.', '!')
            .trim()

        metropolitanChatAnswerOrNull(message)?.let { return it }

        // Match only an explicit short place-discovery follow-up.
        // A new journey or unrelated message must remain untouched.
        val followUp = Regex(
            """^(?:(?:find|show|show me)\s+)?(?:the\s+)?(hospitals|clinics|pharmacies|schools|universities|banks|hotels|guesthouses|embassies)(?:\s+too)?(?:\s*,?\s+and\s+(?:the\s+)?(hospitals|clinics|pharmacies|schools|universities|banks|hotels|guesthouses|embassies)(?:\s+too)?)?$"""
        ).matchEntire(normalized)

        if (lastMultiAreaSearch && followUp != null) {
            val requested = listOf(
                followUp.groupValues[1],
                followUp.groupValues[2]
            ).filter { it.isNotBlank() }
                .distinct()

            val responses = mutableListOf<String>()

            // Sequential calls preserve the existing
            // provider's limited per-area concurrency.
            for (category in requested) {
                val response = answerAcrossAreasOrNull(
                    "find $category across kampala"
                )

                responses.add(
                    response ?: "Map discovery could not process $category."
                )
            }

            if (responses.isNotEmpty()) {
                return responses.joinToString(
                    "\n\n---\n\n"
                )
            }
        }

        // A new explicit multi-area search establishes the
        // context for later short follow-up requests.
        val multiAreaResult =
            answerAcrossAreasOrNull(message)

        if (multiAreaResult != null) {
            lastMultiAreaSearch = true
            return multiAreaResult
        }

        namedPlaceAnswerOrNull(message)?.let { return it }

        val command = parseOrNull(message)

        // Single-area requests are independent searches.
        if (command != null) {
            lastMultiAreaSearch = false
        }

        if (command == null) {
            // Recognized map requests for unsupported geographic
            // areas must not fall through to generative AI.
            val normalized = message
                .trim()
                .lowercase(java.util.Locale.ROOT)
                .replace(Regex("""\s+"""), " ")
                .trimEnd('?', '.', '!')
                .trim()

            val geographicPhrase = Regex(
                """^(?:map search|find(?: me)?|show me|show|are there) (.+?) in (.+)$"""
            ).matchEntire(normalized)

            val recognizedCategories = setOf(
                "hospitals",
                "clinics",
                "pharmacies",
                "schools",
                "universities",
                "banks",
                "fuel stations",
                "hotels",
                "guesthouses",
                "guest houses",
                "police stations",
                "embassies",
                "apartment",
                "apartments",
                "attraction",
                "attractions",
                "bar",
                "bars",
                "cafe",
                "cafes",
                "café",
                "cafés",
                "convenience shop",
                "convenience shops",
                "convenience store",
                "convenience stores",
                "department store",
                "department stores",
                "fast food",
                "fitness centre",
                "fitness centres",
                "fitness center",
                "fitness centers",
                "hostel",
                "hostels",
                "motel",
                "motels",
                "nightclub",
                "nightclubs",
                "night club",
                "night clubs",
                "pub",
                "pubs",
                "restaurant",
                "restaurants",
                "shopping mall",
                "shopping malls",
                "sports centre",
                "sports centres",
                "sports center",
                "sports centers",
                "supermarket",
                "supermarkets"
            )

            if (geographicPhrase != null) {
                val category =
                    geographicPhrase.groupValues[1].trim()

                val area =
                    geographicPhrase.groupValues[2].trim()

                if (
                    category in recognizedCategories &&
                    area.isNotBlank()
                ) {
                    return "Geographic search area not yet supported: " +
                        area.replaceFirstChar {
                            it.titlecase(java.util.Locale.ROOT)
                        } +
                        ". Jarvis has not verified geographic search " +
                        "bounds for this area. No map search was " +
                        "performed and no places or routes have " +
                        "been confirmed. Try a supported Kampala " +
                        "area or provide a specific public place name."
                }
            }

            // Ordinary chat continues through existing routing.
            return null
        }
        val request = command.toRequest()

        val result = try {
            provider.search(request)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "Geographic provider request failed."
            )
        }

        val validated = KampalaGeoDiscoveryContract.validate(
            request,
            result
        )

        return buildString {
            appendLine(
                "Experimental map search: ${command.label}"
            )
            appendLine(
                "Search category: ${command.category}"
            )
            appendLine(
                "Search area: predefined bounds, not live GPS."
            )
            if (!command.label.equals(
                    "Central Kampala",
                    ignoreCase = true
                )
            ) {
                appendLine(
                    "Area coverage: approximate third-party " +
                        "parish bounding rectangle; not an " +
                        "officially verified neighborhood " +
                        "boundary. Nearby places outside the " +
                        "parish polygon may be included."
                )
            }
            appendLine()
            append(
                KampalaGeoDiscoveryPresentation.format(
                    validated,
                    command.category
                )
            )
        }
    }
}