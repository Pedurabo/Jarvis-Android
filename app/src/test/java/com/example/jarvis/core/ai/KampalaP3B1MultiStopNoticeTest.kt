package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaP3B1MultiStopNoticeTest {

    private fun parse(text: String) =
        KampalaJourneyIntentResolver.resolve(text)

    @Test
    fun threePointJourneyPreservesStopOrder() {
        val journey = parse(
            "from Marriott Nsambya via Green Hill Academy to Kingdom Kampala"
        )

        assertNotNull(journey)

        val notice = KampalaP3B1MultiStopNotice.format(journey!!)

        val origin = notice.indexOf("From: Marriott Nsambya")
        val stop = notice.indexOf("Stop 1: Green Hill Academy")
        val destination = notice.indexOf("To: Kingdom Kampala")

        assertTrue(origin >= 0)
        assertTrue(stop > origin)
        assertTrue(destination > stop)
        assertFalse(notice.contains("Open directions:"))
        assertFalse(notice.contains("google.com/maps"))
    }

    @Test
    fun fourPointJourneyPreservesTwoIntermediateStops() {
        val journey = parse(
            "from Nsambya via Ntinda via Kololo to Nakasero"
        )

        assertNotNull(journey)

        val notice = KampalaP3B1MultiStopNotice.format(journey!!)

        assertTrue(
            notice.indexOf("Stop 1: Ntinda") <
                notice.indexOf("Stop 2: Kololo")
        )

        assertTrue(
            notice.indexOf("Stop 2: Kololo") <
                notice.indexOf("To: Nakasero")
        )
    }

    @Test
    fun noticeNeverClaimsDirectionsOrTravelTimes() {
        val journey = KampalaJourneyIntentResolver.Journey(
            origin = "Marriott Nsambya",
            destination = "Kingdom Kampala",
            stops = listOf("Green Hill Academy")
        )

        val notice = KampalaP3B1MultiStopNotice.format(journey)

        assertTrue(notice.contains("not yet"))
        assertTrue(notice.contains("not calculated"))
        assertFalse(notice.contains("https://"))
        assertFalse(notice.contains("Open directions:"))
        assertFalse(notice.contains("ETA:"))
    }

    @Test
    fun existingTwoPointJourneyDoesNotUseMultiStopNotice() {
        val journey = parse("from Katwe to Luzira")

        assertNotNull(journey)
        assertTrue(journey!!.stops.isEmpty())
    }

    @Test
    fun parserDoesNotTurnTechnicalQuestionsIntoJourneys() {
        assertEquals(
            null,
            parse("Explain Kotlin coroutine cancellation")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun noticeRejectsJourneyWithoutStops() {
        KampalaP3B1MultiStopNotice.format(
            KampalaJourneyIntentResolver.Journey(
                origin = "Katwe",
                destination = "Luzira"
            )
        )
    }
}