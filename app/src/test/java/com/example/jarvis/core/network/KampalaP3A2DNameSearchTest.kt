package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaP3A2DNameSearchTest {

    private val endpoint =
        "https://jarvis-uganda-geo-service.onrender.com/geo/search"

    // Width 0.09, deliberately below the backend 0.10 limit.
    private val bounds = KampalaGeoDiscoveryContract.Bounds(
        south = 0.30,
        west = 32.55,
        north = 0.35,
        east = 32.64
    )

    private fun request(name: String, category: String? = null) =
        KampalaGeoDiscoveryContract.SearchRequest(
            query = name,
            category = category,
            bounds = bounds,
            pageSize = 20
        )

    @Test
    fun nameSearchUsesSeparateEndpoint() {
        val url = KampalaJarvisGeoApiProvider.requestUrl(
            endpoint, request("Green Hill Academy")
        )
        assertNotNull("Valid bounded name search must produce a URL", url)
        assertTrue(url!!.contains("/geo/name-search?"))
        assertTrue(url.contains("query=Green+Hill+Academy"))
        assertTrue(url.contains("south=0.300000"))
        assertTrue(url.contains("east=32.640000"))
        assertFalse(url.contains("/geo/search?"))
    }

    @Test
    fun categorySearchRemainsOnExistingEndpoint() {
        val url = KampalaJarvisGeoApiProvider.requestUrl(
            endpoint,
            KampalaGeoDiscoveryContract.SearchRequest(
                category = "bank",
                bounds = bounds,
                pageSize = 20
            )
        )
        assertNotNull("Legacy category search must still produce a URL", url)
        assertTrue(url!!.startsWith("$endpoint?category=bank"))
        assertFalse(url.contains("name-search"))
    }

    @Test
    fun optionalCategoryIsPreserved() {
        val url = KampalaJarvisGeoApiProvider.requestUrl(
            endpoint, request("Green Hill Academy", "school")
        )
        assertNotNull("Bounded name/category search must produce a URL", url)
        assertTrue(url!!.contains("/geo/name-search?"))
        assertTrue(url.contains("category=school"))
    }

    @Test
    fun oversizedSearchBoundsAreRejected() {
        val url = KampalaJarvisGeoApiProvider.requestUrl(
            endpoint,
            KampalaGeoDiscoveryContract.SearchRequest(
                query = "Green Hill Academy",
                bounds = KampalaGeoDiscoveryContract.Bounds(
                    south = 0.10,
                    west = 32.0,
                    north = 0.50,
                    east = 32.5
                )
            )
        )
        assertNull(url)
    }

    @Test
    fun incompleteSearchCannotBecomeAConfirmedListing() {
        val json = """
            {
              "status":"INCOMPLETE",
              "places":[],
              "message":"More matching places exist; refine the name or category."
            }
        """.trimIndent()

        val result = KampalaJarvisGeoApiProvider.parse(
            json, request("Green Hill Academy")
        )
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun namedProviderRecordPreservesSourceIdentity() {
        val json = """
            {
              "status":"SUCCESS",
              "verification":"MAP_LISTINGS_ONLY",
              "snapshotGeneratedAt":"2026-10-25T21:21:21Z",
              "attribution":"(c) OpenStreetMap contributors",
              "places":[{
                "providerId":"node/613003713",
                "name":"Greenhill Academy",
                "category":"school",
                "latitude":0.3116979,
                "longitude":32.5995158,
                "publicAddress":null,
                "sourceUrl":"https://www.openstreetmap.org/node/613003713"
              }]
            }
        """.trimIndent()

        val result = KampalaJarvisGeoApiProvider.parse(
            json, request("Greenhill")
        )
        assertTrue(
            "Valid bounded OSM result must be accepted",
            result is KampalaGeoDiscoveryContract.Result.Success
        )
        val page = (result as KampalaGeoDiscoveryContract.Result.Success).page
        assertEquals(1, page.places.size)
        assertEquals("Greenhill Academy", page.places[0].name)
        assertEquals("node/613003713", page.places[0].providerId)
    }

    @Test
    fun distantCampusIsRejected() {
        val json = """
            {
              "status":"SUCCESS",
              "verification":"MAP_LISTINGS_ONLY",
              "snapshotGeneratedAt":"2026-10-25T21:21:21Z",
              "attribution":"(c) OpenStreetMap contributors",
              "places":[{
                "providerId":"node/4035743517",
                "name":"Green Hill Academy",
                "category":"school",
                "latitude":0.3514044,
                "longitude":32.7709164,
                "publicAddress":null,
                "sourceUrl":"https://www.openstreetmap.org/node/4035743517"
              }]
            }
        """.trimIndent()

        val result = KampalaJarvisGeoApiProvider.parse(
            json, request("Green Hill Academy")
        )
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun invalidShortNameIsRejected() {
        val url = KampalaJarvisGeoApiProvider.requestUrl(
            endpoint, request("a")
        )
        assertNull(url)
    }
}