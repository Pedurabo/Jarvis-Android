package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class MapsHandoffA68ATest {

    private fun place(): KampalaSourceBackedPlacePolicy.Place =
        requireNotNull(
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Watoto Church Downtown"
            )
        )

    private fun journey(origin: String? = null) =
        KampalaDestinationJourney.fromPublishedPlace(
            place(),
            origin
        )

    @Test
    fun publishedDestinationAndOriginBuildMapsUrl() {
        val url = KampalaMapsHandoffPolicy.urlOrNull(
            journey("Ntinda")
        )

        assertNotNull(url)
        assertTrue(
            url!!.startsWith(
                "https://www.google.com/maps/dir/?api=1"
            )
        )
        assertTrue(url.contains("origin=Ntinda"))
        assertTrue(url.contains("destination="))
        assertTrue(url.contains("Watoto+Church+Downtown"))
        assertTrue(url.contains("Plot+87+Kampala+Road"))
    }

    @Test
    fun absentOriginCannotGenerateDirectionsUrl() {
        assertNull(
            KampalaMapsHandoffPolicy.urlOrNull(
                journey()
            )
        )
    }

    @Test
    fun originWhitespaceIsTrimmed() {
        val url = KampalaMapsHandoffPolicy.urlOrNull(
            place(),
            "  Ntinda  "
        )

        assertNotNull(url)
        assertTrue(url!!.contains("origin=Ntinda"))
    }

    @Test
    fun urlUsesMapsRatherThanClaimingCalculatedRoute() {
        val state = journey("Ntinda")
        val notice = state.originFollowUpNotice()

        assertFalse(state.routeCalculated)
        assertTrue(notice.contains("has not calculated"))
        assertTrue(
            notice.contains("google.com/maps/dir/")
        )
        assertFalse(notice.contains("Open directions:"))
    }

    @Test
    fun clarificationPreservesSourceAndUserChoice() {
        val notice =
            KampalaSourceClarificationResponsePolicy.respond(
                place(),
                "Ntinda"
            )

        assertTrue(notice.contains("watotochurch.com"))
        assertTrue(notice.contains("Plot 87"))
        assertTrue(
            notice.contains("Review route in Google Maps")
        )
        assertTrue(
            notice.contains("haven't calculated or checked")
        )
    }

    @Test
    fun clarificationWithoutOriginHasNoDirectionsLink() {
        val notice =
            KampalaSourceClarificationResponsePolicy.respond(
                place(),
                null
            )

        assertFalse(notice.contains("google.com/maps/dir/"))
        assertTrue(notice.contains("Watoto Church Downtown"))
    }

    @Test
    fun unrelatedPlaceCannotBorrowWatotoAddress() {
        assertNull(
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Unrelated Kampala Church Downtown"
            )
        )
    }

    @Test
    fun emergencyPolicyRemainsIntact() {
        val response =
            KampalaEmergencyResponsePolicy.respondOrNull(
                "I was bitten by a dog. What should I do?"
            )

        assertNotNull(response)
        assertTrue(response!!.contains("rabies", true))
    }
}