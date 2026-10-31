package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class NavigationTurnSafetyA66CTest {

    @Test
    fun threeTurnUnverifiedConversationDoesNotAuthorizeRoute() {
        val firstDestination = "watoto church downtown"

        // First turn: no exact verified directory result.
        val verifiedDestination: String? = null
        val unverifiedDestination = firstDestination

        // Second turn: user explicitly supplies the origin.
        val origin =
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "actually, am starting from ntinda"
            )

        assertEquals("ntinda", origin)
        assertEquals("watoto church downtown", unverifiedDestination)

        // Third turn: the user clarifies the destination.
        val clarification =
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    unverifiedDestination,
                    "the downtown branch"
                )

        assertNotNull(clarification)

        // The branch clarification is NOT an origin.
        assertFalse(
            KampalaNavigationTurnSafetyPolicy.canHandleOriginReply(
                verifiedDestination,
                clarification
            )
        )

        // The original origin has not been replaced.
        assertEquals("ntinda", origin)
    }

    @Test
    fun clarificationCannotOverridePendingVerifiedOriginWorkflow() {
        assertFalse(
            KampalaNavigationTurnSafetyPolicy.canHandleOriginReply(
                "A verified directory destination",
                "Watoto Church Downtown branch"
            )
        )
    }

    @Test
    fun actualVerifiedDestinationMayAcceptNormalOriginReply() {
        assertTrue(
            KampalaNavigationTurnSafetyPolicy.canHandleOriginReply(
                "A verified directory destination",
                null
            )
        )
    }

    @Test
    fun missingDestinationNeverAuthorizesOriginRoute() {
        assertFalse(
            KampalaNavigationTurnSafetyPolicy.canHandleOriginReply(
                null,
                null
            )
        )
    }

    @Test
    fun unrelatedMessageIsNotBranchClarification() {
        assertNull(
            KampalaContextualNavigationPolicy
                .clarifiedDestinationQueryOrNull(
                    "Watoto Church",
                    "Tell me about mobile app development"
                )
        )
    }

    @Test
    fun abbreviatedOriginStillRecognized() {
        assertEquals(
            "ntinda",
            KampalaContextualNavigationPolicy.correctedOriginOrNull(
                "actually, am starting from ntinda"
            )
        )
    }

    @Test
    fun dogBiteEmergencyStillWorks() {
        val response =
            KampalaEmergencyResponsePolicy.respondOrNull(
                "I was bitten by a dog. What should I do?"
            )

        assertNotNull(response)
        assertTrue(
            response!!.contains("rabies", ignoreCase = true)
        )
        assertTrue(
            response.contains("soap", ignoreCase = true)
        )
    }
}