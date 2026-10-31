package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class KampalaPlaceResolutionA69BTest {

    private fun record(
        id: String = "downtown",
        name: String = "Watoto Church Downtown",
        area: String = "Central Kampala",
        provider: String = "published-source",
        sourceUrl: String = "https://watotochurch.com/locations/",
        checkedOn: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) = KampalaPlaceResolutionPolicy.PlaceRecord(
        provider = provider,
        providerId = id,
        name = name,
        category = "place_of_worship",
        area = area,
        address = "Plot 87 Kampala Road, Kampala",
        sourceUrl = sourceUrl,
        sourceKind =
            KampalaPlaceResolutionPolicy.SourceKind.OFFICIAL_PUBLICATION,
        sourceCheckedOn = checkedOn,
        latitude = latitude,
        longitude = longitude
    )

    @Test
    fun uniqueExactNameResolvesWithoutInferringRoute() {
        val result =
            KampalaPlaceResolutionPolicy.resolveExact(
                "Watoto Church Downtown",
                listOf(record())
            )

        assertTrue(
            result is KampalaPlaceResolutionPolicy.Resolution.Exact
        )

        val place =
            (result as KampalaPlaceResolutionPolicy.Resolution.Exact).place

        assertEquals("downtown", place.providerId)
        assertNull(place.latitude)
        assertNull(place.longitude)
    }

    @Test
    fun punctuationAndCaseAreNormalized() {
        val result =
            KampalaPlaceResolutionPolicy.resolveExact(
                "WATOTO CHURCH, DOWNTOWN!",
                listOf(record())
            )

        assertTrue(
            result is KampalaPlaceResolutionPolicy.Resolution.Exact
        )
    }

    @Test
    fun differentBranchesMustRemainAmbiguous() {
        val results = listOf(
            record(id = "branch-one", name = "Example Bank"),
            record(id = "branch-two", name = "Example Bank")
        )

        val result =
            KampalaPlaceResolutionPolicy.resolveExact(
                "Example Bank",
                results
            )

        assertTrue(
            result is KampalaPlaceResolutionPolicy.Resolution.Ambiguous
        )

        assertEquals(
            2,
            (result as KampalaPlaceResolutionPolicy.Resolution.Ambiguous)
                .places.size
        )
    }

    @Test
    fun duplicateProviderRecordIsNotAnotherBranch() {
        val same = record()

        val result =
            KampalaPlaceResolutionPolicy.resolveExact(
                same.name,
                listOf(same, same)
            )

        assertTrue(
            result is KampalaPlaceResolutionPolicy.Resolution.Exact
        )
    }

    @Test
    fun areaFilterDisambiguatesMatchingNames() {
        val results = listOf(
            record(
                id = "one",
                name = "Example Hospital",
                area = "Kampala Central"
            ),
            record(
                id = "two",
                name = "Example Hospital",
                area = "Nakawa"
            )
        )

        val result =
            KampalaPlaceResolutionPolicy.resolveExact(
                requestedName = "Example Hospital",
                records = results,
                requestedArea = "Nakawa"
            )

        assertTrue(
            result is KampalaPlaceResolutionPolicy.Resolution.Exact
        )

        assertEquals(
            "two",
            (result as KampalaPlaceResolutionPolicy.Resolution.Exact)
                .place.providerId
        )
    }

    @Test
    fun invalidSourceCannotBecomeDestination() {
        val result =
            KampalaPlaceResolutionPolicy.resolveExact(
                "Watoto Church Downtown",
                listOf(
                    record(
                        sourceUrl = "http://untrusted.example/location"
                    )
                )
            )

        assertTrue(
            result is KampalaPlaceResolutionPolicy.Resolution.NotFound
        )
    }

    @Test
    fun futureSourceDateIsRejected() {
        assertFalse(
            KampalaPlaceResolutionPolicy.isUsable(
                record(checkedOn = "2999-01-01")
            )
        )
    }

    @Test
    fun incompleteGpsPairIsRejected() {
        assertFalse(
            KampalaPlaceResolutionPolicy.isUsable(
                record(latitude = 0.3)
            )
        )
    }

    @Test
    fun impossibleCoordinatesAreRejected() {
        assertFalse(
            KampalaPlaceResolutionPolicy.isUsable(
                record(
                    latitude = 110.0,
                    longitude = 32.0
                )
            )
        )
    }

    @Test
    fun validCoordinatesAreOnlyDataNotVerification() {
        val place = record(
            latitude = 0.3,
            longitude = 32.6
        )

        assertTrue(
            KampalaPlaceResolutionPolicy.isUsable(place)
        )

        assertEquals(
            KampalaPlaceResolutionPolicy.SourceKind.OFFICIAL_PUBLICATION,
            place.sourceKind
        )
    }

    @Test
    fun unknownLocationIsNotInvented() {
        val result =
            KampalaPlaceResolutionPolicy.resolveExact(
                "Nonexistent Example Landmark",
                listOf(record())
            )

        assertTrue(
            result is KampalaPlaceResolutionPolicy.Resolution.NotFound
        )
    }
}