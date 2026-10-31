package com.example.jarvis.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaP3B1CWaypointReportTest {

    private val resolver = KampalaMultiStopGeoResolver

    private fun result(
        vararg findings: KampalaMultiStopGeoResolver.Finding
    ) = KampalaMultiStopGeoResolver.JourneyResult(
        findings.mapIndexed { index, finding ->
            val role = when (index) {
                0 -> KampalaMultiStopGeoResolver.Role.ORIGIN
                findings.lastIndex ->
                    KampalaMultiStopGeoResolver.Role.DESTINATION
                else -> KampalaMultiStopGeoResolver.Role.STOP
            }

            val name = when (index) {
                0 -> "Marriott Nsambya"
                findings.lastIndex -> "Kingdom Kampala"
                else -> "Green Hill Academy"
            }

            KampalaMultiStopGeoResolver.Waypoint(
                role = role,
                requestedName = name,
                finding = finding
            )
        }
    )

    @Test
    fun missingOriginIsScopedToSearchArea() {
        val report = KampalaP3B1CWaypointReport.format(
            result(
                KampalaMultiStopGeoResolver.Finding.NotFound,
                KampalaMultiStopGeoResolver.Finding.Incomplete,
                KampalaMultiStopGeoResolver.Finding.NotFound
            )
        )

        assertTrue(report.contains("From: Marriott Nsambya"))
        assertTrue(report.contains("Stop 1: Green Hill Academy"))
        assertTrue(report.contains("To: Kingdom Kampala"))

        assertTrue(report.contains("within the searched area"))
        assertTrue(report.contains("search incomplete"))
        assertTrue(report.contains("may exist elsewhere"))
        assertFalse(report.contains("Open directions:"))
    }

    @Test
    fun unavailableIsNotDescribedAsNotFound() {
        val report = KampalaP3B1CWaypointReport.format(
            result(
                KampalaMultiStopGeoResolver.Finding.Unavailable(
                    "Simulated provider outage"
                )
            )
        )

        assertTrue(report.contains("geographic information unavailable"))
        assertFalse(report.contains("no exact listing found"))
        assertFalse(report.contains("Simulated provider outage"))
    }

    @Test
    fun ambiguousLocationRequestsClarification() {
        val report = KampalaP3B1CWaypointReport.format(
            result(
                KampalaMultiStopGeoResolver.Finding.Ambiguous(
                    emptyList()
                )
            )
        )

        assertTrue(report.contains("multiple matching listings"))
        assertTrue(report.contains("please clarify"))
        assertFalse(report.contains("Open directions:"))
    }

    @Test
    fun multipleStopsRetainNumberedSequence() {
        val report = KampalaP3B1CWaypointReport.format(
            KampalaMultiStopGeoResolver.JourneyResult(
                listOf(
                    KampalaMultiStopGeoResolver.Waypoint(
                        KampalaMultiStopGeoResolver.Role.ORIGIN,
                        "Nsambya",
                        KampalaMultiStopGeoResolver.Finding.NotFound
                    ),
                    KampalaMultiStopGeoResolver.Waypoint(
                        KampalaMultiStopGeoResolver.Role.STOP,
                        "Ntinda",
                        KampalaMultiStopGeoResolver.Finding.Incomplete
                    ),
                    KampalaMultiStopGeoResolver.Waypoint(
                        KampalaMultiStopGeoResolver.Role.STOP,
                        "Kololo",
                        KampalaMultiStopGeoResolver.Finding.NotFound
                    ),
                    KampalaMultiStopGeoResolver.Waypoint(
                        KampalaMultiStopGeoResolver.Role.DESTINATION,
                        "Nakasero",
                        KampalaMultiStopGeoResolver.Finding.NotFound
                    )
                )
            )
        )

        val first = report.indexOf("From: Nsambya")
        val stopOne = report.indexOf("Stop 1: Ntinda")
        val stopTwo = report.indexOf("Stop 2: Kololo")
        val end = report.indexOf("To: Nakasero")

        assertTrue(first >= 0)
        assertTrue(stopOne > first)
        assertTrue(stopTwo > stopOne)
        assertTrue(end > stopTwo)
        assertFalse(report.contains("Open directions:"))
    }

    @Test
    fun emptyResultsStillCannotOfferRouting() {
        val report = KampalaP3B1CWaypointReport.format(
            KampalaMultiStopGeoResolver.JourneyResult(emptyList())
        )

        assertTrue(report.contains("No route, distance"))
        assertFalse(report.contains("https://www.google.com/maps"))
    }
}