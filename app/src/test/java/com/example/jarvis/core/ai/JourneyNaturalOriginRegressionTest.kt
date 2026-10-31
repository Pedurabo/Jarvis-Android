package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class JourneyNaturalOriginRegressionTest {

    private val goal =
        "I need to get to National Theatre for a play, " +
            "but first I have to head to Aga Khan International " +
            "School to pick up my cousin. Then I have to drop " +
            "him at Garden City Mall before proceeding. " +
            "I also need to pick up Chinese food from a " +
            "restaurant before reaching the theatre."

    private fun start() = ConversationTaskState.Task(
        kind = ConversationTaskState.Kind.NAVIGATION,
        goal = goal
    )

    private fun follow(
        task: ConversationTaskState.Task,
        text: String
    ) = requireNotNull(
        ConversationTaskState.journeyFollowUpOrNull(task, text)
    )

    @Test
    fun exactSamsungSequenceWorks() {
        val origin = follow(start(), "am at serena hotel kigo")

        assertEquals(
            "serena hotel kigo",
            origin.first.get(
                ConversationTaskState.Field.ORIGIN
            )?.value
        )

        assertTrue(
            origin.second.contains(
                "Starting point: serena hotel kigo"
            )
        )

        val completed = follow(
            origin.first,
            "I've picked up my cousin"
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

        val next = follow(completed.first, "what's next")

        assertTrue(
            next.second.contains(
                "Next planned stop: Garden City Mall"
            )
        )

        assertTrue(
            next.second.contains(
                "Starting point: serena hotel kigo"
            )
        )

        assertEquals(goal, next.first.goal)
    }

    @Test
    fun previouslySupportedOriginsStillWork() {
        val phrases = listOf(
            "I'm currently at Serena Hotel in Kigo.",
            "I'm at Serena Hotel Kigo",
            "I am at Serena Hotel Kigo",
            "currently at Serena Hotel Kigo",
            "am at Serena Hotel Kigo",
            "my starting location is Serena Hotel Kigo"
        )

        phrases.forEach { phrase ->
            val result =
                ConversationTaskState.journeyFollowUpOrNull(
                    start(),
                    phrase
                )

            assertNotNull("Unrecognized: $phrase", result)
        }
    }

    @Test
    fun unrelatedRequestsRemainAvailable() {
        listOf(
            "Find banks near me",
            "Find Case Clinic in Nakasero",
            "Show hotels in Kampala",
            "Find schools nearby",
            "Explain first aid for burns"
        ).forEach { phrase ->
            assertNull(
                ConversationTaskState.journeyFollowUpOrNull(
                    start(),
                    phrase
                )
            )
        }
    }

    @Test
    fun tentativePickupDoesNotCompleteTask() {
        assertNull(
            ConversationTaskState.journeyFollowUpOrNull(
                start(),
                "I might pick up my cousin tomorrow"
            )
        )
    }
}