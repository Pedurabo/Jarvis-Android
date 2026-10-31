package com.example.jarvis.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KampalaGeoIncompleteD12CTest {

    private val request = KampalaGeoDiscoveryContract.SearchRequest(
        category = "hospital",
        bounds = KampalaGeoDiscoveryContract.Bounds(
            0.300, 32.550, 0.350, 32.600
        ),
        pageSize = 10
    )

    @Test
    fun backendIncompleteHasTypedClassification() {
        val json = """
            {
              "status": "INCOMPLETE",
              "places": [],
              "message": "Narrow the search area."
            }
        """.trimIndent()

        val result = KampalaJarvisGeoApiProvider.parse(json, request)

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )

        assertEquals(
            KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE,
            (result as KampalaGeoDiscoveryContract.Result.Unavailable).kind
        )
    }

    @Test
    fun malformedResponseCannotTriggerSubdivision() {
        val json = """
            {
              "status": "BROKEN",
              "places": [],
              "message": "Narrow the search area."
            }
        """.trimIndent()

        val result = KampalaJarvisGeoApiProvider.parse(json, request)

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )

        assertEquals(
            KampalaGeoDiscoveryContract.Result.Kind.OTHER,
            (result as KampalaGeoDiscoveryContract.Result.Unavailable).kind
        )
    }

    @Test
    fun unclassifiedFailureNeverSubdivides() = runBlocking {
        var calls = 0

        val delegate = object : KampalaGeoDiscoveryContract.Provider {
            override suspend fun search(
                request: KampalaGeoDiscoveryContract.SearchRequest
            ): KampalaGeoDiscoveryContract.Result {
                calls++
                return KampalaGeoDiscoveryContract.Result.Unavailable(
                    "An invalid response containing the word incomplete."
                )
            }
        }

        val result = KampalaGeoSubdivisionProvider(delegate).search(request)

        assertEquals(1, calls)
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }
}