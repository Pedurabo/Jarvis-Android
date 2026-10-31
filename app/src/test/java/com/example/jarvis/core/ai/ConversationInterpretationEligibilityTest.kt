package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationInterpretationEligibilityTest {

    private fun activeTask() = ConversationTaskState.Task(
        kind = ConversationTaskState.Kind.MEDICAL_ASSISTANCE,
        goal = "Reach medical care",
        fields = mapOf(
            ConversationTaskState.Field.DESTINATION to
                ConversationTaskState.Fact(
                    "Case Clinic",
                    ConversationTaskState.Provenance.USER_SUPPLIED
                )
        ),
        requiresUrgentCare = true
    )

    private fun check(
        message: String,
        task: ConversationTaskState.Task? = activeTask(),
        enabled: Boolean = true,
        protected: Boolean = false
    ) = ConversationInterpretationEligibility.evaluate(
        task = task,
        message = message,
        explicitlyEnabled = enabled,
        protectedRoute = protected
    )

    @Test
    fun disabledByDefault() {
        assertEquals(
            ConversationInterpretationEligibility.Decision.DISABLED,
            check("think it's in Nakasero", enabled = false)
        )
    }

    @Test
    fun rejectsWithoutActiveTask() {
        assertEquals(
            ConversationInterpretationEligibility.Decision.NO_ACTIVE_TASK,
            check("think it's in Nakasero", task = null)
        )
    }

    @Test
    fun protectedRouteAlwaysTakesPriority() {
        assertEquals(
            ConversationInterpretationEligibility.Decision.PROTECTED_ROUTE,
            check("think it's in Nakasero", protected = true)
        )
    }

    @Test
    fun shortFollowUpIsEligible() {
        assertEquals(
            ConversationInterpretationEligibility.Decision.ELIGIBLE,
            check("think it's in Nakasero")
        )
    }

    @Test
    fun correctionCanBeEligible() {
        assertEquals(
            ConversationInterpretationEligibility.Decision.ELIGIBLE,
            check("Actually, I'm starting from Ntinda")
        )
    }

    @Test
    fun blankMessageIsRejected() {
        assertEquals(
            ConversationInterpretationEligibility.Decision.INVALID_MESSAGE,
            check("   ")
        )
    }

    @Test
    fun longInputIsRejected() {
        assertEquals(
            ConversationInterpretationEligibility.Decision.INVALID_MESSAGE,
            check("a".repeat(181))
        )
    }

    @Test
    fun lengthyStandaloneMessageIsSkipped() {
        val message = List(30) { "word" }.joinToString(" ")

        assertEquals(
            ConversationInterpretationEligibility.Decision.STANDALONE_MESSAGE,
            check(message)
        )
    }

    @Test
    fun multilineControlCharactersAreRejected() {
        assertEquals(
            ConversationInterpretationEligibility.Decision.INVALID_MESSAGE,
            check("hello\nworld")
        )
    }
}