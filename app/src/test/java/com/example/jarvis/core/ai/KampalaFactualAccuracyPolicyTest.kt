package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaFactualAccuracyPolicyTest {

    @Test
    fun rejectsObservedFalseLocation() {
        assertTrue(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Tell me about Kampala history",
                "Kampala is a historical region in southern India."
            )
        )
    }

    @Test
    fun rejectsObservedFalseRiver() {
        assertTrue(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Tell me about Kampala history",
                "Geographic location: Situated on the banks of the Kaveri River."
            )
        )
    }

    @Test
    fun allowsCorrectUgandanDescription() {
        assertFalse(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Tell me about Kampala history",
                "Kampala is Uganda's capital and developed around historic hills."
            )
        )
    }

    @Test
    fun allowsHistoricalComparisonWithIndia() {
        assertFalse(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Compare the history of Kampala and India",
                "Kampala is Uganda's capital. India has a different historical background."
            )
        )
    }

    @Test
    fun doesNotInterfereWithUnrelatedQuestions() {
        assertFalse(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Tell me about Indian geography",
                "The Kaveri River flows through southern India."
            )
        )
    }

    @Test
    fun rejectsIncorrectLocationInOrdinaryCityQuestion() {
        assertTrue(
            KampalaFactualAccuracyPolicy.shouldReject(
                "Where is Kampala?",
                "Kampala is located in southern India."
            )
        )
    }
}