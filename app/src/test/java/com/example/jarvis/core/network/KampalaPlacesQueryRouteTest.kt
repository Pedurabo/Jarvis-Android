package com.example.jarvis.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KampalaPlacesQueryRouteTest {

    @Test
    fun shoppingInKisementi() {
        assertEquals(
            KampalaPlacesQueryRoute.SearchRequest(
                area = "Kisementi",
                category = "shopping"
            ),
            KampalaPlacesQueryRoute.resolve("Find shopping places in Kisementi")
        )
    }

    @Test
    fun mosqueSearch() {
        assertEquals(
            KampalaPlacesQueryRoute.SearchRequest(query = "mosque"),
            KampalaPlacesQueryRoute.resolve("Find a mosque")
        )
    }

    @Test
    fun namedDestination() {
        assertEquals(
            KampalaPlacesQueryRoute.SearchRequest(query = "Acacia Mall"),
            KampalaPlacesQueryRoute.resolve("Find Acacia Mall")
        )
    }

    @Test
    fun recognizesOldKampalaArea() {
        assertEquals(
            KampalaPlacesQueryRoute.SearchRequest(
                query = "mosque",
                area = "Old Kampala"
            ),
            KampalaPlacesQueryRoute.resolve("Find a mosque in Old Kampala")
        )
    }

    @Test
    fun doesNotRouteGeneralQuestions() {
        assertNull(
            KampalaPlacesQueryRoute.resolve("Tell me the history of Kampala")
        )
        assertNull(
            KampalaPlacesQueryRoute.resolve("What is a mosque?")
        )
        assertNull(
            KampalaPlacesQueryRoute.resolve("Call Acacia Mall")
        )
        assertNull(
            KampalaPlacesQueryRoute.resolve("Find places")
        )
        assertNull(
            KampalaPlacesQueryRoute.resolve("Find a pharmacy in Ntinda")
        )
    }
}