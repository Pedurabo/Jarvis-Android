package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionalDiscoveryResponsePolicyTest {

    private fun response(query: String) =
        CompositionalDiscoveryResponsePolicy.notice(query)

    @Test
    fun compoundSpiceTripPreservesEachTask() {
        val text = response(
            "Which market has spices for cooking, how do I get there, " +
                "and can I order a SafeBoda or get me an Uber?"
        )

        assertNotNull(text)
        assertTrue(text!!.contains("Market product availability"))
        assertTrue(text.contains("Directions"))
        assertTrue(text.contains("safeboda assistance"))
        assertTrue(text.contains("uber assistance"))
    }

    @Test
    fun compoundRequestNeverClaimsBooking() {
        val text = response(
            "Which market has spices for cooking, how do I get there, " +
                "and can I order a SafeBoda?"
        )!!

        assertTrue(text.contains("No ride has been booked"))
        assertFalse(text.contains("Your ride is confirmed"))
    }

    @Test
    fun marketInventoryIsNotInferredFromPlaceRecords() {
        val text = response(
            "Which market has spices for cooking?"
        )

        assertNotNull(text)
        assertTrue(text!!.contains("cannot verify current stock"))
    }

    @Test
    fun nakaseroInventoryRequiresEvidence() {
        val text = response(
            "What kinds of food are being sold in Nakasero Market?"
        )

        assertNotNull(text)
        assertTrue(text!!.contains("Market product availability"))
    }

    @Test
    fun chineseCuisineConstraintIsPreserved() {
        val text = response(
            "Where can I go to have Chinese cuisine?"
        )

        assertNotNull(text)
        assertTrue(text!!.contains("chinese restaurant discovery"))
        assertTrue(text.contains("cannot verify chinese cuisine"))
    }

    @Test
    fun turkishCuisineAndAreaAreRecognized() {
        val plan = DiscoveryEvidenceCoordinator.interpretAndPlan(
            "Where can I have a Turkish meal in Makindye?"
        )

        assertTrue(plan.decisions.any {
            it.task.cuisine == "turkish" &&
                it.task.area == "makindye"
        })
    }

    @Test
    fun normalHotelSearchKeepsVerifiedRouter() {
        assertNull(response("Find hotels in Kampala"))
    }

    @Test
    fun normalMarketSearchKeepsVerifiedRouter() {
        assertNull(response("Find markets in Kampala"))
    }

    @Test
    fun generalProgrammingRemainsAvailable() {
        assertNull(response("Explain dependency injection in Kotlin"))
        assertNull(response("Write Kotlin code for a restaurant app"))
    }

    @Test
    fun existingPhoneActionsRemainUntouched() {
        assertNull(response("Open Android Settings"))
        assertNull(response("Call the palace"))
    }

    @Test
    fun greetingsRemainAvailable() {
        assertNull(response("Hello Jarvis"))
    }

    @Test
    fun noticeDoesNotClaimVerifiedSearchWasPerformed() {
        val text = response(
            "Which market has spices for cooking?"
        )!!

        assertFalse(text.contains("I found a market selling spices"))
        assertTrue(text.contains("No inventory, route"))
    }
}
