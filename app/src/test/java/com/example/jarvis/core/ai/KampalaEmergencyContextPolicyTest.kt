package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaEmergencyContextPolicyTest {

    private val initialRequest =
        "My neighbour has just been bitten by a dog. " +
            "We're on Luwum Street. Can you help me get to Case Clinic?"

    @Test
    fun capturesUserSuppliedClinicAndOrigin() {
        val context = KampalaEmergencyContextPolicy.capture(initialRequest)

        assertNotNull(context)
        assertEquals("Case Clinic", context!!.destination)
        assertEquals("Luwum Street", context.origin)
    }

    @Test
    fun respondsToUnverifiedNeighbourhoodSuggestion() {
        val context =
            KampalaEmergencyContextPolicy.capture(initialRequest)!!

        val response =
            KampalaEmergencyContextPolicy.followUpResponse(
                context,
                "I think it is in Nakasero."
            )

        assertNotNull(response)
        assertTrue(response!!.contains("Case Clinic"))
        assertTrue(response.contains("Nakasero"))
        assertTrue(response.contains("Luwum Street"))
        assertTrue(response.contains("haven't verified"))
        assertTrue(response.contains("rabies"))
    }

    @Test
    fun doesNotPresentSuggestedAreaAsVerified() {
        val context =
            KampalaEmergencyContextPolicy.capture(initialRequest)!!

        val response =
            KampalaEmergencyContextPolicy.followUpResponse(
                context,
                "I think it is in Nakasero."
            )!!

        assertFalse(response.contains("confirmed address"))
        assertFalse(response.contains("I have verified"))
        assertFalse(response.contains("123 Main Street"))
        assertFalse(response.contains("Open directions:"))
    }

    @Test
    fun unrelatedFollowUpDoesNotMatch() {
        val context =
            KampalaEmergencyContextPolicy.capture(initialRequest)!!

        assertNull(
            KampalaEmergencyContextPolicy.followUpResponse(
                context,
                "What is the weather like?"
            )
        )
    }

    @Test
    fun normalNavigationDoesNotCreateEmergencyContext() {
        assertNull(
            KampalaEmergencyContextPolicy.capture(
                "Can you get me directions to Watoto Church downtown?"
            )
        )
    }

    @Test
    fun unspecifiedClinicDoesNotCreateFabricatedDestination() {
        assertNull(
            KampalaEmergencyContextPolicy.capture(
                "Someone was bitten by a dog. Where is the nearest clinic?"
            )
        )
    }
}