package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaDestinationConfidencePolicyTest {

    private val policy = KampalaDestinationConfidencePolicy

    @Test
    fun oneExactListingIsSelected() {
        val result = policy.resolve(
            "Acacia Mall",
            listOf(
                "Acacia Mall" to "Kisementi, Kampala",
                "Another Mall" to "Kampala"
            ),
            directoryAvailable = true
        )

        assertEquals(
            0,
            (result as KampalaDestinationConfidencePolicy
                .Resolution.ExactListing).index
        )
    }

    @Test
    fun twoDistinctExactListingsAreAmbiguous() {
        val result = policy.resolve(
            "Central Market",
            listOf(
                "Central Market" to "Area One",
                "Central Market" to "Area Two"
            ),
            directoryAvailable = true
        )

        assertEquals(
            listOf(0, 1),
            (result as KampalaDestinationConfidencePolicy
                .Resolution.Ambiguous).indices
        )
    }

    @Test
    fun identicalDuplicateRecordsDoNotCreateAmbiguity() {
        val result = policy.resolve(
            "Acacia Mall",
            listOf(
                "Acacia Mall" to "Kisementi",
                " acacia mall " to " Kisementi "
            ),
            directoryAvailable = true
        )

        assertTrue(
            result is KampalaDestinationConfidencePolicy
                .Resolution.ExactListing
        )
    }

    @Test
    fun elevenDistinctListingsRequireClarification() {
        val entries = (1..11).map { index ->
            "Market" to "Area $index"
        }

        val result = policy.resolve(
            "Market",
            entries,
            directoryAvailable = true
        )

        assertEquals(
            11,
            (result as KampalaDestinationConfidencePolicy
                .Resolution.TooManyMatches).count
        )
    }

    @Test
    fun successfulSearchWithNoExactListingIsUnverified() {
        val result = policy.resolve(
            "Victoria Mall",
            listOf("Different Mall" to "Kampala"),
            directoryAvailable = true
        )

        assertTrue(
            result is KampalaDestinationConfidencePolicy
                .Resolution.Unverified
        )
    }

    @Test
    fun failedDirectoryIsNotMisreportedAsNoMatch() {
        val result = policy.resolve(
            "Victoria Mall",
            emptyList(),
            directoryAvailable = false
        )

        assertTrue(
            result is KampalaDestinationConfidencePolicy
                .Resolution.DirectoryUnavailable
        )
    }

    @Test
    fun unavailableDirectoryOverridesSuppliedEntries() {
        val result = policy.resolve(
            "Acacia Mall",
            listOf("Acacia Mall" to "Kisementi"),
            directoryAvailable = false
        )

        assertTrue(
            result is KampalaDestinationConfidencePolicy
                .Resolution.DirectoryUnavailable
        )
    }

    @Test
    fun blankAddressIsNotUsableVerification() {
        val result = policy.resolve(
            "Acacia Mall",
            listOf("Acacia Mall" to "  "),
            directoryAvailable = true
        )

        assertTrue(
            result is KampalaDestinationConfidencePolicy
                .Resolution.Unverified
        )
    }

    @Test
    fun caseAndWhitespaceDifferencesAreNormalized() {
        val result = policy.resolve(
            "  ACACIA   MALL ",
            listOf("Acacia Mall" to "Kisementi"),
            directoryAvailable = true
        )

        assertTrue(
            result is KampalaDestinationConfidencePolicy
                .Resolution.ExactListing
        )
    }

    @Test
    fun emptySuccessfulDirectoryIsUnverified() {
        val result = policy.resolve(
            "Unlisted Place",
            emptyList(),
            directoryAvailable = true
        )

        assertTrue(
            result is KampalaDestinationConfidencePolicy
                .Resolution.Unverified
        )
    }
}