package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class SourceBackedPlaceA66D2Test {

    @Test
    fun officialCampusResolvesToPublishedAddress() {
        val place = KampalaSourceBackedPlacePolicy.resolveOrNull(
            "Watoto Church Downtown"
        )

        assertNotNull(place)
        assertEquals(
            "Plot 87 Kampala Road, Kampala",
            place!!.address
        )
        assertEquals(
            "https://watotochurch.com/locations/",
            place.sourceUrl
        )
    }

    @Test
    fun existingDowntownAreaIsNotDuplicated() {
        assertEquals(
            "Watoto Church Downtown",
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church Downtown",
                    "the Downtown branch"
                )
        )
    }

    @Test
    fun missingDowntownAreaCanBeClarified() {
        assertEquals(
            "Watoto Church Downtown",
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church",
                    "the Downtown branch"
                )
        )
    }

    @Test
    fun otherPlacesRetainTheirClarification() {
        assertEquals(
            "Example Library Downtown branch",
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Example Library",
                    "the Downtown branch"
                )
        )
    }

    @Test
    fun unspecifiedWatotoCampusIsNotVerified() {
        assertNull(
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Watoto Church"
            )
        )
    }

    @Test
    fun unrelatedPlaceCannotBorrowWatotoAddress() {
        assertNull(
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Other Church Downtown"
            )
        )
    }

    @Test
    fun outputNamesTheOfficialSourceAndLimitation() {
        val notice = KampalaSourceBackedPlacePolicy
            .sourceNoticeOrNull("Watoto Church Downtown")

        assertNotNull(notice)
        assertTrue(notice!!.contains("Plot 87"))
        assertTrue(notice.contains("watotochurch.com"))
        assertTrue(notice.contains("not a calculated"))
    }

    @Test
    fun originCorrectionIsPreserved() {
        assertEquals(
            "ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "actually, am starting from ntinda"
            )
        )
    }

    @Test
    fun dogBiteEmergencyStillWorks() {
        val response = KampalaEmergencyResponsePolicy.respondOrNull(
            "I was bitten by a dog. What should I do?"
        )

        assertNotNull(response)
        assertTrue(response!!.contains("rabies", true))
        assertTrue(response.contains("soap", true))
    }
}