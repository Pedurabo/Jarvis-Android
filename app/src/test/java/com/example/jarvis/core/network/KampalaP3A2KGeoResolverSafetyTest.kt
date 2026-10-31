package com.example.jarvis.core.network

import com.example.jarvis.core.ai.KampalaJourneyIntentResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P3-A2K: Behavioural regression coverage for multi-stop
 * geographic resolution.
 *
 * All provider listings are fixtures. No live network requests.
 * No result may authorize road routing or a Maps handoff.
 */
class KampalaP3A2KGeoResolverSafetyTest {

    private val bounds = KampalaGeoDiscoveryContract.Bounds(
        south = 0.30,
        west = 32.55,
        north = 0.35,
        east = 32.64
    )

    private val journey = KampalaJourneyIntentResolver.Journey(
        origin = "Marriott Nsambya",
        destination = "Kingdom Kampala",
        stops = listOf("Green Hill Academy")
    )

    private fun place(
        id: String,
        name: String,
        latitude: Double = 0.32,
        longitude: Double = 32.59
    ) = KampalaGeoDiscoveryContract.Place(
        providerId = id,
        name = name,
        category = "school",
        coordinates = KampalaGeoDiscoveryContract.Coordinates(
            latitude = latitude,
            longitude = longitude
        ),
        publicAddress = null,
        area = null,
        sourceUrl = "https://www.openstreetmap.org/node/$id",
        isPublicListing = true
    )

    private fun success(
        places: List<KampalaGeoDiscoveryContract.Place>,
        nextPageToken: String? = null
    ) = KampalaGeoDiscoveryContract.Result.Success(
        KampalaGeoDiscoveryContract.Page(
            provider = "OpenStreetMap test fixture",
            places = places,
            nextPageToken = nextPageToken
        )
    )

    @Test
    fun preservesOriginStopDestinationOrder() = runBlocking {
        val requested = mutableListOf<String>()

        val resolver = KampalaMultiStopGeoResolver { request ->
            requested += request.query.orEmpty()
            success(emptyList())
        }

        val result = resolver.resolve(journey, bounds)

        assertEquals(
            listOf(
                "Marriott Nsambya",
                "Green Hill Academy",
                "Kingdom Kampala"
            ),
            requested
        )

        assertEquals(
            requested,
            result.waypoints.map { it.requestedName }
        )

        assertEquals(
            listOf(
                KampalaMultiStopGeoResolver.Role.ORIGIN,
                KampalaMultiStopGeoResolver.Role.STOP,
                KampalaMultiStopGeoResolver.Role.DESTINATION
            ),
            result.waypoints.map { it.role }
        )

        assertFalse(result.readyForRouting)
    }

    @Test
    fun providerIncompleteStaysIncomplete() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                reason = "More matching places exist.",
                kind = KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE
            )
        }

        val result = resolver.resolve(journey, bounds)

        assertEquals(3, result.waypoints.size)

        assertTrue(
            result.waypoints.all {
                it.finding ===
                    KampalaMultiStopGeoResolver.Finding.Incomplete
            }
        )

        assertFalse(result.allHaveUniqueProviderListings)
        assertFalse(result.readyForRouting)
    }

    @Test
    fun ordinaryProviderFailureStaysUnavailable() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "Provider temporarily unavailable."
            )
        }

        val result = resolver.resolve(journey, bounds)

        assertTrue(
            result.waypoints.all {
                it.finding is
                    KampalaMultiStopGeoResolver.Finding.Unavailable
            }
        )

        assertFalse(result.readyForRouting)
    }

    @Test
    fun emptyCompleteSearchIsNotFound() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver {
            success(emptyList())
        }

        val result = resolver.resolve(journey, bounds)

        assertTrue(
            result.waypoints.all {
                it.finding ===
                    KampalaMultiStopGeoResolver.Finding.NotFound
            }
        )

        assertFalse(result.readyForRouting)
    }

    @Test
    fun duplicateNamesRequireDisambiguation() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver { request ->
            success(
                listOf(
                    place("10001", request.query.orEmpty()),
                    place("10002", request.query.orEmpty())
                )
            )
        }

        val result = resolver.resolve(journey, bounds)

        assertTrue(
            result.waypoints.all {
                it.finding is
                    KampalaMultiStopGeoResolver.Finding.Ambiguous
            }
        )

        assertTrue(
            result.waypoints.all {
                val finding = it.finding as
                    KampalaMultiStopGeoResolver.Finding.Ambiguous
                finding.candidates.size == 2
            }
        )

        assertFalse(result.readyForRouting)
    }

    @Test
    fun paginatedSuccessCannotProveUniqueListing() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver { request ->
            success(
                places = listOf(
                    place("10003", request.query.orEmpty())
                ),
                nextPageToken = "next-safe-page"
            )
        }

        val result = resolver.resolve(journey, bounds)

        assertTrue(
            result.waypoints.all {
                it.finding ===
                    KampalaMultiStopGeoResolver.Finding.Incomplete
            }
        )

        assertFalse(result.allHaveUniqueProviderListings)
        assertFalse(result.readyForRouting)
    }

    @Test
    fun providerExceptionCannotBecomeMissingPlace() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver {
            throw IllegalStateException("Simulated network failure")
        }

        val result = resolver.resolve(journey, bounds)

        assertTrue(
            result.waypoints.all {
                it.finding is
                    KampalaMultiStopGeoResolver.Finding.Unavailable
            }
        )

        assertFalse(result.readyForRouting)
    }

    @Test
    fun mixedWaypointEvidencePreservesEveryDecision() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver { request ->
            when (request.query) {
                "Marriott Nsambya" ->
                    success(emptyList())

                "Green Hill Academy" ->
                    KampalaGeoDiscoveryContract.Result.Unavailable(
                        "More matching places exist.",
                        KampalaGeoDiscoveryContract.Result.Kind.INCOMPLETE
                    )

                "Kingdom Kampala" ->
                    success(
                        listOf(
                            place("10004", "Kingdom Kampala")
                        )
                    )

                else ->
                    error("Unexpected waypoint: ${request.query}")
            }
        }

        val result = resolver.resolve(journey, bounds)

        assertEquals(3, result.waypoints.size)

        assertTrue(
            result.waypoints[0].finding ===
                KampalaMultiStopGeoResolver.Finding.NotFound
        )

        assertTrue(
            result.waypoints[1].finding ===
                KampalaMultiStopGeoResolver.Finding.Incomplete
        )

        assertTrue(
            result.waypoints[2].finding is
                KampalaMultiStopGeoResolver.Finding.UniqueListing
        )

        assertFalse(result.allHaveUniqueProviderListings)
        assertFalse(result.readyForRouting)
    }

    @Test
    fun allUniqueListingsStillCannotAuthorizeRouting() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver { request ->
            success(
                listOf(
                    place("10005", request.query.orEmpty())
                )
            )
        }

        val result = resolver.resolve(journey, bounds)

        assertTrue(result.allHaveUniqueProviderListings)

        // OSM listings do not prove verified entrances or road access.
        assertFalse(result.readyForRouting)
    }

    @Test
    fun outOfBoundsProviderListingIsRejected() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver { request ->
            success(
                listOf(
                    place(
                        "10006",
                        request.query.orEmpty(),
                        latitude = 0.80
                    )
                )
            )
        }

        val result = resolver.resolve(journey, bounds)

        assertTrue(
            result.waypoints.all {
                it.finding is
                    KampalaMultiStopGeoResolver.Finding.Unavailable
            }
        )

        assertFalse(result.readyForRouting)
    }

    @Test
    fun invalidBoundsDoNotCallProvider() = runBlocking {
        var providerCalls = 0

        val resolver = KampalaMultiStopGeoResolver {
            providerCalls++
            success(emptyList())
        }

        val invalidBounds = KampalaGeoDiscoveryContract.Bounds(
            south = 0.50,
            west = 32.55,
            north = 0.30,
            east = 32.64
        )

        val result = resolver.resolve(journey, invalidBounds)

        assertEquals(0, providerCalls)

        assertTrue(
            result.waypoints.all {
                it.finding is
                    KampalaMultiStopGeoResolver.Finding.Unavailable
            }
        )

        assertFalse(result.readyForRouting)
    }
}