package com.example.jarvis.core.network

import com.example.jarvis.core.ai.KampalaJourneyIntentResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaP3A2BGeoResolverTest {

    private val bounds = KampalaGeoDiscoveryContract.Bounds(
        south = -0.1,
        west = 32.0,
        north = 0.7,
        east = 33.0
    )

    private val journey = KampalaJourneyIntentResolver.Journey(
        origin = "Marriott Nsambya",
        destination = "Kingdom Kampala",
        stops = listOf("Green Hill Academy")
    )

    private fun place(
        name: String,
        id: String = name
    ) = KampalaGeoDiscoveryContract.Place(
        providerId = id,
        name = name,
        category = "place",
        coordinates = KampalaGeoDiscoveryContract.Coordinates(
            latitude = 0.31,
            longitude = 32.59
        ),
        publicAddress = "Public map record",
        area = "Kampala",
        sourceUrl = "https://www.openstreetmap.org/",
        isPublicListing = true
    )

    private fun result(
        places: List<KampalaGeoDiscoveryContract.Place>,
        token: String? = null
    ) = KampalaGeoDiscoveryContract.Result.Success(
        KampalaGeoDiscoveryContract.Page(
            provider = "Test geographic provider",
            places = places,
            nextPageToken = token
        )
    )

    @Test
    fun providerListingsPreserveJourneyOrder() = runBlocking {
        val requested = mutableListOf<String>()

        val resolver = KampalaMultiStopGeoResolver { request ->
            val name = request.query!!
            requested += name
            result(listOf(place(name)))
        }

        val output = resolver.resolve(journey, bounds)

        assertEquals(
            listOf(
                "Marriott Nsambya",
                "Green Hill Academy",
                "Kingdom Kampala"
            ),
            requested
        )

        assertEquals(
            listOf(
                KampalaMultiStopGeoResolver.Role.ORIGIN,
                KampalaMultiStopGeoResolver.Role.STOP,
                KampalaMultiStopGeoResolver.Role.DESTINATION
            ),
            output.waypoints.map { it.role }
        )

        assertTrue(output.allHaveUniqueProviderListings)
        assertFalse(output.readyForRouting)
    }

    @Test
    fun multipleCampusListingsRemainAmbiguous() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver { request ->
            val name = request.query!!

            if (name == "Green Hill Academy") {
                result(
                    listOf(
                        place(name, "campus-a"),
                        place(name, "campus-b")
                    )
                )
            } else {
                result(listOf(place(name)))
            }
        }

        val output = resolver.resolve(journey, bounds)

        assertTrue(
            output.waypoints[1].finding is
                KampalaMultiStopGeoResolver.Finding.Ambiguous
        )
        assertFalse(output.allHaveUniqueProviderListings)
        assertFalse(output.readyForRouting)
    }

    @Test
    fun incompleteProviderPageDoesNotProveUniqueness() =
        runBlocking {
            val resolver = KampalaMultiStopGeoResolver { request ->
                result(
                    listOf(place(request.query!!)),
                    token = "page-two"
                )
            }

            val output = resolver.resolve(journey, bounds)

            assertTrue(
                output.waypoints.all {
                    it.finding is
                        KampalaMultiStopGeoResolver.Finding.Incomplete
                }
            )
            assertFalse(output.readyForRouting)
        }

    @Test
    fun invalidCoordinatesRejectProviderEvidence() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver { request ->
            val invalid = place(request.query!!).copy(
                coordinates = KampalaGeoDiscoveryContract.Coordinates(
                    latitude = 999.0,
                    longitude = 32.59
                )
            )

            result(listOf(invalid))
        }

        val output = resolver.resolve(journey, bounds)

        assertTrue(
            output.waypoints.all {
                it.finding is
                    KampalaMultiStopGeoResolver.Finding.Unavailable
            }
        )
        assertFalse(output.readyForRouting)
    }

    @Test
    fun missingListingNeverBecomesVerified() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver { request ->
            if (request.query == "Green Hill Academy") {
                result(emptyList())
            } else {
                result(listOf(place(request.query!!)))
            }
        }

        val output = resolver.resolve(journey, bounds)

        assertTrue(
            output.waypoints[1].finding is
                KampalaMultiStopGeoResolver.Finding.NotFound
        )
        assertFalse(output.readyForRouting)
    }

    @Test
    fun legacyTwoPointJourneysRemainSupported() = runBlocking {
        val legacy = KampalaJourneyIntentResolver.Journey(
            origin = "Katwe",
            destination = "Luzira"
        )

        val resolver = KampalaMultiStopGeoResolver { request ->
            result(listOf(place(request.query!!)))
        }

        val output = resolver.resolve(legacy, bounds)

        assertEquals(2, output.waypoints.size)
        assertTrue(output.allHaveUniqueProviderListings)
        assertFalse(output.readyForRouting)
    }

    @Test
    fun unavailableProviderDoesNotInventLocations() = runBlocking {
        val resolver = KampalaMultiStopGeoResolver {
            KampalaGeoDiscoveryContract.Result.Unavailable(
                "Provider unavailable"
            )
        }

        val output = resolver.resolve(journey, bounds)

        assertTrue(
            output.waypoints.all {
                it.finding is
                    KampalaMultiStopGeoResolver.Finding.Unavailable
            }
        )
        assertFalse(output.readyForRouting)
    }
}