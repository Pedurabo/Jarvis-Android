package com.example.jarvis.ui

import org.junit.Assert.*
import org.junit.Test

class MapsActionPresentationA68B2Test {

    private val mapsUrl =
        "https://www.google.com/maps/dir/" +
            "?api=1&origin=Ntinda&destination=" +
            "Watoto+Church+Downtown%2C+Plot+87+Kampala+Road"

    private fun response() =
        "You're starting from Ntinda and heading to " +
            "Watoto Church Downtown. " +
            "Official source: https://watotochurch.com/locations/. " +
            "Jarvis has not calculated or checked a route. " +
            "Review route in Google Maps (tap to open): " +
            mapsUrl

    @Test
    fun compactActionExtractsGeneratedMapLink() {
        val action =
            JarvisMapsActionPresentation.parseOrNull(response())

        assertNotNull(action)
        assertEquals(mapsUrl, action!!.url)
        assertEquals(
            "Review route in Google Maps",
            action.label
        )
    }

    @Test
    fun visibleTextRetainsOfficialSource() {
        val action =
            JarvisMapsActionPresentation.parseOrNull(response())

        assertNotNull(action)
        assertTrue(
            action!!.visibleText.contains(
                "https://watotochurch.com/locations/"
            )
        )
        assertTrue(
            action.visibleText.contains(
                "has not calculated or checked a route"
            )
        )
    }

    @Test
    fun rawMapsUrlIsRemovedFromVisibleProse() {
        val action =
            JarvisMapsActionPresentation.parseOrNull(response())

        assertNotNull(action)
        assertFalse(
            action!!.visibleText.contains(
                "google.com/maps/dir/"
            )
        )
    }

    @Test
    fun sourceOnlyAnswerHasNoRouteAction() {
        assertNull(
            JarvisMapsActionPresentation.parseOrNull(
                "Official source: " +
                    "https://watotochurch.com/locations/"
            )
        )
    }

    @Test
    fun arbitraryMapsLinkIsNotPromoted() {
        assertNull(
            JarvisMapsActionPresentation.parseOrNull(
                "Try this link: $mapsUrl"
            )
        )
    }

    @Test
    fun nonGoogleHostIsRejected() {
        val tampered = response().replace(
            "www.google.com",
            "www.example.com"
        )

        assertNull(
            JarvisMapsActionPresentation.parseOrNull(
                tampered
            )
        )
    }

    @Test
    fun incompleteRouteParametersAreRejected() {
        val incomplete = response().replace(
            "&destination=",
            "&missingDestination="
        )

        assertNull(
            JarvisMapsActionPresentation.parseOrNull(
                incomplete
            )
        )
    }

    @Test
    fun trailingUnrelatedTextIsNotConsumed() {
        assertNull(
            JarvisMapsActionPresentation.parseOrNull(
                response() + " and something else"
            )
        )
    }

    @Test
    fun noNavigationIsInferredFromOrdinaryDiscussion() {
        assertNull(
            JarvisMapsActionPresentation.parseOrNull(
                "Explain Google Maps integration in Android."
            )
        )
    }
}