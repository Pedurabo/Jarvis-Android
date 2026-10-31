package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaEmergencyResponsePolicyTest {

    @Test
    fun dogBiteEmergencyGetsImmediateGuidance() {
        val response = KampalaEmergencyResponsePolicy.respondOrNull(
            "My neighbour has just been bitten by a dog. " +
                "We are on Luwum Street. Can you help me get to Case Clinic?"
        )

        assertNotNull(response)
        val answer = response!!

        assertTrue(answer.contains("soap and running water"))
        assertTrue(answer.contains("15 minutes"))
        assertTrue(answer.contains("rabies"))
        assertTrue(answer.contains("tetanus"))
        assertTrue(answer.contains("urgently"))
    }

    @Test
    fun doesNotClaimToHaveTakenEmergencyActions() {
        val response = KampalaEmergencyResponsePolicy.respondOrNull(
            "A dog has bitten my friend today. Help!"
        )!!

        assertTrue(
            response.contains(
                "not verified any clinic address or contacted emergency services"
            )
        )
        assertFalse(
            response.contains("I need immediate medical assistance")
        )
        assertFalse(
            response.contains("[End of transcript]")
        )
    }

    @Test
    fun educationalQuestionsDoNotTriggerEmergencyFlow() {
        assertNull(
            KampalaEmergencyResponsePolicy.respondOrNull(
                "Explain dog bite first aid for a school presentation"
            )
        )
    }

    @Test
    fun unrelatedNavigationIsUnaffected() {
        assertNull(
            KampalaEmergencyResponsePolicy.respondOrNull(
                "How do I get to Kololo from Makindye?"
            )
        )
    }

    @Test
    fun unrelatedHealthQuestionIsUnaffected() {
        assertNull(
            KampalaEmergencyResponsePolicy.respondOrNull(
                "Where can I find a pharmacy in Kampala?"
            )
        )
    }
}