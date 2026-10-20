package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaGeographicGroundingCdi43Test {

    @Test
    fun sixHillsQuestionIsProtected() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "What can you tell me about the 6 hills of Kampala?"
            )
        )
    }

    @Test
    fun traditionalSevenHillsRemainProtected() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "How about the 7 hills of Kampala, can you list them?"
            )
        )
    }

    @Test
    fun writtenNumberVariationsAreRecognized() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "Tell me about the six hills of Kampala"
            )
        )
    }

    @Test
    fun eightHillsQuestionDoesNotInventGeography() {
        assertNotNull(
            KampalaFactualReliabilityBoundary.notice(
                "What are the eight hills in Kampala?"
            )
        )
    }

    @Test
    fun rejectsObservedCambodiaFabrication() {
        assertTrue(
            KampalaFactualAccuracyPolicy.shouldReject(
                "What can you tell me about the 6 hills of Kampala?",
                "The hills are geological formations in Kampala, Cambodia."
            )
        )
    }

    @Test
    fun rejectsKampalaInCambodiaStatement() {
        assertTrue(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Where is Kampala?",
                "Kampala is a major city in Cambodia."
            )
        )
    }

    @Test
    fun retainsIndiaContradictionProtection() {
        assertTrue(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Where is Kampala?",
                "Kampala is located in southern India."
            )
        )
    }

    @Test
    fun correctUgandaCountryAttributionPasses() {
        assertFalse(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Where is Kampala?",
                "Kampala is Uganda's capital city."
            )
        )
    }

    @Test
    fun unrelatedCambodiaQuestionPasses() {
        assertFalse(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Where is Phnom Penh?",
                "Phnom Penh is in Cambodia."
            )
        )
    }

    @Test
    fun unrelatedProgrammingQuestionNotIntercepted() {
        assertTrue(
            KampalaFactualReliabilityBoundary.notice(
                "Explain dependency injection in Kotlin"
            ) == null
        )
    }

    @Test
    fun verifiedDirectoryRequestNotIntercepted() {
        assertTrue(
            KampalaFactualReliabilityBoundary.notice(
                "Find hotels in Kampala"
            ) == null
        )
    }

    @Test
    fun androidActionNotIntercepted() {
        assertTrue(
            KampalaFactualReliabilityBoundary.notice(
                "Open Android Settings"
            ) == null
        )
    }
}
