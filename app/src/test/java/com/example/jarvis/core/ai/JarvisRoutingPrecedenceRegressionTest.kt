package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class JarvisRoutingPrecedenceRegressionTest {

    private fun normalizeInControllerOrder(query: String): String =
        JarvisGeoRoutingRepair.normalize(
            JarvisGeoConversationWording.normalize(
                JarvisConversationStabilityPolicy
                    .normalizeGeographicRequest(query)
            )
        )

    @Test
    fun nakawaCommaAndDoubleQuestionMarks() {
        assertEquals(
            "find banks in nakawa",
            normalizeInControllerOrder(
                "how about banks in nakawa, can you find those??"
            )
        )
    }

    @Test
    fun nakawaQuestionMarkFollowUp() {
        assertEquals(
            "find banks in nakawa",
            normalizeInControllerOrder(
                "how about banks in nakawa? can you find those?"
            )
        )
    }

    @Test
    fun nakawaPeriodFollowUp() {
        assertEquals(
            "find banks in nakawa",
            normalizeInControllerOrder(
                "how about banks in nakawa. can you find those?"
            )
        )
    }

    @Test
    fun depositRequestStillReachesOldKampalaGuard() {
        assertEquals(
            "find banks in old kampala",
            normalizeInControllerOrder(
                "am trying to deposit some money but the atms " +
                    "aren't working, can you find me banks in old kampala?"
            )
        )
    }

    @Test
    fun ntindaCommandRemainsStable() {
        assertEquals(
            "find hospitals in ntinda",
            normalizeInControllerOrder("find hospitals in ntinda")
        )
    }

    @Test
    fun ordinaryTechnicalQuestionRemainsUnchanged() {
        val question = "Explain Kotlin coroutines in simple terms"

        assertEquals(
            question,
            normalizeInControllerOrder(question)
        )
    }

    @Test
    fun journeyContinuationWithoutVerificationIsIntercepted() {
        val notice =
            JarvisConversationStabilityPolicy
                .unresolvedJourneyFollowUpNotice(
                    "let's proceed with the journey, " +
                        "am currently at Marriott in nsambya"
                )

        assertNotNull(notice)
        assertTrue(notice!!.contains("haven't verified"))
        assertTrue(notice.contains("won't invent directions"))
    }

    @Test
    fun regularConversationIsNotTreatedAsNavigation() {
        assertNull(
            JarvisConversationStabilityPolicy
                .unresolvedJourneyFollowUpNotice(
                    "Can you explain the journey of a Kotlin coroutine?"
                )
        )
    }

    @Test
    fun freshJourneyIsLeftToExistingJourneyHandler() {
        assertNull(
            JarvisConversationStabilityPolicy
                .unresolvedJourneyFollowUpNotice(
                    "I need to go to Green Hill Academy " +
                        "then Kingdom Kampala"
                )
        )
    }
}