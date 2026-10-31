package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class ConversationJourneyProgressTest {

    private val message =
        "I need to get to National Theatre for a play, " +
            "but first I have to head to Aga Khan International " +
            "School to pick up my cousin. Then I have to drop " +
            "him at Garden City Mall before proceeding. " +
            "I also need to pick up Chinese food from a " +
            "restaurant before reaching the theatre."

    private fun task() = ConversationTaskState.Task(
        kind = ConversationTaskState.Kind.NAVIGATION,
        goal = message
    )

    private fun continueJourney(
        task: ConversationTaskState.Task,
        message: String
    ) = requireNotNull(
        ConversationTaskState.journeyFollowUpOrNull(
            task,
            message
        )
    )

    @Test
    fun originIsRetainedWithoutRepeatedQuestions() {
        val updated = continueJourney(
            task(),
            "I'm currently at Serena Hotel in Kigo."
        )

        assertTrue(
            updated.second.contains(
                "Starting point: Serena Hotel in Kigo"
            )
        )

        assertFalse(
            updated.second.contains(
                "Please provide your starting point"
            )
        )

        assertTrue(
            updated.second.contains(
                "Still needed: choose a restaurant"
            )
        )
    }

    @Test
    fun firstPickupCanBeMarkedCompleted() {
        val first = continueJourney(
            task(),
            "currently at Serena Hotel in Kigo"
        )

        val completed = continueJourney(
            first.first,
            "I've picked up my cousin."
        )

        assertEquals(
            "1",
            completed.first.get(
                ConversationTaskState.Field.COMPLETED_STOPS
            )?.value
        )

        assertTrue(
            completed.second.contains(
                "Aga Khan International School [COMPLETED]"
            )
        )

        assertTrue(
            completed.second.contains(
                "Next planned stop: Garden City Mall"
            )
        )

        assertTrue(
            completed.second.contains(
                "Starting point: Serena Hotel in Kigo"
            )
        )
    }

    @Test
    fun whatsNextPreservesCompletion() {
        val first = continueJourney(
            task(),
            "I'm currently at Serena Hotel in Kigo."
        )

        val completed = continueJourney(
            first.first,
            "I've picked up my cousin."
        )

        val next = continueJourney(
            completed.first,
            "What's next?"
        )

        assertTrue(
            next.second.contains(
                "Next planned stop: Garden City Mall"
            )
        )

        assertEquals(
            "1",
            next.first.get(
                ConversationTaskState.Field.COMPLETED_STOPS
            )?.value
        )
    }

    @Test
    fun unrelatedBankRequestIsNotIntercepted() {
        assertNull(
            ConversationTaskState.journeyFollowUpOrNull(
                task(),
                "Find banks near me"
            )
        )
    }

    @Test
    fun unconfirmedPickupIsNotMarkedCompleted() {
        assertNull(
            ConversationTaskState.journeyFollowUpOrNull(
                task(),
                "I might pick up my cousin tomorrow."
            )
        )
    }

    @Test
    fun unknownTaskIsNotMarkedCompleted() {
        val shopping = ConversationTaskState.Task(
            kind = ConversationTaskState.Kind.SHOPPING,
            goal = "Find groceries"
        )

        assertNull(
            ConversationTaskState.journeyFollowUpOrNull(
                shopping,
                "I've picked up my cousin."
            )
        )
    }

    @Test
    fun repeatedConfirmationDoesNotAdvanceAnotherStop() {
        val first = continueJourney(
            task(),
            "I've picked up my cousin."
        )

        assertNull(
            ConversationTaskState.journeyFollowUpOrNull(
                first.first,
                "I've picked up my cousin."
            )
        )
    }
}