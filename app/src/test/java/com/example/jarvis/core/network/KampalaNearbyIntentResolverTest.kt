package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaNearbyIntentResolverTest {

    private val backendCategories = setOf(
        "hospital",
        "clinic",
        "pharmacy",
        "school",
        "university",
        "bank",
        "fuel_station",
        "hotel",
        "guest_house",
        "police",
        "embassy",
        "apartment",
        "attraction",
        "bar",
        "cafe",
        "convenience_shop",
        "department_store",
        "fast_food",
        "fitness_centre",
        "hostel",
        "motel",
        "nightclub",
        "pub",
        "restaurant",
        "shopping_mall",
        "sports_centre",
        "supermarket"
    )

    @Test
    fun explicitNearbyRequestsResolveCorrectCategories() {
        val cases = mapOf(
            "find pharmacies near me" to "pharmacy",
            "show nearby hotels" to "hotel",
            "show me hospitals nearby" to "hospital",
            "locate fuel stations close to me" to "fuel_station",
            "find clinics around me" to "clinic",
            "find banks near my current location" to "bank",
            "please find guest houses near me" to "guest_house",
            "show nearest supermarkets" to "supermarket",
            "find convenience stores near me" to "convenience_shop",
            "find fitness centres near me" to "fitness_centre",
            "find department stores near me" to "department_store",
            "find cafés near me" to "cafe"
        )

        for ((query, expectedCategory) in cases) {
            val result =
                KampalaNearbyIntentResolver.parseOrNull(query)

            assertNotNull(query, result)
            assertEquals(query, expectedCategory, result!!.category)
        }
    }

    @Test
    fun everyResolvedCategoryIsSupportedByBackend() {
        val phrases = listOf(
            "hospitals", "clinics", "pharmacies",
            "schools", "universities", "banks",
            "fuel stations", "hotels", "guest houses",
            "police stations", "embassies", "apartments",
            "attractions", "bars", "cafes",
            "convenience shops", "department stores",
            "fast food", "fitness centres", "hostels",
            "motels", "nightclubs", "pubs",
            "restaurants", "shopping malls",
            "sports centres", "supermarkets"
        )

        for (phrase in phrases) {
            val result =
                KampalaNearbyIntentResolver.parseOrNull(
                    "find $phrase near me"
                )

            assertNotNull(phrase, result)
            assertTrue(
                "$phrase resolved to unsupported category ${result!!.category}",
                result.category in backendCategories
            )
        }
    }

    @Test
    fun namedAreaRequestsDoNotTriggerNearbyIntent() {
        val queries = listOf(
            "find hospitals in Ntinda",
            "find pharmacies in Central Kampala",
            "show hotels in Nakawa",
            "find banks in Rubaga"
        )

        for (query in queries) {
            assertNull(
                query,
                KampalaNearbyIntentResolver.parseOrNull(query)
            )
        }
    }

    @Test
    fun KampalaWideRequestsDoNotTriggerNearbyIntent() {
        val queries = listOf(
            "find restaurants across Kampala",
            "show hospitals throughout Kampala",
            "find hotels in Kampala",
            "find pharmacies across kampala"
        )

        for (query in queries) {
            assertNull(
                query,
                KampalaNearbyIntentResolver.parseOrNull(query)
            )
        }
    }

    @Test
    fun ambiguousOrUnrelatedRequestsDoNotTriggerNearbyIntent() {
        val queries = listOf(
            "find something nearby",
            "what is nearby?",
            "nearby",
            "find a nearby service",
            "explain Kotlin coroutines",
            "call the police",
            "I need an ambulance",
            "",
            "   "
        )

        for (query in queries) {
            assertNull(
                query,
                KampalaNearbyIntentResolver.parseOrNull(query)
            )
        }
    }

    @Test
    fun punctuationAndWhitespaceAreNormalized() {
        val result =
            KampalaNearbyIntentResolver.parseOrNull(
                "  FIND   PHARMACIES   NEAR ME?  "
            )

        assertNotNull(result)
        assertEquals("pharmacy", result!!.category)
    }
    @Test
    fun nearbyBoundsAreValidAndWithinProviderDimensionLimits() {
        val bounds = KampalaExplicitGeoDiscovery.nearbyBoundsOrNull(
            latitude = 0.3476,
            longitude = 32.5825
        )

        assertNotNull(bounds)
        assertTrue(KampalaGeoDiscoveryContract.validBounds(bounds!!))
        assertTrue(bounds.north - bounds.south <= 0.10)
        assertTrue(bounds.east - bounds.west <= 0.10)
        assertTrue(0.3476 in bounds.south..bounds.north)
        assertTrue(32.5825 in bounds.west..bounds.east)
    }

    @Test
    fun nearbyBoundsRejectInvalidCoordinates() {
        assertNull(KampalaExplicitGeoDiscovery.nearbyBoundsOrNull(Double.NaN, 32.5))
        assertNull(KampalaExplicitGeoDiscovery.nearbyBoundsOrNull(91.0, 32.5))
        assertNull(KampalaExplicitGeoDiscovery.nearbyBoundsOrNull(0.3, 181.0))
    }

}
