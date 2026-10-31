package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class KampalaP3B2BSnakebiteEmergencyTest {

    @Test
    fun exactSamsungSnakebiteReportIsRecognized() {
        val notice =
            KampalaP3B2BSnakebiteEmergency.noticeOrNull(
                "been bitten by a snake"
            )

        assertNotNull(notice)
        assertTrue(notice!!.contains("URGENT"))
        assertTrue(notice.contains("emergency medical care"))
        assertTrue(notice.contains("Do not apply a tourniquet"))
        assertTrue(notice.contains("not contacted emergency services"))
    }

    @Test
    fun commonFirstPersonSnakebiteWordingIsRecognized() {
        val examples = listOf(
            "I was bitten by a snake",
            "I got bitten by a snake",
            "A snake bit me",
            "I have a snakebite",
            "I was snake-bitten"
        )

        examples.forEach { message ->
            assertNotNull(
                "Missed snakebite report: $message",
                KampalaP3B2BSnakebiteEmergency
                    .noticeOrNull(message)
            )
        }
    }

    @Test
    fun emergencyReportContainingJourneyDetailsIsRecognized() {
        val notice =
            KampalaP3B2BSnakebiteEmergency.noticeOrNull(
                "I was bitten by a snake while heading " +
                    "to the university, then I need to get " +
                    "to the embassy."
            )

        assertNotNull(notice)
        assertFalse(notice!!.contains("Open directions:"))
        assertFalse(notice.contains("itinerary"))
    }

    @Test
    fun ordinaryTravelRequestIsNotIntercepted() {
        assertNull(
            KampalaP3B2BSnakebiteEmergency.noticeOrNull(
                "From Serena Hotel Kigo via Victoria University " +
                    "to the Russian Embassy"
            )
        )
    }

    @Test
    fun unrelatedTechnicalQuestionIsNotIntercepted() {
        assertNull(
            KampalaP3B2BSnakebiteEmergency.noticeOrNull(
                "Explain Kotlin coroutine cancellation."
            )
        )
    }

    @Test
    fun genericSnakebiteInformationQuestionIsNotAnIncident() {
        assertNull(
            KampalaP3B2BSnakebiteEmergency.noticeOrNull(
                "What are common snakebite symptoms?"
            )
        )
    }

    @Test
    fun emergencyNoticeDoesNotInventServicesOrRoutes() {
        val notice = requireNotNull(
            KampalaP3B2BSnakebiteEmergency.noticeOrNull(
                "A snake bit me"
            )
        )

        assertFalse(notice.contains("https://"))
        assertFalse(notice.contains("Open directions:"))
        assertFalse(notice.contains("ETA:"))
        assertFalse(notice.contains("ambulance dispatched"))
        assertTrue(notice.contains("not contacted emergency services"))
    }

    @Test
    fun dogBiteIsNotMisclassifiedAsSnakebite() {
        assertNull(
            KampalaP3B2BSnakebiteEmergency.noticeOrNull(
                "I was bitten by a dog"
            )
        )
    }

    @Test
    fun reportWithEarlierItineraryStillGetsEmergencyGuidance() {
        val notice =
            KampalaP3B2BSnakebiteEmergency.noticeOrNull(
                "I was bitten by a snake before travelling " +
                    "to the Ministry of Internal Affairs."
            )

        assertNotNull(notice)
        assertTrue(notice!!.startsWith("URGENT"))
        assertFalse(notice.contains("Your planned journey"))
    }
}