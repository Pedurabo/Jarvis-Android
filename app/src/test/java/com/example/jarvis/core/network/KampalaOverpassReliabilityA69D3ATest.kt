package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaOverpassReliabilityA69D3ATest {

    private val request =
        KampalaGeoDiscoveryContract.SearchRequest(
            category = "hospital",
            bounds = KampalaGeoDiscoveryContract.Bounds(
                south = 0.30,
                west = 32.55,
                north = 0.35,
                east = 32.60
            ),
            pageSize = 10
        )

    private fun element(
        id: Int,
        amenity: String = "hospital",
        named: Boolean = true
    ): String {
        val name = if (named) {
            """"name": "Example $id","""
        } else {
            ""
        }

        return """
          {
            "type": "node",
            "id": $id,
            "lat": 0.32,
            "lon": 32.57,
            "tags": {
              $name
              "amenity": "$amenity"
            }
          }
        """.trimIndent()
    }

    private fun json(elements: List<String>): String =
        """{"elements":[${elements.joinToString(",")}]}"""

    @Test
    fun providerRequestsExtraTruncationRecord() {
        val query =
            KampalaOverpassDiscoveryProvider.buildQuery(request)

        assertNotNull(query)
        assertTrue(query!!.contains("out center 11;"))
    }

    @Test
    fun extraRecordCausesIncompleteResultFailure() {
        val result =
            KampalaOverpassDiscoveryProvider.parse(
                json((1..11).map { element(it) }),
                request
            )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun tenValidRecordsRemainAccepted() {
        val result =
            KampalaOverpassDiscoveryProvider.parse(
                json((1..10).map { element(it) }),
                request
            )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Success
        )

        assertEquals(
            10,
            (result as KampalaGeoDiscoveryContract.Result.Success)
                .page.places.size
        )
    }

    @Test
    fun wrongOsmCategoryIsExcluded() {
        val result =
            KampalaOverpassDiscoveryProvider.parse(
                json(listOf(element(1, "school"))),
                request
            )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Success
        )

        assertTrue(
            (result as KampalaGeoDiscoveryContract.Result.Success)
                .page.places.isEmpty()
        )
    }

    @Test
    fun unnamedExtraRecordStillSignalsTruncation() {
        val result =
            KampalaOverpassDiscoveryProvider.parse(
                json((1..11).map { element(it, named = false) }),
                request
            )

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }
}