package com.example.jarvis.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KampalaPlacesDiscoveryExpansionTest {

    @Test
    fun existingShoppingRouteRemainsAvailable() {
        val request = KampalaPlacesQueryRoute.resolve(
            "Find shopping places in Kisementi"
        )
        assertEquals("shopping", request?.category)
        assertEquals("Kisementi", request?.area)
    }

    @Test
    fun existingMosqueRouteRemainsAvailable() {
        val request = KampalaPlacesQueryRoute.resolve(
            "Find a mosque in Old Kampala"
        )
        assertEquals("mosque", request?.query)
        assertEquals("Old Kampala", request?.area)
    }

    @Test
    fun existingNamedPlaceRouteRemainsAvailable() {
        val request = KampalaPlacesQueryRoute.resolve("Find Acacia Mall")
        assertEquals("Acacia Mall", request?.query)
    }

    @Test
    fun supportsRestaurantDiscovery() {
        val request = KampalaPlacesQueryRoute.resolve(
            "Find restaurants in Kampala"
        )
        assertEquals("restaurant", request?.category)
        assertNull(request?.area)
    }

    @Test
    fun supportsAccommodationDiscovery() {
        val request = KampalaPlacesQueryRoute.resolve(
            "Show me hotels in Kampala"
        )
        assertEquals("accommodation", request?.category)
    }

    @Test
    fun supportsHealthcareDiscovery() {
        val request = KampalaPlacesQueryRoute.resolve(
            "Find hospitals in Kampala"
        )
        assertEquals("healthcare", request?.category)
    }

    @Test
    fun supportsMarketDiscovery() {
        val request = KampalaPlacesQueryRoute.resolve(
            "Find markets in Kampala"
        )
        assertEquals("market", request?.category)
    }

    @Test
    fun supportsNeighbourhoodFiltering() {
        val request = KampalaPlacesQueryRoute.resolve(
            "Find cafes in Kisementi"
        )
        assertEquals("cafe", request?.category)
        assertEquals("Kisementi", request?.area)
    }

    @Test
    fun supportsCivicDiscovery() {
        val request = KampalaPlacesQueryRoute.resolve(
            "Find government offices in Kampala"
        )
        assertEquals("government", request?.category)
    }

    @Test
    fun supportsChurchDiscovery() {
        val request = KampalaPlacesQueryRoute.resolve(
            "Find churches in Kampala"
        )
        assertEquals("church", request?.category)
    }

    @Test
    fun doesNotRouteBookingInstructionsAsSearches() {
        assertNull(
            KampalaPlacesQueryRoute.resolve("Book a hotel in Kampala")
        )
    }

    @Test
    fun doesNotInventUnknownNeighbourhoods() {
        assertNull(
            KampalaPlacesQueryRoute.resolve(
                "Find restaurants in an unknown district"
            )
        )
    }

    @Test
    fun doesNotInterceptKnowledgeQuestions() {
        assertNull(
            KampalaPlacesQueryRoute.resolve(
                "Tell me about Kampala history"
            )
        )
    }
}
