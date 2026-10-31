package com.example.jarvis.core.network

import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * CDI.6.0-A6.9-D2B
 *
 * Experimental bounded OpenStreetMap/Overpass discovery.
 *
 * Not wired into chat, location services, or automatic polling.
 * Public Overpass instances are not a production SLA.
 *
 * Attribution: © OpenStreetMap contributors (ODbL).
 */
internal class KampalaOverpassDiscoveryProvider(
    private val transport: Transport = HttpTransport()
) : KampalaGeoDiscoveryContract.Provider {

    internal interface Transport {
        suspend fun fetch(overpassQuery: String): String
    }

    internal class HttpTransport : Transport {

        // Dedicated HTTP client: does not inherit BODY logging.
        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()

        override suspend fun fetch(
            overpassQuery: String
        ): String = withContext(Dispatchers.IO) {

            val body = FormBody.Builder()
                .add("data", overpassQuery)
                .build()

            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .header(
                    "User-Agent",
                    "Jarvis-Android-Experimental/0.1 " +
                        "(https://github.com/Pedurabo/Jarvis-Android)"
                )
                .header("Accept", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // CDI.6.0-A6.9-D5B:
                    // Preserve the status code as a typed failure.
                    // Never include a response body or server HTML.
                    throw KampalaOverpassHttpException(response.code)
                }

                val responseBody = response.body
                    ?: throw IOException("Empty Overpass response")

                // Cap actual response bytes, not just JSON records.
                val source = responseBody.source()
                val buffer = okio.Buffer()

                val maxBytes = 512L * 1024L
                var total = 0L

                while (true) {
                    val remaining = maxBytes + 1L - total
                    if (remaining <= 0L) {
                        throw IOException(
                            "Overpass response exceeded size limit"
                        )
                    }

                    val count = source.read(
                        buffer,
                        minOf(remaining, 8192L)
                    )

                    if (count == -1L) break
                    total += count

                    if (total > maxBytes) {
                        throw IOException(
                            "Overpass response exceeded size limit"
                        )
                    }
                }

                buffer.readUtf8()
            }
        }
    }

    companion object {

        private const val PROVIDER = "OpenStreetMap/Overpass"

        /**
         * Strictly allowlisted OSM tags.
         * No user-entered query text is interpolated into QL.
         */
        internal fun tagForCategory(
            category: String?
        ): Pair<String, String>? =
            when (category?.trim()?.lowercase(Locale.ROOT)) {
                "hospital" -> "amenity" to "hospital"
                "clinic" -> "amenity" to "clinic"
                "pharmacy" -> "amenity" to "pharmacy"
                "school" -> "amenity" to "school"
                "university" -> "amenity" to "university"
                "bank" -> "amenity" to "bank"
                "fuel_station" -> "amenity" to "fuel"
                "hotel" -> "tourism" to "hotel"
                "guest_house" -> "tourism" to "guest_house"
                "police" -> "amenity" to "police"
                "embassy" -> "office" to "diplomatic"
                else -> null
            }

        internal fun buildQuery(
            request: KampalaGeoDiscoveryContract.SearchRequest
        ): String? {
            if (!KampalaGeoDiscoveryContract.validRequest(request)) {
                return null
            }

            // First version: category-only, no free-text queries
            // and no unsupported cursor-pagination semantics.
            if (!request.query.isNullOrBlank()) return null
            if (request.pageToken != null) return null

            val tag = tagForCategory(request.category)
                ?: return null

            val b = request.bounds

            // Limit public-server query footprint.
            if (b.north - b.south > 0.10) return null
            if (b.east - b.west > 0.10) return null

            fun number(value: Double): String =
                String.format(Locale.US, "%.6f", value)

            val box = listOf(
                b.south, b.west, b.north, b.east
            ).joinToString(",") { number(it) }

            return "[out:json][timeout:15];" +
                "nwr[\"${tag.first}\"=\"${tag.second}\"]($box);" +
                "out center ${request.pageSize + 1};"
        }

        internal fun parse(
            json: String,
            request: KampalaGeoDiscoveryContract.SearchRequest
        ): KampalaGeoDiscoveryContract.Result {
            val root = JsonParser.parseString(json).asJsonObject
            val elements = root.getAsJsonArray("elements")
                ?: return KampalaGeoDiscoveryContract.Result.Unavailable(
                    "Overpass response omitted elements."
                )

            // Request one extra element to detect incomplete pages.
            // Do not silently present capped results as complete.
            if (elements.size() > request.pageSize) {
                return KampalaGeoDiscoveryContract.Result.Unavailable(
                    "OpenStreetMap returned more matches than " +
                        "the requested limit. Narrow the search area."
                )
            }

            val requiredTag = tagForCategory(request.category)
                ?: return KampalaGeoDiscoveryContract.Result.Unavailable(
                    "Unsupported place category."
                )

            val places = mutableListOf<
                KampalaGeoDiscoveryContract.Place
            >()

            for (element in elements) {
                if (!element.isJsonObject) continue

                val obj = element.asJsonObject
                val type = obj.get("type")?.asString ?: continue

                if (type !in setOf("node", "way", "relation")) {
                    continue
                }

                val id = obj.get("id")?.asLong ?: continue
                if (id <= 0L) continue

                val tags = obj.getAsJsonObject("tags") ?: continue

                if (
                    tags.get(requiredTag.first)?.asString !=
                    requiredTag.second
                ) {
                    continue
                }

                val name = tags.get("name")?.asString
                    ?.trim().orEmpty()

                // Unnamed features are not usable place results.
                if (name.isBlank()) continue

                val point = if (type == "node") {
                    obj
                } else {
                    obj.getAsJsonObject("center") ?: continue
                }

                val lat = point.get("lat")?.asDouble ?: continue
                val lon = point.get("lon")?.asDouble ?: continue

                val coordinates =
                    KampalaGeoDiscoveryContract.Coordinates(lat, lon)

                if (!KampalaGeoDiscoveryContract.withinBounds(
                        coordinates,
                        request.bounds
                    )
                ) continue

                val source =
                    "https://www.openstreetmap.org/$type/$id"

                // OSM map features are public listings,
                // not officially confirmed business records.
                places += KampalaGeoDiscoveryContract.Place(
                    providerId = "$type/$id",
                    name = name,
                    category = request.category.orEmpty(),
                    coordinates = coordinates,
                    publicAddress = null,
                    area = null,
                    sourceUrl = source,
                    isPublicListing = true
                )
            }

            val result =
                KampalaGeoDiscoveryContract.Result.Success(
                    KampalaGeoDiscoveryContract.Page(
                        provider = PROVIDER,
                        places = places,
                        nextPageToken = null
                    )
                )

            return KampalaGeoDiscoveryContract.validate(
                request,
                result
            )
        }
    }

    override suspend fun search(
        request: KampalaGeoDiscoveryContract.SearchRequest
    ): KampalaGeoDiscoveryContract.Result {

        val query = buildQuery(request)
            ?: return KampalaGeoDiscoveryContract.Result.Unavailable(
                "Unsupported or oversized geographic request."
            )

        return try {
            val json = transport.fetch(query)
            parse(json, request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            // CDI.6.0-A6.9-D5B:
            // Classify failures without leaking raw exception
            // messages, URLs, request bodies or user context.
            KampalaGeoDiscoveryContract.Result.Unavailable(
                KampalaOverpassFailurePolicy.reason(failure)
            )
        }
    }
}