package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class TechnicalTaskPolicyTest {

    private fun check(
        expected: TechnicalTaskPolicy.Kind,
        query: String
    ) {
        assertEquals(
            query,
            expected,
            TechnicalTaskPolicy.classify(query)
        )
    }

    @Test
    fun simpleKotlinExplanations() {
        check(
            TechnicalTaskPolicy.Kind.EXPLANATION,
            "Explain Kotlin variables"
        )
        check(
            TechnicalTaskPolicy.Kind.EXPLANATION,
            "What is a Kotlin data class?"
        )
        check(
            TechnicalTaskPolicy.Kind.EXPLANATION,
            "How does Kotlin null safety work?"
        )
    }

    @Test
    fun codeGenerationRequiresAccuracyAttention() {
        check(
            TechnicalTaskPolicy.Kind.CODE_TASK,
            "Write a Kotlin function"
        )
        check(
            TechnicalTaskPolicy.Kind.CODE_TASK,
            "Create a MutableList in Kotlin"
        )
        check(
            TechnicalTaskPolicy.Kind.CODE_TASK,
            "Implement an Android ViewModel"
        )
    }

    @Test
    fun debuggingRequiresAccuracyAttention() {
        check(
            TechnicalTaskPolicy.Kind.CODE_TASK,
            "Fix this Kotlin compilation error"
        )
        check(
            TechnicalTaskPolicy.Kind.CODE_TASK,
            "Debug a coroutine function"
        )
        check(
            TechnicalTaskPolicy.Kind.CODE_TASK,
            "Review this Java class"
        )
    }

    @Test
    fun deviceRequestsAreNotProgrammingTasks() {
        check(
            TechnicalTaskPolicy.Kind.GENERAL,
            "Call +12025550100"
        )
        check(
            TechnicalTaskPolicy.Kind.GENERAL,
            "Open WhatsApp"
        )
        check(
            TechnicalTaskPolicy.Kind.GENERAL,
            "Launch the calculator app"
        )
    }

    @Test
    fun ordinaryQueriesRemainGeneral() {
        check(
            TechnicalTaskPolicy.Kind.GENERAL,
            "What's the weather in Kampala?"
        )
        check(
            TechnicalTaskPolicy.Kind.GENERAL,
            "Hello Jarvis"
        )
        check(
            TechnicalTaskPolicy.Kind.GENERAL,
            ""
        )
    }
}