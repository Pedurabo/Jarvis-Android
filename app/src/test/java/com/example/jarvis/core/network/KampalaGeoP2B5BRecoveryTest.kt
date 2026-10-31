package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.util.Locale

/**
 * P2-B5B:
 * Five real INCOMPLETE root responses from P2-B4.
 *
 * Child responses are deterministic test fixtures,
 * NOT claims about live child-tile coverage.
 */
class KampalaGeoP2B5BRecoveryTest {

    private val names = listOf(
        "Ntinda",
        "Old Kampala",
        "Kansanga",
        "Kabalagala",
        "Bugoloobi"
    )

    private fun fixture(name: String): String {
        val safe = name.replace(" ", "-")

        val stream = javaClass.classLoader
            ?.getResourceAsStream("geo-p2b5b/$safe-bank.json")
            ?: error("Missing fixture for $name")

        return stream.bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
    }

    private fun requestBounds(
        url: String
    ): KampalaGeoDiscoveryContract.Bounds {

        val uri = URI(url)

        assertEquals("/geo/search", uri.path)

        val query = uri.rawQuery
            .split("&")
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0] to parts[1]
            }

        assertEquals("bank", query["category"])
        assertEquals("10", query["limit"])

        fun number(key: String): Double =
            query[key]?.toDouble()
                ?: error("Missing coordinate $key")

        return KampalaGeoDiscoveryContract.Bounds(
            south = number("south"),
            west = number("west"),
            north = number("north"),
            east = number("east")
        )
    }

    @Test
    fun fiveRealIncompleteParentsTriggerSafeSubdivision() = runBlocking {

        val catalog = KampalaGeoP2CandidateCatalog.areas
            .associateBy { it.label }

        for ((index, name) in names.withIndex()) {

            val area = catalog[name]
                ?: error("Missing candidate area $name")

            val request = KampalaGeoDiscoveryContract.SearchRequest(
                category = "bank",
                bounds = area.bounds,
                pageSize = 10
            )

            val rootFixture = fixture(name)

            var calls = 0
            val tiles = mutableListOf<KampalaGeoDiscoveryContract.Bounds>()

            val transport = object :
                KampalaJarvisGeoApiProvider.Transport {

                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult {

                    val bounds = requestBounds(url)

                    calls++
                    tiles.add(bounds)

                    if (calls == 1) {
                        assertEquals(area.bounds.south, bounds.south, 0.000001)
                        assertEquals(area.bounds.west, bounds.west, 0.000001)
                        assertEquals(area.bounds.north, bounds.north, 0.000001)
                        assertEquals(area.bounds.east, bounds.east, 0.000001)

                        return KampalaJarvisGeoApiProvider.HttpResult(
                            200,
                            rootFixture
                        )
                    }

                    // The first child returns one synthetic listing;
                    // the remaining children complete with zero listings.
                    val places = if (calls == 2) {
                        val latitude =
                            (bounds.south + bounds.north) / 2.0

                        val longitude =
                            (bounds.west + bounds.east) / 2.0

                        val id = 900000001L + index

                        val latText = String.format(
                            Locale.US, "%.8f", latitude
                        )

                        val lonText = String.format(
                            Locale.US, "%.8f", longitude
                        )

                        """
                        [{
                          "providerId":"node/$id",
                          "name":"Synthetic Test Bank",
                          "category":"bank",
                          "latitude":$latText,
                          "longitude":$lonText,
                          "publicAddress":null,
                          "sourceUrl":"https://www.openstreetmap.org/node/$id"
                        }]
                        """.trimIndent()
                    } else {
                        "[]"
                    }

                    val response = """
                        {
                          "status":"SUCCESS",
                          "verification":"MAP_LISTINGS_ONLY",
                          "snapshotGeneratedAt":"2026-10-25T21:21:21Z",
                          "attribution":"(c) OpenStreetMap contributors",
                          "places":$places
                        }
                    """.trimIndent()

                    return KampalaJarvisGeoApiProvider.HttpResult(
                        200,
                        response
                    )
                }
            }

            val api = KampalaJarvisGeoApiProvider(
                "https://jarvis-uganda-geo-service.onrender.com",
                transport
            )

            val provider = KampalaGeoSubdivisionProvider(api)

            val result = provider.search(request)

            assertTrue(
                "$name recovery failed: $result",
                result is KampalaGeoDiscoveryContract.Result.Success
            )

            val page = (
                result as KampalaGeoDiscoveryContract.Result.Success
            ).page

            assertEquals(
                "$name should make one root and four child requests",
                5,
                calls
            )

            assertEquals(
                "$name should visit five distinct tiles",
                5,
                tiles.distinct().size
            )

            assertEquals(1, page.places.size)
            assertNull(page.nextPageToken)

            val place = page.places.single()

            assertEquals("bank", place.category)

            assertTrue(
                KampalaGeoDiscoveryContract.withinBounds(
                    place.coordinates,
                    area.bounds
                )
            )

            assertTrue(
                KampalaGeoDiscoveryContract.validPlace(place)
            )

            assertTrue(
                KampalaGeoDiscoveryContract.validate(
                    request,
                    result
                ) is KampalaGeoDiscoveryContract.Result.Success
            )
        }
    }

    @Test
    fun incompleteChildAtDepthLimitNeverClaimsSuccess() = runBlocking {

        val root = KampalaGeoP2CandidateCatalog.areas
            .first { it.label == "Ntinda" }

        val request = KampalaGeoDiscoveryContract.SearchRequest(
            category = "bank",
            bounds = root.bounds,
            pageSize = 10
        )

        val body = fixture("Ntinda")
        var calls = 0

        val transport = object :
            KampalaJarvisGeoApiProvider.Transport {

            override suspend fun get(
                url: String
            ): KampalaJarvisGeoApiProvider.HttpResult {

                requestBounds(url)
                calls++

                return KampalaJarvisGeoApiProvider.HttpResult(
                    200,
                    body
                )
            }
        }

        val api = KampalaJarvisGeoApiProvider(
            "https://jarvis-uganda-geo-service.onrender.com",
            transport
        )

        val result = KampalaGeoSubdivisionProvider(api)
            .search(request)

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )

        assertEquals(
            "Depth-three subdivision should stop after 22 requests",
            22,
            calls
        )
    }
}