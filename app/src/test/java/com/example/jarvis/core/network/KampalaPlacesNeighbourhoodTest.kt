package com.example.jarvis.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KampalaPlacesNeighbourhoodTest {

    @Test
    fun restaurantsInKololo() {
        val result = KampalaPlacesQueryRoute.resolve(
            "Find restaurants in Kololo"
        )
        assertEquals("restaurant", result?.category)
        assertEquals("Kololo", result?.area)
    }

    @Test
    fun hotelsInNakasero() {
        val result = KampalaPlacesQueryRoute.resolve(
            "Find hotels in Nakasero"
        )
        assertEquals("accommodation", result?.category)
        assertEquals("Nakasero", result?.area)
    }

    @Test
    fun hospitalsInNtinda() {
        val result = KampalaPlacesQueryRoute.resolve(
            "Find hospitals in Ntinda"
        )
        assertEquals("healthcare", result?.category)
        assertEquals("Ntinda", result?.area)
    }

    @Test
    fun supermarketsInBugolobi() {
        val result = KampalaPlacesQueryRoute.resolve(
            "Find supermarkets in Bugolobi"
        )
        assertEquals("supermarket", result?.category)
        assertEquals("Bugolobi", result?.area)
    }

    @Test
    fun churchesInMakindye() {
        val result = KampalaPlacesQueryRoute.resolve(
            "Find churches in Makindye"
        )
        assertEquals("church", result?.category)
        assertEquals("Makindye", result?.area)
    }

    @Test
    fun oldKampalaStillWorks() {
        val result = KampalaPlacesQueryRoute.resolve(
            "Find places in Old Kampala"
        )
        assertEquals("Old Kampala", result?.area)
    }

    @Test
    fun shoppingInKisementiStillWorks() {
        val result = KampalaPlacesQueryRoute.resolve(
            "Find shopping places in Kisementi"
        )
        assertEquals("shopping", result?.category)
        assertEquals("Kisementi", result?.area)
    }

    @Test
    fun unknownNeighbourhoodIsNotInvented() {
        assertNull(
            KampalaPlacesQueryRoute.resolve(
                "Find restaurants in Unknown District"
            )
        )
    }

    @Test
    fun historyIsNotIntercepted() {
        assertNull(
            KampalaPlacesQueryRoute.resolve(
                "Tell me about Kampala history"
            )
        )
    }
}
