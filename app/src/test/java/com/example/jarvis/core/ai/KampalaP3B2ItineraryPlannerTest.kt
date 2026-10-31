package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class KampalaP3B2ItineraryPlannerTest {

    private val message =
        "I need to get to National Theatre for a play, " +
            "but first I have to head to Aga Khan International " +
            "School to pick up my cousin. Then I have to drop " +
            "him at Garden City Mall before proceeding. " +
            "I also need to pick up Chinese food from a " +
            "restaurant before reaching the theatre."

    @Test
    fun extractsFourActivitiesInOrder() {
        val plan = requireNotNull(
            KampalaP3B2ItineraryPlanner.extract(message)
        )

        assertEquals(
            listOf(
                KampalaP3B2ItineraryPlanner.Activity.PICK_UP_PERSON,
                KampalaP3B2ItineraryPlanner.Activity.DROP_OFF_PERSON,
                KampalaP3B2ItineraryPlanner.Activity.PICK_UP_FOOD,
                KampalaP3B2ItineraryPlanner.Activity.FINAL_DESTINATION
            ),
            plan.stops.map { it.activity }
        )

        assertNull(plan.origin)
    }

    @Test
    fun retainsUserSuppliedNamesAndPurposes() {
        val plan = requireNotNull(
            KampalaP3B2ItineraryPlanner.extract(message)
        )

        assertEquals(
            "Aga Khan International School",
            plan.stops[0].requestedPlace
        )
        assertEquals("Pick up my cousin", plan.stops[0].purpose)
        assertEquals("Garden City Mall", plan.stops[1].requestedPlace)
        assertEquals("National Theatre", plan.stops[3].requestedPlace)
        assertEquals("Attend play", plan.stops[3].purpose)
    }

    @Test
    fun foodPickupDoesNotConsumeEarlierSentence() {
        val plan = requireNotNull(
            KampalaP3B2ItineraryPlanner.extract(message)
        )

        assertNull(plan.stops[2].requestedPlace)
        assertEquals("Pick up Chinese food", plan.stops[2].purpose)
        assertTrue(plan.stops[2].needsSelection)
    }

    @Test
    fun outputKeepsOriginalGuardSafetyWording() {
        val answer = requireNotNull(
            KampalaP3B1FJourneyGuard.noticeOrNull(message)
        )

        assertTrue(answer.contains("multi-stop journey"))
        assertTrue(answer.contains("starting point"))
        assertTrue(answer.contains("restaurant pickup"))
        assertTrue(answer.contains("No route"))
        assertFalse(answer.contains("Open directions:"))
        assertFalse(answer.contains("google.com/maps"))
    }

    @Test
    fun outputContainsOrderedActivityNames() {
        val plan = requireNotNull(
            KampalaP3B2ItineraryPlanner.extract(message)
        )
        val answer = KampalaP3B2ItineraryPlanner.describe(plan)

        val school = answer.indexOf("1. Aga Khan International School")
        val mall = answer.indexOf("2. Garden City Mall")
        val food = answer.indexOf("3. Restaurant not yet selected")
        val theatre = answer.indexOf("4. National Theatre")

        assertTrue(
            school >= 0 &&
                school < mall &&
                mall < food &&
                food < theatre
        )
    }

    @Test
    fun unrelatedRequestsAreIgnored() {
        assertNull(
            KampalaP3B2ItineraryPlanner.extract(
                "Find Chinese restaurants near Garden City."
            )
        )
        assertNull(
            KampalaP3B2ItineraryPlanner.extract(
                "Explain Kotlin coroutines and Java threads."
            )
        )
    }

    @Test
    fun incompleteItineraryFallsBackSafely() {
        assertNull(
            KampalaP3B2ItineraryPlanner.extract(
                "I need to get to National Theatre for a play, " +
                    "but first I must visit school."
            )
        )
    }

    @Test
    fun reversedActivitiesAreNotSilentlyReordered() {
        val changed = message.replace(
            "Then I have to drop him at Garden City Mall before proceeding. " +
                "I also need to pick up Chinese food from a restaurant",
            "Then I have to pick up Chinese food from a restaurant. " +
                "I also have to drop him at Garden City Mall before proceeding"
        )
        assertNull(KampalaP3B2ItineraryPlanner.extract(changed))
    }
}