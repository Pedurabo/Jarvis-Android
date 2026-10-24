package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaDestinationMatchPolicyTest {

    @Test
    fun selectsOneExactListing() {
        val entries = listOf(
            "Acacia Mall" to "Kisementi, Kampala",
            "Other Mall" to "Kampala"
        )

        assertEquals(
            listOf(0),
            KampalaDestinationMatchPolicy.exactIndices(
                "Acacia Mall", entries
            )
        )
    }

    @Test
    fun returnsDistinctAddressesForSameName() {
        val entries = listOf(
            "Sample Cafe" to "Road A",
            "Sample Cafe" to "Road B"
        )

        assertEquals(
            listOf(0, 1),
            KampalaDestinationMatchPolicy.exactIndices(
                "Sample Cafe", entries
            )
        )
    }

    @Test
    fun repeatedListingsAreNotDuplicateChoices() {
        val entries = listOf(
            "Sample Cafe" to "Road A",
            " sample cafe " to " Road A ",
            "Sample Cafe" to "Road B"
        )

        assertEquals(
            listOf(0, 2),
            KampalaDestinationMatchPolicy.exactIndices(
                "Sample Cafe", entries
            )
        )
    }

    @Test
    fun nearMatchIsNotClaimedAsVerified() {
        val entries = listOf(
            "American Embassy Annex" to "Example Address"
        )

        assertTrue(
            KampalaDestinationMatchPolicy.exactIndices(
                "American Embassy", entries
            ).isEmpty()
        )
    }

    @Test
    fun emptyAddressIsNotAUsableDestination() {
        val entries = listOf(
            "Example Place" to ""
        )

        assertTrue(
            KampalaDestinationMatchPolicy.exactIndices(
                "Example Place", entries
            ).isEmpty()
        )
    }
}