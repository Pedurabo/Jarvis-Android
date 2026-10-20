package com.example.jarvis.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaPlacesResultPolicyTest {

    private fun emptyResult(
        category: String? = null,
        area: String? = null,
        query: String? = null
    ): String {
        val request = KampalaPlacesQueryRoute.SearchRequest(
            query = query,
            area = area,
            category = category
        )

        return KampalaPlacesResultPolicy.format(
            request,
            KampalaPlacesRepository.SearchResult.Success(emptyList())
        )
    }

    @Test
    fun restaurantNoticeNamesKololo() {
        val message = emptyResult(
            category = "restaurant",
            area = "Kololo"
        )

        assertTrue(message.contains("restaurant in Kololo"))
        assertTrue(message.contains("current Jarvis Kampala directory"))
        assertTrue(message.contains("does not mean"))
    }

    @Test
    fun accommodationNoticeNamesNakasero() {
        val message = emptyResult(
            category = "accommodation",
            area = "Nakasero"
        )

        assertTrue(message.contains("accommodation in Nakasero"))
    }

    @Test
    fun citywideSearchUsesKampala() {
        val message = emptyResult(category = "healthcare")
        assertTrue(message.contains("healthcare in Kampala"))
    }

    @Test
    fun areaOnlySearchUsesPlaces() {
        val message = emptyResult(area = "Ntinda")
        assertTrue(message.contains("places in Ntinda"))
    }

    @Test
    fun emptyResultsDoNotClaimAvailability() {
        val message = emptyResult(
            category = "restaurant",
            area = "Bugolobi"
        )

        assertTrue(message.contains("availability have been assumed"))
        assertFalse(message.contains("All restaurants are closed"))
    }

    @Test
    fun serviceFailuresPreserveFailureMessage() {
        val failure = "The Kampala places service is unavailable."

        val message = KampalaPlacesResultPolicy.format(
            KampalaPlacesQueryRoute.SearchRequest(
                area = "Makindye",
                category = "healthcare"
            ),
            KampalaPlacesRepository.SearchResult.Failure(failure)
        )

        assertEquals(failure, message)
    }

    @Test
    fun successfulPlacePreservesExistingFormatting() {
        val place = KampalaPlace(
            id = "test-place",
            name = "Example Verified Place",
            category = "shopping",
            area = "Kisementi",
            address = "Example address",
            sourceUrl = "https://example.org/place",
            verifiedAt = "2026-10-19"
        )

        val result = KampalaPlacesRepository.SearchResult.Success(
            listOf(place)
        )

        val request = KampalaPlacesQueryRoute.SearchRequest(
            area = "Kisementi",
            category = "shopping"
        )

        assertEquals(
            KampalaPlacesResponseFormatter.format(result),
            KampalaPlacesResultPolicy.format(request, result)
        )
    }
}
