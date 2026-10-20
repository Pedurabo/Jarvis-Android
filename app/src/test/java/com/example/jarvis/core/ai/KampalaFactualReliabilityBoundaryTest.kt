package com.example.jarvis.core.ai

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaFactualReliabilityBoundaryTest {

    @Test
    fun sevenHillsQuestionHasSafeNotice() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "how about the 7 hills of kampala, can you list them?"
            )
        )
    }

    @Test
    fun listsOfHillsAreProtected() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "List the hills of Kampala"
            )
        )
    }

    @Test
    fun listsOfLakesAreProtected() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "list the lakes in kampala"
            )
        )
    }

    @Test
    fun administrativeCountsAreProtected() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "how many divisions are in kampala"
            )
        )
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "how many subcounties make up Kampala?"
            )
        )
    }

    @Test
    fun bothKasubiSpellingsAreHandledWithoutInventedFacts() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "how about the kasubi tombs?"
            )
        )
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "how about the Kasumi tombs??"
            )
        )
    }

    @Test
    fun compoundLandmarksAndFoodQueryIsRecognized() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "what are the major landmarks in makindye and " +
                    "where can I go to have a turkish meal?"
            )
        )
    }

    @Test
    fun unsupportedCuisineRankingIsProtected() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "which restaurant has the best chinese cuisine in kampala"
            )
        )
    }

    @Test
    fun nearestMarketRequiresRealLocationContext() {
        val notice = KampalaFactualReliabilityBoundary.notice(
            "what is the nearest market"
        )
        assertNotNull(notice)
        assertTrue(notice!!.contains("starting location"))
    }

    @Test
    fun generalKampalaOverviewIsGrounded() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "What can you tell me about Kampala?"
            )
        )
    }

    @Test
    fun unsupportedRetailAndPalaceLocationQuestionsAreProtected() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "where is nakumat?"
            )
        )
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "where is the palace located, which division"
            )
        )
    }

    @Test
    fun existingVerifiedDirectoryQueriesAreNotIntercepted() {
        listOf(
            "Find hotels in Kampala",
            "Recommend restaurants in Kololo",
            "what are the famous food points in kampala",
            "what are the best cinema halls in kampala",
            "football grounds"
        ).forEach {
            assertNull(KampalaFactualReliabilityBoundary.notice(it))
        }
    }

    @Test
    fun knowledgeAndGeneralProgrammingRemainAvailable() {
        listOf(
            "Tell me about Kampala history",
            "Explain dependency injection",
            "What is a lake?",
            "Write Kotlin code for a museum app"
        ).forEach {
            assertNull(KampalaFactualReliabilityBoundary.notice(it))
        }
    }

    @Test
    fun phoneActionsRemainOutsideTheBoundary() {
        listOf(
            "Open Android Settings",
            "Call the palace",
            "Send a message about Kasubi Tombs",
            "Navigate to the nearest market",
            "Book a hotel in Kampala"
        ).forEach {
            assertNull(KampalaFactualReliabilityBoundary.notice(it))
        }
    }
}
