package com.example.jarvis.core.network

import com.example.jarvis.core.ai.KampalaPlaceResolutionPolicy
import org.junit.Assert.*
import org.junit.Test

class KampalaDirectoryAdapterA69C1Test {

    private fun place(
        id: String = "watoto-downtown",
        name: String = "Watoto Church Downtown",
        area: String = "Central Kampala",
        source: String = "https://watotochurch.com/locations/"
    ) = KampalaPlace(
        id = id,
        name = name,
        category = "place_of_worship",
        area = area,
        address = "Plot 87 Kampala Road, Kampala",
        sourceUrl = source,
        verifiedAt = "2026-10-01"
    )

    @Test
    fun directoryFieldsMapWithoutInventingCoordinates() {
        val record =
            KampalaDirectoryResolutionAdapter.toRecord(place())

        assertEquals("watoto-downtown", record.providerId)
        assertEquals("Watoto Church Downtown", record.name)
        assertEquals("Central Kampala", record.area)
        assertEquals(
            "https://watotochurch.com/locations/",
            record.sourceUrl
        )
        assertEquals("2026-10-01", record.sourceCheckedOn)
        assertEquals(
            KampalaPlaceResolutionPolicy.SourceKind.DIRECTORY,
            record.sourceKind
        )
        assertNull(record.latitude)
        assertNull(record.longitude)
    }

    @Test
    fun existingDirectoryResultResolvesExactly() {
        val outcome =
            KampalaDirectoryResolutionAdapter.resolve(
                "Watoto Church Downtown",
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(place())
                )
            )

        assertTrue(
            outcome is KampalaDirectoryResolutionAdapter.Outcome.Resolved
        )

        val resolution =
            (outcome as KampalaDirectoryResolutionAdapter.Outcome.Resolved)
                .resolution

        assertTrue(
            resolution is KampalaPlaceResolutionPolicy.Resolution.Exact
        )
    }

    @Test
    fun sameNameDifferentBranchesStayAmbiguous() {
        val outcome =
            KampalaDirectoryResolutionAdapter.resolve(
                "Example Bank",
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(
                        place("bank-central", "Example Bank", "Central Kampala"),
                        place("bank-nakawa", "Example Bank", "Nakawa")
                    )
                )
            )

        val resolution =
            (outcome as KampalaDirectoryResolutionAdapter.Outcome.Resolved)
                .resolution

        assertTrue(
            resolution is KampalaPlaceResolutionPolicy.Resolution.Ambiguous
        )
    }

    @Test
    fun areaFilterChoosesOnlyMatchingBranch() {
        val outcome =
            KampalaDirectoryResolutionAdapter.resolve(
                requestedName = "Example Bank",
                requestedArea = "Nakawa",
                result = KampalaPlacesRepository.SearchResult.Success(
                    listOf(
                        place("bank-central", "Example Bank", "Central Kampala"),
                        place("bank-nakawa", "Example Bank", "Nakawa")
                    )
                )
            )

        val resolution =
            (outcome as KampalaDirectoryResolutionAdapter.Outcome.Resolved)
                .resolution as KampalaPlaceResolutionPolicy.Resolution.Exact

        assertEquals("bank-nakawa", resolution.place.providerId)
    }

    @Test
    fun duplicatedProviderRecordIsNotAnotherBranch() {
        val same = place()

        val outcome =
            KampalaDirectoryResolutionAdapter.resolve(
                same.name,
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(same, same)
                )
            )

        val resolution =
            (outcome as KampalaDirectoryResolutionAdapter.Outcome.Resolved)
                .resolution

        assertTrue(
            resolution is KampalaPlaceResolutionPolicy.Resolution.Exact
        )
    }

    @Test
    fun unknownDestinationIsNotFabricated() {
        val outcome =
            KampalaDirectoryResolutionAdapter.resolve(
                "Unknown Example Landmark",
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(place())
                )
            )

        val resolution =
            (outcome as KampalaDirectoryResolutionAdapter.Outcome.Resolved)
                .resolution

        assertTrue(
            resolution is KampalaPlaceResolutionPolicy.Resolution.NotFound
        )
    }

    @Test
    fun repositoryFailureIsNotMistakenForMissingPlace() {
        val outcome =
            KampalaDirectoryResolutionAdapter.resolve(
                "Watoto Church Downtown",
                KampalaPlacesRepository.SearchResult.Failure(
                    "Directory unavailable"
                )
            )

        assertTrue(
            outcome is KampalaDirectoryResolutionAdapter.Outcome.Unavailable
        )

        assertEquals(
            "Directory unavailable",
            (outcome as KampalaDirectoryResolutionAdapter.Outcome.Unavailable)
                .message
        )
    }

    @Test
    fun badSourceCannotProduceResolvedDestination() {
        val outcome =
            KampalaDirectoryResolutionAdapter.resolve(
                "Watoto Church Downtown",
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(
                        place(source = "http://invalid.example")
                    )
                )
            )

        val resolution =
            (outcome as KampalaDirectoryResolutionAdapter.Outcome.Resolved)
                .resolution

        assertTrue(
            resolution is KampalaPlaceResolutionPolicy.Resolution.NotFound
        )
    }
    @Test
    fun controllerIntegrationKeepsUniqueExactListing() {
        val matches =
            KampalaDirectoryResolutionAdapter.matchingDirectoryPlaces(
                "Watoto Church Downtown",
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(place())
                )
            )

        assertEquals(1, matches.size)
        assertEquals("watoto-downtown", matches.single().id)
    }

    @Test
    fun controllerIntegrationPreservesAmbiguousBranches() {
        val matches =
            KampalaDirectoryResolutionAdapter.matchingDirectoryPlaces(
                "Example Bank",
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(
                        place("bank-central", "Example Bank", "Central Kampala"),
                        place("bank-nakawa", "Example Bank", "Nakawa")
                    )
                )
            )

        assertEquals(2, matches.size)
        assertEquals(
            setOf("bank-central", "bank-nakawa"),
            matches.map { it.id }.toSet()
        )
    }

    @Test
    fun controllerIntegrationDoesNotPromoteUnavailableService() {
        val matches =
            KampalaDirectoryResolutionAdapter.matchingDirectoryPlaces(
                "Watoto Church Downtown",
                KampalaPlacesRepository.SearchResult.Failure(
                    "Directory offline"
                )
            )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun controllerIntegrationDoesNotPromoteUnknownPlace() {
        val matches =
            KampalaDirectoryResolutionAdapter.matchingDirectoryPlaces(
                "Unknown Example Landmark",
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(place())
                )
            )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun controllerIntegrationRejectsInvalidSource() {
        val matches =
            KampalaDirectoryResolutionAdapter.matchingDirectoryPlaces(
                "Watoto Church Downtown",
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(
                        place(source = "http://invalid.example")
                    )
                )
            )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun controllerIntegrationDoesNotSelectDuplicateRecordTwice() {
        val same = place()

        val matches =
            KampalaDirectoryResolutionAdapter.matchingDirectoryPlaces(
                same.name,
                KampalaPlacesRepository.SearchResult.Success(
                    listOf(same, same)
                )
            )

        assertEquals(1, matches.size)
    }
}
