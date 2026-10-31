package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class JarvisGeoConversationWordingTest {

    @Test
    fun politeNtindaSearchUsesExistingGeoRoute() {
        assertEquals(
            "find hospitals in ntinda?",
            JarvisGeoConversationWording.normalize(
                "can you find hospitals in ntinda?"
            )
        )
    }

    @Test
    fun conversationalNakawaRequestIsNormalized() {
        assertEquals(
            "find banks in nakawa",
            JarvisGeoConversationWording.normalize(
                "how about banks in nakawa? can you find those?"
            )
        )
    }

    @Test
    fun findMeWordingIsNormalized() {
        assertEquals(
            "find hospitals in ntinda",
            JarvisGeoConversationWording.normalize(
                "find me hospitals in ntinda"
            )
        )
    }

    @Test
    fun directSearchIsPreserved() {
        val query = "find banks in nakawa?"

        assertEquals(
            query,
            JarvisGeoConversationWording.normalize(query)
        )
    }

    @Test
    fun unsupportedOldKampalaStillReachesGeoHandler() {
        assertEquals(
            "find banks in old kampala?",
            JarvisGeoConversationWording.normalize(
                "can you find banks in old kampala?"
            )
        )
    }

    @Test
    fun unknownAreaDoesNotReceiveInventedCoverage() {
        assertEquals(
            "find banks in unknown suburb?",
            JarvisGeoConversationWording.normalize(
                "can you find banks in unknown suburb?"
            )
        )
    }

    @Test
    fun normalConversationIsUntouched() {
        val queries = listOf(
            "hello jarvis",
            "can you find the Kotlin bug?",
            "how about explaining coroutines?",
            "could you find my saved notes?",
            "what are banks?",
            "find my lost phone"
        )

        for (query in queries) {
            assertEquals(
                query,
                JarvisGeoConversationWording.normalize(query)
            )
        }
    }

    @Test
    fun combinedRequestIsNotTruncated() {
        val query =
            "find me hospitals in kireka ND banks in nakawa"

        assertEquals(
            query,
            JarvisGeoConversationWording.normalize(query)
        )
    }

    @Test
    fun combinedSupportedAreasAreNotCollapsed() {
        val query =
            "find hospitals in ntinda and banks in nakawa"

        assertEquals(
            query,
            JarvisGeoConversationWording.normalize(query)
        )
    }
}