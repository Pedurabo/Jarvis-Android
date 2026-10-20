package com.example.jarvis.core.ai

import com.example.jarvis.core.network.KampalaPlacesQueryRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M1F-I3C.120L.2
 *
 * Regression protection for Kampala neighbourhood discovery.
 *
 * These tests do not connect to a server or perform actions.
 * They check the routing and fallback policy contracts.
 */
class KampalaLocationReliabilityRegressionTest {

    private fun assertDirectoryRoute(
        input: String,
        expectedArea: String,
        expectedCategory: String?
    ) {
        val request = KampalaPlacesQueryRoute.resolve(input)

        assertNotNull("Expected directory route: $input", request)
        assertEquals(expectedArea, request?.area)

        if (expectedCategory != null) {
            assertEquals(expectedCategory, request?.category)
        }

        assertNull(
            "Valid location must not produce a fallback notice: $input",
            KampalaLocationFallback.notice(input)
        )
    }

    @Test
    fun validNeighbourhoodsRetainCorrectFilters() {
        assertDirectoryRoute(
            "Find restaurants in Kololo",
            "Kololo",
            "restaurant"
        )

        assertDirectoryRoute(
            "Find hotels in Nakasero",
            "Nakasero",
            "accommodation"
        )

        assertDirectoryRoute(
            "Find places in Ntinda",
            "Ntinda",
            null
        )

        assertDirectoryRoute(
            "Find shopping centers in Bugolobi",
            "Bugolobi",
            "shopping"
        )

        assertDirectoryRoute(
            "Find hospitals in Makindye",
            "Makindye",
            "healthcare"
        )
    }

    @Test
    fun existingNeighbourhoodsArePreserved() {
        assertDirectoryRoute(
            "Find shopping places in Kisementi",
            "Kisementi",
            "shopping"
        )

        assertDirectoryRoute(
            "Find places in Old Kampala",
            "Old Kampala",
            null
        )
    }

    @Test
    fun knownMisspellingsGetSuggestionsWithoutSearches() {
        val cases = listOf(
            "find places in ntingd" to "Ntinda",
            "Find hotels in ntindaa" to "Ntinda",
            "Find restaurants in kolollo" to "Kololo",
            "Find shopping centers in bugolobii" to "Bugolobi",
            "Find hospitals in nakesero" to "Nakasero"
        )

        cases.forEach { (input, suggestion) ->
            assertNull(
                "Misspelling must not become a directory search: $input",
                KampalaPlacesQueryRoute.resolve(input)
            )

            val notice = KampalaLocationFallback.notice(input)

            assertNotNull(
                "Misspelling must produce a safe notice: $input",
                notice
            )

            assertTrue(
                "Expected suggested neighbourhood $suggestion",
                notice!!.contains("Did you mean $suggestion?")
            )

            assertTrue(
                "Jarvis must disclose that it did not search another area",
                notice.contains(
                    "haven't searched a different location automatically"
                )
            )
        }
    }

    @Test
    fun unsupportedAreasGetHonestNotices() {
        val input = "Find restaurants in Unknown District"

        assertNull(KampalaPlacesQueryRoute.resolve(input))

        val notice = requireNotNull(
            KampalaLocationFallback.notice(input)
        )

        assertTrue(notice.contains("couldn't identify"))
        assertTrue(notice.contains("No directory results have been assumed"))
        assertFalse(notice.contains("Did you mean"))
    }

    @Test
    fun fallbackDoesNotHijackUnrelatedRequests() {
        listOf(
            "Tell me about Kampala history",
            "Explain how a computer works",
            "Book a hotel in Kampala",
            "Open settings",
            "Call Acacia Mall"
        ).forEach { input ->
            assertNull(
                "Non-directory request intercepted: $input",
                KampalaLocationFallback.notice(input)
            )
        }
    }

    @Test
    fun historyRemainsOutsidePlacesRouting() {
        assertNull(
            KampalaPlacesQueryRoute.resolve(
                "Tell me about Kampala history"
            )
        )

        assertNull(
            KampalaLocationFallback.notice(
                "Tell me about Kampala history"
            )
        )
    }

    @Test
    fun genericUnsupportedLocationDoesNotTriggerSearch() {
        val input = "Show me hotels in Mystery Village"

        assertNull(KampalaPlacesQueryRoute.resolve(input))

        val notice = KampalaLocationFallback.notice(input)

        assertNotNull(notice)
        assertTrue(notice!!.contains("currently supported Kampala search area"))
    }
}
