package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaP3B1FJourneyGuardTest {

    @Test
    fun compoundSchoolMallRestaurantTheatreJourneyIsProtected() {
        val message =
            "I need to get to national theater for a play, " +
                "but first I have to head to Aga Khan " +
                "international school to pick up my cousin. " +
                "Then I have to drop him at Garden City Mall " +
                "before I proceed. I will pick up Chinese " +
                "food from a restaurant before proceeding " +
                "to the destination."

        val notice =
            KampalaP3B1FJourneyGuard.noticeOrNull(message)

        assertNotNull(notice)
        assertTrue(notice!!.contains("multi-stop journey"))
        assertTrue(notice.contains("starting point"))
        assertTrue(notice.contains("restaurant pickup"))
        assertTrue(notice.contains("No route"))
        assertFalse(notice.contains("Open directions:"))
        assertFalse(notice.contains("google.com/maps"))
    }

    @Test
    fun restaurantOnlyQueryIsNotIntercepted() {
        assertNull(
            KampalaP3B1FJourneyGuard.noticeOrNull(
                "Find a Chinese restaurant in Kampala."
            )
        )
    }

    @Test
    fun technicalQuestionIsNotIntercepted() {
        assertNull(
            KampalaP3B1FJourneyGuard.noticeOrNull(
                "Explain the difference between Kotlin " +
                    "coroutines and Java threads."
            )
        )
    }

    @Test
    fun ordinaryTwoPointJourneyIsNotIntercepted() {
        assertNull(
            KampalaP3B1FJourneyGuard.noticeOrNull(
                "from Katwe to Luzira"
            )
        )
    }

    @Test
    fun shortSingleErrandIsNotIntercepted() {
        assertNull(
            KampalaP3B1FJourneyGuard.noticeOrNull(
                "I want to pick up food before going home."
            )
        )
    }

    @Test
    fun protectedResponseNeverClaimsVerifiedRouting() {
        val notice =
            KampalaP3B1FJourneyGuard.noticeOrNull(
                "First I have to go to the school, " +
                    "then pick up my cousin and finally " +
                    "proceed to the theatre."
            )

        assertNotNull(notice)
        assertFalse(notice!!.contains("https://"))
        assertFalse(notice.contains("ETA:"))
        assertFalse(notice.contains("Open directions:"))
    }
}