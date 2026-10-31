package com.example.jarvis.core.network

import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.net.URI
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * CDI.6.0-A6.9-D5K-B2
 *
 * HTTPS adapter for the Jarvis Uganda geographic API.
 *
 * No GPS, background polling, route calculation,
 * automatic Overpass fallback or controller cutover.
 */
internal class KampalaJarvisGeoApiProvider(
    baseUrl: String,
    private val transport: Transport = HttpTransport()
) : KampalaGeoDiscoveryContract.Provider {

    internal data class HttpResult(
        val statusCode: Int,
        val body: String
    )

    internal interface Transport {
        suspend fun get(url: String): HttpResult
    }

    internal class HttpTransport : Transport {

        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()

        override suspend fun get(
            url: String
        ): HttpResult = withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->

                val body = response.body
                    ?: return@withContext HttpResult(
                        response.code,
                        ""
                    )

                val buffer = Buffer()
                val source = body.source()

                val maxBytes = 256L * 1024L
                var total = 0L

                while (true) {
                    val remaining = maxBytes + 1L - total

                    if (remaining <= 0L) {
                        throw IllegalStateException(
                            "Geographic response exceeded size limit"
                        )
                    }

                    val count = source.read(
                        buffer,
                        minOf(8192L, remaining)
                    )

                    if (count == -1L) break

                    total += count

                    if (total > maxBytes) {
                        throw IllegalStateException(
                            "Geographic response exceeded size limit"
                        )
                    }
                }

                HttpResult(
                    response.code,
                    buffer.readUtf8()
                )
            }
        }
    }

    private val endpoint = validateBaseUrl(baseUrl)

    companion object {

        private val supportedCategories = setOf(
            "hospital",
            "clinic",
            "pharmacy",
            "school",
            "university",
            "bank",
            "fuel_station",
            "hotel",
            "guest_house",
            "police",
            "embassy",
            // JARVIS-GEO27-INTEGRATION-V5
            "apartment",
            "attraction",
            "bar",
            "cafe",
            "convenience_shop",
            "department_store",
            "fast_food",
            "fitness_centre",
            "hostel",
            "motel",
            "nightclub",
            "pub",
            "restaurant",
            "shopping_mall",
            "sports_centre",
            "supermarket"
        )

        internal fun validateBaseUrl(value: String): String {

            val normalized = value.trim()
            val uri = URI(normalized)

            require(
                uri.scheme == "https" &&
                    !uri.host.isNullOrBlank() &&
                    uri.rawUserInfo == null &&
                    uri.rawQuery == null &&
                    uri.rawFragment == null &&
                    (
                        uri.rawPath.isNullOrEmpty() ||
                            uri.rawPath == "/"
                    ) &&
                    (
                        uri.port == -1 ||
                            uri.port in 1..65535
                    )
            ) {
                "An HTTPS backend origin is required."
            }

            return normalized.trimEnd('/') + "/geo/search"
        }

        private fun number(value: Double): String =
            String.format(Locale.US, "%.6f", value)

        internal fun requestUrl(
            endpoint: String,
            request: KampalaGeoDiscoveryContract.SearchRequest
        ): String? {

            if (
                !KampalaGeoDiscoveryContract.validRequest(request)
            ) {
                return null
            }

            // P3-A2D: Separate bounded name search from category discovery.
            if (request.pageToken != null) return null

            val category = request.category
            if (category != null && category !in supportedCategories) {
                return null
            }

            val bounds = request.bounds
            if (bounds.north - bounds.south > 0.10) return null
            if (bounds.east - bounds.west > 0.10) return null

            val boundsQuery =
                "&south=${number(bounds.south)}" +
                "&west=${number(bounds.west)}" +
                "&north=${number(bounds.north)}" +
                "&east=${number(bounds.east)}" +
                "&limit=${request.pageSize}"

            val name = request.query?.trim()

            if (!name.isNullOrBlank()) {
                if (name.length !in 2..120) return null

                val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
                val nameEndpoint =
                    endpoint.removeSuffix("/search") + "/name-search"

                val categoryQuery =
                    category?.let { "&category=$it" }.orEmpty()

                return nameEndpoint +
                    "?query=$encodedName" +
                    categoryQuery +
                    boundsQuery
            }

            if (category == null) return null

            return endpoint +
                "?category=$category" +
                boundsQuery
        }

        internal fun parse(
            json: String,
            request: KampalaGeoDiscoveryContract.SearchRequest
        ): KampalaGeoDiscoveryContract.Result {

            fun unavailable() =
                KampalaGeoDiscoveryContract.Result.Unavailable(
                    "The Uganda geographic index returned " +
                        "an unusable or incomplete response."
                )

            return try {

                val root = JsonParser.parseString(json).asJsonObject

                if (root.get("status")?.asString == "INCOMPLETE") {
                    val entries = root.getAsJsonArray("places")
                    val message = root.get("message")
                        ?.takeUnless { it.isJsonNull }
                        ?.asString

                    if (
                        entries != null &&
                        entries.size() == 0 &&
                        (
                            message == "Narrow the search area." ||
                            (
                                !request.query.isNullOrBlank() &&
                                message ==
                                    "More matching places exist; refine the name or category."
                            )
                        )
                    ) {
                        return KampalaGeoDiscoveryContract.Result.Unavailable(
                            "The map search area contains more listings than " +
                                "one response can safely return.",
                            KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE
                        )
                    }

                    return unavailable()
                }

                if (
                    root.get("status")?.asString != "SUCCESS"
                ) {
                    return unavailable()
                }

                if (
                    root.get("verification")?.asString !=
                    "MAP_LISTINGS_ONLY"
                ) {
                    return unavailable()
                }

                val timestamp = root
                    .get("snapshotGeneratedAt")
                    ?.asString
                    ?: return unavailable()

                Instant.parse(timestamp)

                val attribution = root
                    .get("attribution")
                    ?.asString
                    .orEmpty()

                if (
                    !attribution.contains(
                        "OpenStreetMap",
                        ignoreCase = true
                    )
                ) {
                    return unavailable()
                }

                val entries = root.getAsJsonArray("places")
                    ?: return unavailable()

                if (entries.size() > request.pageSize) {
                    return unavailable()
                }

                // P3-A2D: Optional category only for name search.
                val category = request.category
                if (
                    category == null &&
                    request.query.isNullOrBlank()
                ) return unavailable()

                val places = entries.map { entry ->

                    val item = entry.asJsonObject

                    val providerId =
                        item.get("providerId").asString.trim()

                    val osmIdentity = Regex(
                        """^(node|way|relation)/([1-9][0-9]*)$"""
                    ).matchEntire(providerId)

                    require(osmIdentity != null)

                    val type = osmIdentity.groupValues[1]
                    val id = osmIdentity.groupValues[2].toLong()
                    val name = item.get("name").asString
                    val recordCategory =
                        item.get("category").asString

                    val latitude =
                        item.get("latitude").asDouble

                    val longitude =
                        item.get("longitude").asDouble

                    val sourceUrl =
                        item.get("sourceUrl").asString

                    require(id > 0L)
                    require(
                        recordCategory in supportedCategories &&
                            (category == null || recordCategory == category)
                    )

                    if (!request.query.isNullOrBlank()) {
                        val normalizedName = name.trim()
                            .replace(Regex("""\s+"""), " ")
                            .lowercase()
                        val normalizedQuery = request.query.trim()
                            .replace(Regex("""\s+"""), " ")
                            .lowercase()
                        require(normalizedName.contains(normalizedQuery))
                    }

                    require(
                        sourceUrl ==
                            "https://www.openstreetmap.org/$type/$id"
                    )

                    val address = item.get("publicAddress")
                        ?.takeUnless { it.isJsonNull }
                        ?.asString

                    // JARVIS-OSM-RECORD-DATES-V1
                    // Do not substitute snapshotGeneratedAt.
                    val recordEdited =
                        item.get("osmLastEditedAt")
                            ?.takeUnless { it.isJsonNull }
                            ?.let { element ->
                                runCatching {
                                    val candidate =
                                        element.asString
                                    val parsed =
                                        Instant.parse(candidate)

                                    if (
                                        parsed.isAfter(
                                            Instant.now()
                                        )
                                    ) {
                                        null
                                    } else {
                                        parsed.toString()
                                    }
                                }.getOrNull()
                            }

                    KampalaGeoDiscoveryContract.Place(
                        providerId = providerId,
                        name = name,
                        category = recordCategory,
                        coordinates =
                            KampalaGeoDiscoveryContract.Coordinates(
                                latitude,
                                longitude
                            ),
                        publicAddress = address,
                        area = null,
                        sourceUrl = sourceUrl,
                        isPublicListing = true,
                        osmLastEditedAt = recordEdited
                    )
                }

                val result =
                    KampalaGeoDiscoveryContract.Result.Success(
                        KampalaGeoDiscoveryContract.Page(
                            provider =
                                "OpenStreetMap / Jarvis Uganda " +
                                    "snapshot ($timestamp)",
                            places = places,
                            nextPageToken = null
                        )
                    )

                KampalaGeoDiscoveryContract.validate(
                    request,
                    result
                )

            } catch (_: Exception) {
                unavailable()
            }
        }
    }

    /**
     * Retries one transient transport failure once.
     *
     * HTTP 502/503/504 and socket timeouts are eligible.
     * All other HTTP responses and exceptions preserve
     * the existing provider behavior.
     */
    private suspend fun getWithOneTransientRetry(
        url: String
    ): HttpResult {
        var attempt = 0

        while (true) {
            try {
                val response = transport.get(url)

                if (
                    attempt == 0 &&
                    response.statusCode in setOf(502, 503, 504)
                ) {
                    attempt++
                    kotlinx.coroutines.delay(350L)
                    continue
                }

                return response

            } catch (cancelled: CancellationException) {
                throw cancelled

            } catch (timeout: java.net.SocketTimeoutException) {
                if (attempt != 0) {
                    throw timeout
                }

                attempt++
                kotlinx.coroutines.delay(350L)
            }
        }
    }
    // JARVIS-NATIONAL-ISOLATED-V2
    // Request URL contains no coordinates. World bounds are
    // used only to validate the returned OSM records locally.
    suspend fun searchNationalName(
        name: String,
        category: String? = null,
        limit: Int = 20
    ): KampalaGeoDiscoveryContract.Result {
        val cleaned = name.trim()

        if (
            cleaned.length !in 2..120 ||
            cleaned.any { it.isISOControl() } ||
            (category != null && category !in supportedCategories) ||
            limit !in 1..50
        ) {
            return KampalaGeoDiscoveryContract.Result.Unavailable(
                "Invalid national named-place request."
            )
        }

        val validationRequest =
            KampalaGeoDiscoveryContract.SearchRequest(
                query = cleaned,
                category = category,
                bounds = KampalaGeoDiscoveryContract.Bounds(
                    south = -90.0,
                    west = -180.0,
                    north = 90.0,
                    east = 180.0
                ),
                pageSize = limit,
                pageToken = null
            )

        val url = endpoint.removeSuffix("/search") +
            "/name-search?query=" +
            java.net.URLEncoder.encode(cleaned, "UTF-8") +
            (category?.let { "&category=$it" } ?: "") +
            "&limit=$limit"

        return try {
            val response = getWithOneTransientRetry(url)

            if (response.statusCode == 200) {
                parse(response.body, validationRequest)
            } else {
                KampalaGeoDiscoveryContract.Result.Unavailable(
                    "The Uganda national place service is unavailable."
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "The national named-place search failed."
            )
        }
    }
    // JARVIS-METROPOLITAN-V2-PROVIDER-V1
    //
    // Opt-in HTTP integration for the deployed metropolitan API.
    // The existing geographic Provider contract remains unchanged.
    //
    // Results are transport-level responses, not verified destinations.
    // Conversation and navigation policies must interpret these safely.

    internal data class MetropolitanResponse(
        val status: String,
        val httpStatus: Int,
        val payload: String?,
        val message: String
    ) {
        val available: Boolean
            get() = httpStatus == 200

        val needsDisambiguation: Boolean
            get() = status == "NEEDS_GEOGRAPHIC_DISAMBIGUATION"

        val administrativeContextUnverified: Boolean
            get() = status == "ADMINISTRATIVE_CONTEXT_UNVERIFIED"
    }

    private fun metropolitanUnavailable(
        message: String
    ) = MetropolitanResponse(
        status = "UNAVAILABLE",
        httpStatus = 0,
        payload = null,
        message = message
    )

    private fun metropolitanText(
        value: String?,
        maxLength: Int = 120
    ): String? {
        val normalized = value?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        if (
            normalized.length > maxLength ||
            normalized.any { it.isISOControl() }
        ) {
            return null
        }

        return normalized
    }

    private fun metropolitanEncode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    private fun metropolitanOrigin(): String =
        endpoint.removeSuffix("/geo/search")

    private suspend fun metropolitanGet(
        path: String,
        parameters: List<Pair<String, String>>
    ): MetropolitanResponse {
        val query = parameters.joinToString("&") { (key, value) ->
            metropolitanEncode(key) + "=" + metropolitanEncode(value)
        }

        val url = metropolitanOrigin() + path +
            if (query.isEmpty()) "" else "?$query"

        return try {
            val response = getWithOneTransientRetry(url)

            val root = try {
                JsonParser.parseString(response.body).asJsonObject
            } catch (_: Exception) {
                return metropolitanUnavailable(
                    "The metropolitan geographic service returned " +
                        "an unreadable response."
                )
            }

            val status = try {
                root.get("status")?.asString
            } catch (_: Exception) {
                null
            } ?: return metropolitanUnavailable(
                "The metropolitan geographic response has no status."
            )

            val accepted = setOf(
                "RESOLVED",
                "NEEDS_GEOGRAPHIC_DISAMBIGUATION",
                "ADMINISTRATIVE_CONTEXT_UNVERIFIED",
                "SEARCH_COMPLETED",
                "LOCATION_NOT_INDEXED",
                "INVALID_REQUEST"
            )

            if (
                status !in accepted ||
                response.statusCode !in setOf(200, 400, 404)
            ) {
                return metropolitanUnavailable(
                    "The metropolitan geographic service is unavailable."
                )
            }

            MetropolitanResponse(
                status = status,
                httpStatus = response.statusCode,
                payload = response.body,
                message = when (status) {
                    "RESOLVED" ->
                        "A geographic identity was resolved."

                    "NEEDS_GEOGRAPHIC_DISAMBIGUATION" ->
                        "Multiple geographic identities may match."

                    "ADMINISTRATIVE_CONTEXT_UNVERIFIED" ->
                        "The requested administrative membership " +
                            "has not been verified."

                    "SEARCH_COMPLETED" ->
                        "The geographic search completed."

                    "LOCATION_NOT_INDEXED" ->
                        "The location was not found in the " +
                            "available geographic index."

                    else ->
                        "The geographic request was not accepted."
                }
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: java.net.SocketTimeoutException) {
            metropolitanUnavailable(
                "The geographic service timed out. " +
                    "Its availability has not been established."
            )
        } catch (_: Exception) {
            metropolitanUnavailable(
                "The metropolitan geographic service could not " +
                    "be reached or its response could not be processed."
            )
        }
    }

    suspend fun resolveMetropolitanLocation(
        name: String,
        district: String? = null,
        division: String? = null,
        selectedIdentity: String? = null
    ): MetropolitanResponse {
        val cleanName = metropolitanText(name)
            ?: return metropolitanUnavailable(
                "A valid locality name is required."
            )

        val cleanDistrict = district?.let {
            metropolitanText(it)
                ?: return metropolitanUnavailable(
                    "The district value is invalid."
                )
        }

        val cleanDivision = division?.let {
            metropolitanText(it)
                ?: return metropolitanUnavailable(
                    "The division value is invalid."
                )
        }

        val cleanIdentity = selectedIdentity?.let {
            val identity = metropolitanText(it, 80)
                ?: return metropolitanUnavailable(
                    "The selected geographic identity is invalid."
                )

            if (
                !Regex("^(node|way|relation)/[1-9][0-9]*$")
                    .matches(identity)
            ) {
                return metropolitanUnavailable(
                    "The selected geographic identity is invalid."
                )
            }

            identity
        }

        val parameters = mutableListOf(
            "name" to cleanName
        )

        cleanDistrict?.let {
            parameters.add("district" to it)
        }

        cleanDivision?.let {
            parameters.add("division" to it)
        }

        cleanIdentity?.let {
            parameters.add("selectedIdentity" to it)
        }

        return metropolitanGet(
            "/geo/v2/resolve",
            parameters
        )
    }

    suspend fun searchMetropolitanLocality(
        name: String,
        category: String? = null,
        district: String? = null,
        division: String? = null,
        selectedIdentity: String? = null,
        radiusKm: Double = 5.0,
        limit: Int = 10
    ): MetropolitanResponse {
        val cleanName = metropolitanText(name)
            ?: return metropolitanUnavailable(
                "A valid locality name is required."
            )

        if (
            !radiusKm.isFinite() ||
            radiusKm <= 0.0 ||
            radiusKm > 50.0 ||
            limit !in 1..50
        ) {
            return metropolitanUnavailable(
                "The search radius or result limit is invalid."
            )
        }

        val parameters = mutableListOf(
            "name" to cleanName,
            "radiusKm" to String.format(
                Locale.US, "%.3f", radiusKm
            ),
            "limit" to limit.toString()
        )

        for ((key, value) in listOf(
            "category" to category,
            "district" to district,
            "division" to division,
            "selectedIdentity" to selectedIdentity
        )) {
            if (value != null) {
                val cleaned = metropolitanText(value)
                    ?: return metropolitanUnavailable(
                        "The $key value is invalid."
                    )

                if (
                    key == "selectedIdentity" &&
                    !Regex("^(node|way|relation)/[1-9][0-9]*$")
                        .matches(cleaned)
                ) {
                    return metropolitanUnavailable(
                        "The geographic identity is invalid."
                    )
                }

                parameters.add(key to cleaned)
            }
        }

        return metropolitanGet(
            "/geo/v2/locality-search",
            parameters
        )
    }

    suspend fun searchMetropolitanArea(
        district: String,
        division: String? = null,
        category: String? = null,
        limit: Int = 10
    ): MetropolitanResponse {
        val cleanDistrict = metropolitanText(district)
            ?: return metropolitanUnavailable(
                "A valid district is required."
            )

        if (limit !in 1..50) {
            return metropolitanUnavailable(
                "The search result limit is invalid."
            )
        }

        val parameters = mutableListOf(
            "district" to cleanDistrict,
            "limit" to limit.toString()
        )

        for ((key, value) in listOf(
            "division" to division,
            "category" to category
        )) {
            if (value != null) {
                val cleaned = metropolitanText(value)
                    ?: return metropolitanUnavailable(
                        "The $key value is invalid."
                    )

                parameters.add(key to cleaned)
            }
        }

        return metropolitanGet(
            "/geo/v2/area-search",
            parameters
        )
    }
    override suspend fun search(
        request: KampalaGeoDiscoveryContract.SearchRequest
    ): KampalaGeoDiscoveryContract.Result {

        val url = requestUrl(endpoint, request)
            ?: return KampalaGeoDiscoveryContract.Result.Unavailable(
                "Unsupported Uganda geographic request."
            )

        return try {

            val response = getWithOneTransientRetry(url)

            if (response.statusCode == 200) {
                parse(response.body, request)
            } else {
                val reason = when (response.statusCode) {
                    408, 504 ->
                        "The Uganda geographic service timed out."
                    429 ->
                        "The Uganda geographic service is rate-limiting requests."
                    in 500..599 ->
                        "The Uganda geographic service returned a server error."
                    else ->
                        "The Uganda geographic service returned an HTTP error."
                }

                KampalaGeoDiscoveryContract.Result.Unavailable(reason)
            }

        } catch (cancelled: CancellationException) {
            throw cancelled

        } catch (_: java.net.SocketTimeoutException) {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "The Uganda geographic service request timed out."
            )

        } catch (_: java.net.UnknownHostException) {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "The Uganda geographic service hostname could not be resolved."
            )

        } catch (_: java.net.ConnectException) {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "A connection to the Uganda geographic service could not be established."
            )

        } catch (_: javax.net.ssl.SSLException) {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "A secure connection to the Uganda geographic service failed."
            )

        } catch (_: java.io.IOException) {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "The Uganda geographic service connection was interrupted."
            )

        } catch (_: Exception) {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "The Uganda geographic service encountered an unexpected request error."
            )
        }
    }
}