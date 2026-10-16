package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinAnswerWarningPolicyTest {
    private val query = "Write a Kotlin function that creates a list of names and adds another name."
    private val invalidCode = """
        ```kotlin
        fun getNamesList() {
            val names = listOf("Alice", "Bob")
            names.add("Carol")
            println(names)
        }
        ```
    """.trimIndent()
    private val validCode = """
        ```kotlin
        fun getNamesList() {
            val names = mutableListOf("Alice", "Bob")
            names.add("Carol")
            println(names)
        }
        ```
    """.trimIndent()

    @Test fun correctionAppearsBeforeOriginal() {
        val result = KotlinAnswerWarningPolicy.decorate(query, invalidCode)
        assertTrue(result.startsWith("**Suggested correction (not compiler-verified):**"))
        assertTrue(result.contains("val names = mutableListOf(\"Alice\", \"Bob\")"))
        assertTrue(result.indexOf("**Suggested correction") < result.indexOf("**Original generated answer"))
        assertTrue(result.indexOf("**Original generated answer") < result.lastIndexOf(invalidCode))
    }

    @Test fun warningIsStillVisible() {
        val result = KotlinAnswerWarningPolicy.decorate(query, invalidCode)
        assertTrue(result.contains("**Kotlin accuracy warning:**"))
        assertTrue(result.contains("This code has not been compiler-verified."))
    }

    @Test fun originalIsPreservedExactly() {
        val result = KotlinAnswerWarningPolicy.decorate(query, invalidCode)
        assertTrue(result.endsWith(invalidCode))
    }

    @Test fun validCodeIsUnchanged() = assertEquals(validCode, KotlinAnswerWarningPolicy.decorate(query, validCode))

    @Test fun nonKotlinTaskIsUnchanged() = assertEquals(invalidCode, KotlinAnswerWarningPolicy.decorate("Write a Python function that adds a name", invalidCode))

    @Test fun ordinaryConversationIsUnchanged() = assertEquals(invalidCode, KotlinAnswerWarningPolicy.decorate("Hello Jarvis", invalidCode))

    @Test fun incorrectTeachingExampleIsUnchanged() {
        val answer = """
            This example is incorrect:
            ```kotlin
            val names = listOf("Alice")
            names.add("Bob")
            ```
            Use mutableListOf instead.
        """.trimIndent()
        assertEquals(answer, KotlinAnswerWarningPolicy.decorate("Write a Kotlin explanation showing invalid code", answer))
    }

    @Test fun decorationIsIdempotent() {
        val once = KotlinAnswerWarningPolicy.decorate(query, invalidCode)
        assertEquals(once, KotlinAnswerWarningPolicy.decorate(query, once))
    }

    @Test fun complexCodeFallsBackToOriginalThenWarning() {
        val complex = """
            ```kotlin
            fun first() { val names = listOf("A"); names.add("B") }
            fun second() { println("hello") }
            ```
        """.trimIndent()
        val result = KotlinAnswerWarningPolicy.decorate(query, complex)
        assertTrue(result.startsWith(complex))
        assertTrue(result.contains("**Kotlin accuracy warning:**"))
        assertFalse(result.contains("**Suggested correction"))
    }
}
