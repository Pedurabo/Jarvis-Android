package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaP3A2MultiStopResolutionTest {

    private val journey = KampalaJourneyIntentResolver.Journey(
        origin = "Marriott Nsambya",
        destination = "Kingdom Kampala",
        stops = listOf("Green Hill Academy")
    )

    @Test
    fun preservesOriginStopAndDestinationOrder() {
        val result = KampalaMultiStopResolutionPolicy.resolve(journey) {
            KampalaMultiStopResolutionPolicy.DirectoryEvidence(
                available = true,
                entries = listOf(it to "Public directory record")
            )
        }

        assertEquals(
            listOf(
                "Marriott Nsambya",
                "Green Hill Academy",
                "Kingdom Kampala"
            ),
            result.places.map { it.requestedName }
        )

        assertEquals(
            listOf(
                KampalaMultiStopResolutionPolicy.Role.ORIGIN,
                KampalaMultiStopResolutionPolicy.Role.STOP,
                KampalaMultiStopResolutionPolicy.Role.DESTINATION
            ),
            result.places.map { it.role }
        )

        assertTrue(result.allHaveUniqueDirectoryListings)
        assertFalse(result.readyForRouting)
    }

    @Test
    fun ambiguousCampusRequiresFurtherResolution() {
        val result = KampalaMultiStopResolutionPolicy.resolve(journey) { name ->
            if (name == "Green Hill Academy") {
                KampalaMultiStopResolutionPolicy.DirectoryEvidence(
                    available = true,
                    entries = listOf(
                        name to "Campus record A",
                        name to "Campus record B"
                    )
                )
            } else {
                KampalaMultiStopResolutionPolicy.DirectoryEvidence(
                    available = true,
                    entries = listOf(name to "Directory record")
                )
            }
        }

        assertTrue(
            result.places[1].status is
                KampalaMultiStopResolutionPolicy.Status.Ambiguous
        )
        assertFalse(result.allHaveUniqueDirectoryListings)
        assertFalse(result.readyForRouting)
    }

    @Test
    fun missingListingRemainsUnverified() {
        val result = KampalaMultiStopResolutionPolicy.resolve(journey) { name ->
            KampalaMultiStopResolutionPolicy.DirectoryEvidence(
                available = true,
                entries = if (name == "Marriott Nsambya") {
                    emptyList()
                } else {
                    listOf(name to "Directory record")
                }
            )
        }

        assertTrue(
            result.places[0].status is
                KampalaMultiStopResolutionPolicy.Status.Unverified
        )
        assertFalse(result.readyForRouting)
    }

    @Test
    fun unavailableDirectoryDoesNotBecomeVerified() {
        val result = KampalaMultiStopResolutionPolicy.resolve(journey) {
            KampalaMultiStopResolutionPolicy.DirectoryEvidence(
                available = false,
                entries = emptyList()
            )
        }

        assertTrue(
            result.places.all {
                it.status is
                    KampalaMultiStopResolutionPolicy.Status.DirectoryUnavailable
            }
        )
        assertFalse(result.readyForRouting)
    }

    @Test
    fun legacyTwoPointJourneyRemainsSupported() {
        val legacy = KampalaJourneyIntentResolver.Journey(
            origin = "Katwe",
            destination = "Luzira"
        )

        val result = KampalaMultiStopResolutionPolicy.resolve(legacy) {
            KampalaMultiStopResolutionPolicy.DirectoryEvidence(
                available = true,
                entries = listOf(it to "Directory record")
            )
        }

        assertEquals(2, result.places.size)
        assertEquals(
            listOf(
                KampalaMultiStopResolutionPolicy.Role.ORIGIN,
                KampalaMultiStopResolutionPolicy.Role.DESTINATION
            ),
            result.places.map { it.role }
        )
        assertFalse(result.readyForRouting)
    }

    @Test
    fun manyListingsRemainUnresolved() {
        val result = KampalaMultiStopResolutionPolicy.resolve(journey) { name ->
            KampalaMultiStopResolutionPolicy.DirectoryEvidence(
                available = true,
                entries = List(11) { name to "Record $it" }
            )
        }

        assertTrue(
            result.places.all {
                it.status is
                    KampalaMultiStopResolutionPolicy.Status.TooManyMatches
            }
        )
        assertFalse(result.readyForRouting)
    }
}