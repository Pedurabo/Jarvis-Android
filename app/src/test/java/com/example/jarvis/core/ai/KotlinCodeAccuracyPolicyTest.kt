package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinCodeAccuracyPolicyTest {

    @Test
    fun detectsReadOnlyListMutation() {
        assertTrue(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(
                """
                fun names() {
                    val names = listOf("Alice", "Bob")
                    names.add("Carol")
                    println(names)
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun allowsMutableListMutation() {
        assertFalse(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(
                """
                fun names() {
                    val names = mutableListOf("Alice", "Bob")
                    names.add("Carol")
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun allowsNonMutatingListAddition() {
        assertFalse(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(
                """
                val names = listOf("Alice", "Bob")
                val updated = names + "Carol"
                println(updated)
                """.trimIndent()
            )
        )
    }

    @Test
    fun differentVariableDoesNotTriggerDetection() {
        assertFalse(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(
                """
                val original = listOf("Alice")
                val editable = mutableListOf("Bob")
                editable.add("Carol")
                """.trimIndent()
            )
        )
    }

    @Test
    fun ordinaryExplanationDoesNotTriggerDetection() {
        assertFalse(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(
                "Kotlin lists are read-only by default."
            )
        )
    }

    @Test
    fun detectsPreviouslyObservedExample() {
        assertTrue(
            KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(
                """
                fun getNamesList() {
                    val names = listOf("Alice", "Bob", "Charlie")
                    names.add("Dave")
                    println(names)
                }
                """.trimIndent()
            )
        )
    }
}