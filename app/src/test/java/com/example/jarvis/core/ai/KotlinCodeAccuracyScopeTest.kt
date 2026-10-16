package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinCodeAccuracyScopeTest {

    @Test
    fun stringLiteralContainingInvalidCodeIsNotFlagged() {
        val code = """
            fun example() {
                val documentation = "val names = listOf(1); names.add(2)"
                println(documentation)
            }
        """.trimIndent()

        assertFalse(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(code)
        )
    }

    @Test
    fun sameNameInSeparateFunctionsIsNotFlagged() {
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
    fun nestedScopeShadowingIsNotFlagged() {
        val code = """
            fun example() {
                val names = listOf("Alice")
                run {
                    val names = mutableListOf("Bob")
                    names.add("Carol")
                }
            }
        """.trimIndent()

        assertFalse(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(code)
        )
    }

    @Test
    fun invalidMutationInSameScopeStillFlagged() {
        val code = """
            fun example() {
                val names = listOf("Alice")
                names.add("Bob")
            }
        """.trimIndent()

        assertTrue(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(code)
        )
    }
}