package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class JarvisConversationBoundaryRepairTest {

    @Test
    fun depositRequestReachesExistingUnsupportedAreaGuard() {
        assertEquals(
            "find banks in old kampala",
            JarvisGeoRoutingRepair.normalize(
                "trying to deposit some money but the atms aren't working, " +
                    "can you find me banks in old kampala?"
            )
        )
    }

    @Test
    fun sentenceEndingInFollowUpMapsToExistingCommand() {
        assertEquals(
            "find banks in nakawa",
            JarvisGeoRoutingRepair.normalize(
                "how about banks in nakawa, can you find those?"
            )
        )
    }

    @Test
    fun questionMarkFollowUpUsesSameArea() {
        assertEquals(
            "find banks in nakawa",
            JarvisGeoRoutingRepair.normalize(
                "how about banks in nakawa? can you find those?"
            )
        )
    }

    @Test
    fun knownWorkingRequestsRemainUnchanged() {
        val inputs = listOf(
            "find hospitals in ntinda",
            "find banks in nakawa?",
            "find banks in old kampala"
        )

        for (input in inputs) {
            assertEquals(input, JarvisGeoRoutingRepair.normalize(input))
        }
    }

    @Test
    fun combinedGeographyIsNeverSilentlyTruncated() {
        val input = "find hospitals in kireka and banks in nakawa"

        assertEquals(input, JarvisGeoRoutingRepair.normalize(input))
    }

    @Test
    fun ordinaryChatIsNotRewritten() {
        val inputs = listOf(
            "hello jarvis",
            "what are Kotlin coroutines?",
            "can you find my saved document?",
            "explain bank deposits",
            "I am trying to deposit changes to Git"
        )

        for (input in inputs) {
            assertEquals(input, JarvisGeoRoutingRepair.normalize(input))
        }
    }

    @Test
    fun leakedConversationTranscriptIsRejected() {
        val response = """
            CONTEXT:
            user: how about banks in old kampala?
            assistant: The following banks are available.
            Please provide code review comments.
        """.trimIndent()

        assertEquals(
            JarvisP2B7COutputPolicy.SAFE_RESPONSE,
            JarvisP2B7COutputPolicy.review(
                "find me banks in old kampala", response
            )
        )
    }

    @Test
    fun legitimateTechnicalExplanationIsPreserved() {
        val response = """
            CONTEXT:
            user: example
            assistant: example
        """.trimIndent()

        assertEquals(
            response,
            JarvisP2B7COutputPolicy.review(
                "Explain this transcript format", response
            )
        )
    }

    @Test
    fun ordinaryGeographicAnswerIsPreserved() {
        val response =
            "Map listings in Ntinda. Source: OpenStreetMap."

        assertEquals(
            response,
            JarvisP2B7COutputPolicy.review(
                "find hospitals in ntinda", response
            )
        )
    }
}