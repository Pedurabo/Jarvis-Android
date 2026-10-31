package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class KampalaGeoP0FullSubdivisionTest {

    private fun resource(name: String): String {
        val stream = javaClass.classLoader
            ?.getResourceAsStream(name)
            ?: error("Missing fixture: $name")

        return stream.bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
    }

    private fun manifest(): Map<String, Int> =
        resource("geo-p0-full-subdivision/manifest.txt")
            .lineSequence()
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0] to parts[1].toInt()
            }

    private class FixtureTransport(
        private val load: (String) -> String
    ) : KampalaJarvisGeoApiProvider.Transport {

        val visited = mutableListOf<String>()

        override suspend fun get(
            url: String
        ): KampalaJarvisGeoApiProvider.HttpResult {

            val uri = URI(url)

            assertEquals("/geo/search", uri.path)

            val query = uri.rawQuery
                .split("&")
                .associate {
                    val parts = it.split("=", limit = 2)
                    parts[0] to parts[1]
                }

            assertEquals("hospital", query["category"])
            assertEquals("10", query["limit"])

            val names = listOf(
                "south",
                "west",
                "north",
                "east"
            )

            val key = names.joinToString("_") {
                query[it] ?: error("Missing coordinate: $it")
            }

            assertFalse(
                "Duplicate tile request: $key",
                visited.contains(key)
            )

            visited.add(key)

            return KampalaJarvisGeoApiProvider.HttpResult(
                200,
                load("geo-p0-full-subdivision/$key.json")
            )
        }
    }

    @Test
    fun entireRealSubdivisionProducesValidatedListings() = runBlocking {
        val captured = manifest()

        val transport = FixtureTransport { name ->
            resource(name)
        }

        val api = KampalaJarvisGeoApiProvider(
            "https://jarvis-uganda-geo-service.onrender.com",
            transport
        )

        val subdivision = KampalaGeoSubdivisionProvider(api)

        val request = KampalaExplicitGeoDiscovery.parseOrNull(
            "find hospitals in central kampala"
        )!!.toRequest()

        val result = subdivision.search(request)

        assertTrue(
            "Full real-backend fixture search failed: $result",
            result is KampalaGeoDiscoveryContract.Result.Success
        )

        val page = (
            result as KampalaGeoDiscoveryContract.Result.Success
        ).page

        assertEquals(
            captured["requests"],
            transport.visited.size
        )

        assertTrue(page.places.isNotEmpty())
        assertEquals(10, page.places.size)
        assertNull(page.nextPageToken)

        assertTrue(page.provider.contains("OpenStreetMap"))
        assertTrue(page.provider.contains("showing 10 of"))

        val ids = page.places.map { it.providerId }

        assertEquals(ids.size, ids.distinct().size)

        for (place in page.places) {
            assertTrue(
                KampalaGeoDiscoveryContract.validPlace(place)
            )

            assertTrue(
                KampalaGeoDiscoveryContract.withinBounds(
                    place.coordinates,
                    request.bounds
                )
            )
        }

        val validated = KampalaGeoDiscoveryContract.validate(
            request,
            result
        )

        assertTrue(
            validated is KampalaGeoDiscoveryContract.Result.Success
        )

        val formatted = KampalaGeoDiscoveryPresentation.format(
            validated,
            "hospital"
        )

        assertTrue(formatted.contains("Geographic place discovery"))
        assertTrue(formatted.contains("Map record:"))
        assertFalse(
            formatted.contains(
                "Geographic discovery is currently unavailable"
            )
        )
    }
}