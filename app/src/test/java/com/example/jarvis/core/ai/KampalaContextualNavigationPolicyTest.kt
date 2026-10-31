package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaContextualNavigationPolicyTest {

    @Test
    fun recognizesNaturalFollowUps() {
        assertTrue(
            KampalaContextualNavigationPolicy.isFollowUp(
                "how do I get there?"
            )
        )

        assertTrue(
            KampalaContextualNavigationPolicy.isFollowUp(
                "Take me there"
            )
        )

        assertTrue(
            KampalaContextualNavigationPolicy.isFollowUp(
                "directions to that place"
            )
        )
    }

    @Test
    fun unrelatedQuestionsDoNotResolve() {
        assertFalse(
            KampalaContextualNavigationPolicy.isFollowUp(
                "What is JSON?"
            )
        )

        assertFalse(
            KampalaContextualNavigationPolicy.isFollowUp(
                "How do I get to Kololo?"
            )
        )
    }

    @Test
    fun singleDirectoryEntryCanBeReferenced() {
        val destination =
            KampalaContextualNavigationPolicy.uniqueDestination(
                listOf(
                    "Gaddafi National Mosque" to
                        "Old Kampala Road, Kampala"
                )
            )

        assertEquals(
            "Gaddafi National Mosque, Old Kampala Road, Kampala",
            destination
        )
    }

    @Test
    fun multipleEntriesAreNeverGuessed() {
        assertNull(
            KampalaContextualNavigationPolicy.uniqueDestination(
                listOf(
                    "Mosque A" to "Old Kampala",
                    "Mosque B" to "Old Kampala"
                )
            )
        )
    }

    @Test
    fun missingDirectoryEntryIsNotRemembered() {
        assertNull(
            KampalaContextualNavigationPolicy.uniqueDestination(
                emptyList()
            )
        )

        assertNull(
            KampalaContextualNavigationPolicy.uniqueDestination(
                listOf("Unknown Mosque" to "")
            )
        )
    }
}