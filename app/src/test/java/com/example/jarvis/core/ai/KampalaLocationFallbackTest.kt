package com.example.jarvis.core.ai

import com.example.jarvis.core.network.KampalaPlacesQueryRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaLocationFallbackTest {

    @Test
    fun misspelledNtindaGetsSuggestion() {
        val notice = KampalaLocationFallback.notice(
            "find places in ntingd"
        )

        assertNotNull(notice)
        assertTrue(notice!!.contains("Did you mean Ntinda?"))
        assertTrue(notice.contains("haven't searched"))
    }

    @Test
    fun unknownDistrictGetsSafeNotice() {
        val notice = KampalaLocationFallback.notice(
            "Find restaurants in Unknown District"
        )

        assertNotNull(notice)
        assertTrue(notice!!.contains("couldn't identify"))
    }

    @Test
    fun existingRouterStillReturnsNullForUnknownDistrict() {
        assertNull(
            KampalaPlacesQueryRoute.resolve(
                "Find restaurants in Unknown District"
            )
        )
    }

    @Test
    fun existingRouterStillReturnsNullForMisspelling() {
        assertNull(
            KampalaPlacesQueryRoute.resolve(
                "find places in ntingd"
            )
        )
    }

    @Test
    fun validNtindaRequestDoesNotGetFallbackNotice() {
        assertNull(
            KampalaLocationFallback.notice(
                "Find places in Ntinda"
            )
        )

        assertEquals(
            "Ntinda",
            KampalaPlacesQueryRoute.resolve(
                "Find places in Ntinda"
            )?.area
        )
    }

    @Test
    fun validKololoRequestDoesNotGetFallbackNotice() {
        assertNull(
            KampalaLocationFallback.notice(
                "Find restaurants in Kololo"
            )
        )
    }

    @Test
    fun existingKampalaHotelSearchIsNotIntercepted() {
        assertNull(
            KampalaLocationFallback.notice(
                "Find hotels in Kampala"
            )
        )

        assertEquals(
            "accommodation",
            KampalaPlacesQueryRoute.resolve(
                "Find hotels in Kampala"
            )?.category
        )
    }

    @Test
    fun historyQuestionIsNotIntercepted() {
        assertNull(
            KampalaLocationFallback.notice(
                "Tell me about Kampala history"
            )
        )
    }

    @Test
    fun bookingRequestIsNotIntercepted() {
        assertNull(
            KampalaLocationFallback.notice(
                "Book a hotel in Kampala"
            )
        )
    }

    @Test
    fun unrelatedQuestionIsNotIntercepted() {
        assertNull(
            KampalaLocationFallback.notice(
                "Explain how a computer works"
            )
        )
    }

    @Test
    fun fallbackDoesNotAutocorrectAndSearch() {
        val notice = KampalaLocationFallback.notice(
            "Find hospitals in ntingd"
        )

        assertNotNull(notice)
        assertTrue(notice!!.contains("Ntinda"))
        assertNull(
            KampalaPlacesQueryRoute.resolve(
                "Find hospitals in ntingd"
            )
        )
    }
}
