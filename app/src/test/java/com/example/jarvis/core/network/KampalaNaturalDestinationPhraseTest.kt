package com.example.jarvis.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaNaturalDestinationPhraseTest {

    @Test
    fun standaloneTaxiParkPhraseRoutesToDirectory() {
        assertEquals(
            "New Taxi Park",
            KampalaNamedDestinationRoute.resolve(
                "new taxi park in kampala"
            )?.query
        )
    }

    @Test
    fun locationOfTaxiParkPhraseRoutesToDirectory() {
        assertEquals(
            "New Taxi Park",
            KampalaNamedDestinationRoute.resolve(
                "location of new taxi park kampala"
            )?.query
        )
    }

    @Test
    fun existingWhereIsTaxiParkStillWorks() {
        assertEquals(
            "new taxi park",
            KampalaNamedDestinationRoute.resolve(
                "where is new taxi park"
            )?.query
        )
    }

    @Test
    fun kingdomKampalaStillWorks() {
        assertEquals(
            "kingdom kampala",
            KampalaNamedDestinationRoute.resolve(
                "where do I find kingdom kampala"
            )?.query
        )
    }

    @Test
    fun taxParkTypoStillNeedsConfirmation() {
        assertNull(
            KampalaNamedDestinationRoute.resolve(
                "new tax park in kampala"
            )
        )

        val notice = requireNotNull(
            KampalaNamedDestinationRoute.notice(
                "new tax park in kampala"
            )
        )

        assertTrue(notice.contains("Did you mean New Taxi Park"))
        assertTrue(notice.contains("not automatically changed"))
    }

    @Test
    fun missingNamedDestinationGetsSpecificNotice() {
        val request = requireNotNull(
            KampalaNamedDestinationRoute.resolve(
                "Where is New Taxi Park?"
            )
        )

        val answer = KampalaPlacesResultPolicy.format(
            request,
            KampalaPlacesRepository.SearchResult.Success(emptyList())
        )

        assertTrue(answer.contains("verified directory listing"))
        assertTrue(answer.contains("can't confirm its address"))
        assertTrue(answer.contains("No location has been assumed"))
        assertFalse(answer.contains("broader directory category"))
    }

    @Test
    fun categoryRoutingKeepsExistingNotice() {
        val request = requireNotNull(
            KampalaPlacesQueryRoute.resolve(
                "Find restaurants in Ntinda"
            )
        )

        assertEquals("restaurant", request.category)
        assertEquals("Ntinda", request.area)

        val answer = KampalaPlacesResultPolicy.format(
            request,
            KampalaPlacesRepository.SearchResult.Success(emptyList())
        )

        assertTrue(answer.contains("restaurant in Ntinda"))
    }

    @Test
    fun nonDestinationRequestsStayOutsideNamedRouter() {
        listOf(
            "Tell me about Kampala history",
            "Book a hotel in Kampala",
            "Call Acacia Mall",
            "Find hotels in Kampala",
            "Explain traffic laws",
            "location of my phone"
        ).forEach {
            assertNull(KampalaNamedDestinationRoute.resolve(it))
        }
    }

    @Test
    fun unknownNamedDestinationIsNotInvented() {
        val request = requireNotNull(
            KampalaNamedDestinationRoute.resolve(
                "Where is Imaginary Mall?"
            )
        )

        assertEquals("Imaginary Mall", request.query)

        val answer = KampalaPlacesResultPolicy.format(
            request,
            KampalaPlacesRepository.SearchResult.Success(emptyList())
        )

        assertTrue(answer.contains("couldn't find a verified"))
    }
}
