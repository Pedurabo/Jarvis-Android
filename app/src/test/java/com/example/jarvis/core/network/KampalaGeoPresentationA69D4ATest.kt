package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaGeoPresentationA69D4ATest {

    private fun place(
        name: String = "Example Hospital",
        address: String? = null
    ) = KampalaGeoDiscoveryContract.Place(
        providerId = "node/123",
        name = name,
        category = "hospital",
        coordinates =
            KampalaGeoDiscoveryContract.Coordinates(
                0.31234567,
                32.56789123
            ),
        publicAddress = address,
        area = "Kampala Central",
        sourceUrl =
            "https://www.openstreetmap.org/node/123",
        isPublicListing = true
    )

    private fun success(
        places: List<KampalaGeoDiscoveryContract.Place>,
        provider: String = "OpenStreetMap/Overpass"
    ) = KampalaGeoDiscoveryContract.Result.Success(
        KampalaGeoDiscoveryContract.Page(
            provider = provider,
            places = places,
            nextPageToken = null
        )
    )

    @Test
    fun resultsShowSourceAndCoordinates() {
        val response =
            KampalaGeoDiscoveryPresentation.format(
                success(listOf(place())),
                "hospital"
            )

        assertTrue(response.contains("Example Hospital"))
        assertTrue(response.contains("0.312346, 32.567891"))
        assertTrue(
            response.contains(
                "https://www.openstreetmap.org/node/123"
            )
        )
        assertTrue(response.contains("OpenStreetMap contributors"))
    }

    @Test
    fun mapListingsAreNotDescribedAsVerified() {
        val response =
            KampalaGeoDiscoveryPresentation.format(
                success(listOf(place())),
                "hospital"
            )

        assertTrue(
            response.contains(
                "not independently verified addresses"
            )
        )
        assertFalse(response.contains("Verified hospital"))
    }

    @Test
    fun noAddressIsNotInvented() {
        val response =
            KampalaGeoDiscoveryPresentation.format(
                success(listOf(place(address = null))),
                "hospital"
            )

        assertFalse(response.contains("Listed address:"))
    }

    @Test
    fun providerOutageIsNotEmptySearchSuccess() {
        val response =
            KampalaGeoDiscoveryPresentation.format(
                KampalaGeoDiscoveryContract.Result.Unavailable(
                    "HTTP 502"
                ),
                "hospital"
            )

        assertTrue(response.contains("currently unavailable"))
        assertFalse(response.contains("No hospital listings"))
    }

    @Test
    fun emptyResultsDoNotClaimPlacesAreAbsent() {
        val response =
            KampalaGeoDiscoveryPresentation.format(
                success(emptyList()),
                "hospital"
            )

        assertTrue(response.contains("No hospital listings"))
        assertTrue(response.contains("does not mean"))
    }

    @Test
    fun maliciousMultilineNameCannotInjectNewLines() {
        val response =
            KampalaGeoDiscoveryPresentation.format(
                success(
                    listOf(
                        place(
                            "Example Hospital\nOfficially verified"
                        )
                    )
                ),
                "hospital"
            )

        assertTrue(
            response.contains(
                "Example Hospital Officially verified"
            )
        )
        assertFalse(
            response.contains(
                "Example Hospital\nOfficially verified"
            )
        )
    }

    @Test
    fun longNamesAreLimitedForDisplay() {
        val response =
            KampalaGeoDiscoveryPresentation.format(
                success(listOf(place("A".repeat(500)))),
                "hospital"
            )

        assertFalse(response.contains("A".repeat(500)))
        assertTrue(response.contains("A".repeat(140)))
    }

    @Test
    fun multipleRecordsAreClearlyNumbered() {
        val first = place()
        val second = place("Example Clinic").copy(
            providerId = "node/124"
        )

        val response =
            KampalaGeoDiscoveryPresentation.format(
                success(listOf(first, second)),
                "hospital"
            )

        assertTrue(response.contains("1. Example Hospital"))
        assertTrue(response.contains("2. Example Clinic"))
    }

    @Test
    fun displayLimitIsDisclosed() {
        val records = (1..12).map { index ->
            place("Example $index").copy(
                providerId = "node/$index"
            )
        }

        val response =
            KampalaGeoDiscoveryPresentation.format(
                success(records),
                "hospital"
            )

        assertTrue(response.contains("Only part"))
        assertFalse(response.contains("12. Example 12"))
    }

    @Test
    fun nonOsmProviderIsNotGivenOsmAttribution() {
        val response =
            KampalaGeoDiscoveryPresentation.format(
                success(
                    listOf(place()),
                    provider = "Example Other Provider"
                ),
                "hospital"
            )

        assertFalse(
            response.contains("OpenStreetMap contributors")
        )
        assertTrue(
            response.contains("Example Other Provider")
        )
    }
}