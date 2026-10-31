package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class JourneyLiveFollowUpVerificationTest {

    private val goal =
        "I need to get to National Theatre for a play, " +
            "but first I have to head to Aga Khan International " +
            "School to pick up my cousin. Then I have to drop " +
            "him at Garden City Mall before proceeding. " +
            "I also need to pick up Chinese food from a " +
            "restaurant before reaching the theatre."

    @Test
    fun exactSamsungFollowUpsRetainJourneyAndAdvanceProgress() {
        val initial = ConversationTaskState.Task(
            kind = ConversationTaskState.Kind.NAVIGATION,
            goal = goal
        )

        val origin = requireNotNull(
            ConversationTaskState.journeyFollowUpOrNull(
                initial, "am at serena hotel kigo"
            )
        )

        assertEquals(
            "serena hotel kigo",
            origin.first.get(
                ConversationTaskState.Field.ORIGIN
            )?.value
        )

        val pickup = requireNotNull(
            ConversationTaskState.journeyFollowUpOrNull(
                origin.first, "I've picked up my cousin"
            )
        )

        assertEquals(
            "1",
            pickup.first.get(
                ConversationTaskState.Field.COMPLETED_STOPS
            )?.value
        )

        val next = requireNotNull(
            ConversationTaskState.journeyFollowUpOrNull(
                pickup.first, "what's next"
            )
        )

        assertEquals(goal, next.first.goal)
        assertTrue(
            next.second.contains(
                "Next planned stop: Garden City Mall"
            )
        )
        assertTrue(
            next.second.contains(
                "Aga Khan International School [COMPLETED]"
            )
        )
        assertTrue(
            next.second.contains(
                "Starting point: serena hotel kigo"
            )
        )
    }

    @Test
    fun unrelatedKampalaDiscoveryIsNotIntercepted() {
        val initial = ConversationTaskState.Task(
            kind = ConversationTaskState.Kind.NAVIGATION,
            goal = goal
        )

        listOf(
            "Find banks near me",
            "Find Case Clinic in Nakasero",
            "Find hotels in Kampala",
            "Show local universities",
            "Find restaurants nearby"
        ).forEach {
            assertNull(
                ConversationTaskState.journeyFollowUpOrNull(
                    initial, it
                )
            )
        }
    }

    @Test
    fun earlierOriginWordingStillWorks() {
        val initial = ConversationTaskState.Task(
            kind = ConversationTaskState.Kind.NAVIGATION,
            goal = goal
        )

        listOf(
            "I'm currently at Serena Hotel in Kigo",
            "currently at Serena Hotel Kigo",
            "I am at Serena Hotel Kigo",
            "am at Serena Hotel Kigo"
        ).forEach {
            assertNotNull(
                ConversationTaskState.journeyFollowUpOrNull(
                    initial, it
                )
            )
        }
    }
}