package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinCodeAccuracyEdgeCaseTest {

    @Test
    fun incorrectCodeShownAsExplanationIsNotFlagged() {
        val answer = """
            The following example is incorrect:

            ```kotlin
            val names = listOf("Alice", "Bob")
            names.add("Carol")
            ```

            Use mutableListOf instead.
        """.trimIndent()

        assertFalse(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(answer)
        )
    }

    @Test
    fun codeInsideCommentIsNotFlagged() {
        val code = """
            // val names = listOf("Alice")
            // names.add("Bob")
            println("This is just a comment")
        """.trimIndent()

        assertFalse(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(code)
        )
    }

    @Test
    fun validMutationInAnotherScopeIsNotFlagged() {
        val code = """
            fun first() {
                val names = listOf("Alice")
                println(names)
            }

            fun second() {
                val names = mutableListOf("Bob")
                names.add("Carol")
            }
        """.trimIndent()

        assertFalse(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(code)
        )
    }

    @Test
    fun actualInvalidCodeStillFlagged() {
        val code = """
            fun names() {
                val names = listOf("Alice", "Bob")
                names.add("Carol")
            }
        """.trimIndent()

        assertTrue(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(code)
        )
    }
}