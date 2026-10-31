package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class ConversationJourneyContinuityTest {

    private val journey =
        "I need to get to National Theatre for a play, " +
            "but first I have to head to Aga Khan International " +
            "School to pick up my cousin. Then I have to drop " +
            "him at Garden City Mall before proceeding. " +
            "I also need to pick up Chinese food from a " +
            "restaurant before reaching the theatre."

    private fun startTask() =
        ConversationTaskState.Task(
            kind = ConversationTaskState.Kind.NAVIGATION,
            goal = journey
        )

    @Test
    fun originFollowUpRetainsOriginalGoal() {
        val result = requireNotNull(
            ConversationTaskState.journeyFollowUpOrNull(
                startTask(),
                "I'm currently at Serena Hotel in Kigo"
            )
        )

        assertEquals(journey, result.first.goal)

        assertEquals(
            "Serena Hotel in Kigo",
            result.first.get(
                ConversationTaskState.Field.ORIGIN
            )?.value
        )

        assertEquals(
            ConversationTaskState.Provenance.USER_SUPPLIED,
            result.first.get(
                ConversationTaskState.Field.ORIGIN
            )?.provenance
        )
    }

    @Test
    fun itinerarySurvivesTwoConversationTurns() {
        val first = requireNotNull(
            ConversationTaskState.journeyFollowUpOrNull(
                startTask(),
                "currently at Serena Hotel in Kigo"
            )
        )

        val second = requireNotNull(
            ConversationTaskState.journeyFollowUpOrNull(
                first.first,
                "What details do you still need?"
            )
        )

        val answer = second.second

        assertTrue(answer.contains("Serena Hotel in Kigo"))
        assertTrue(answer.contains("Aga Khan International School"))
        assertTrue(answer.contains("Garden City Mall"))
        assertTrue(answer.contains("National Theatre"))
        assertTrue(answer.contains("Restaurant not yet selected"))
        assertTrue(answer.contains("No route"))
    }

    @Test
    fun missingOriginIsReportedWithoutInventingOne() {
        val result = requireNotNull(
            ConversationTaskState.journeyFollowUpOrNull(
                startTask(),
                "What details do you need?"
            )
        )

        assertTrue(
            result.second.contains(
                "Still needed: your starting point"
            )
        )
    }

    @Test
    fun unrelatedQueryIsNotConsumed() {
        assertNull(
            ConversationTaskState.journeyFollowUpOrNull(
                startTask(),
                "Find banks near me"
            )
        )
    }

    @Test
    fun noActiveTaskMeansNoFollowUpInterception() {
        assertNull(
            ConversationTaskState.journeyFollowUpOrNull(
                null,
                "I'm currently at Serena Hotel in Kigo"
            )
        )
    }

    @Test
    fun nonNavigationTaskIsPreserved() {
        val shopping = ConversationTaskState.Task(
            kind = ConversationTaskState.Kind.SHOPPING,
            goal = "Find groceries"
        )

        assertNull(
            ConversationTaskState.journeyFollowUpOrNull(
                shopping,
                "What details do you need?"
            )
        )
    }
}