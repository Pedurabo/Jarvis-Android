package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinSafeCorrectionPolicyTest {
    private val invalid = """
        fun getNamesList() {
            val names = listOf("Alice", "Bob", "Charlie")
            // Add a new name
            names.add("Dave")
            println(names)
        }
    """.trimIndent()

    @Test fun proposesMutableListForObservedFailure() {
        val corrected = KotlinSafeCorrectionPolicy.suggest(invalid)
        assertTrue(corrected?.contains("val names = mutableListOf(") == true)
        assertFalse(corrected!!.contains("val names = listOf("))
        assertTrue(corrected.contains("names.add(\"Dave\")"))
    }

    @Test fun keepsOriginalInDisplayAfterSuggestedCorrection() {
        val decorated = KotlinAnswerWarningPolicy.decorate(
            "Write a Kotlin function that adds a name", invalid
        )
        assertTrue(decorated.startsWith("**Suggested correction (not compiler-verified):**"))
        assertTrue(decorated.contains("**Original generated answer (may be incorrect):**"))
        assertTrue(decorated.endsWith(invalid))
        assertTrue(decorated.indexOf("**Suggested correction") < decorated.indexOf("**Original generated answer"))
        assertTrue(decorated.contains("Kotlin accuracy warning"))
        assertTrue(decorated.contains("Suggested correction (not compiler-verified)"))
        assertTrue(decorated.contains("val names = mutableListOf("))
    }

    @Test fun decoratingTwiceIsIdempotent() {
        val once = KotlinAnswerWarningPolicy.decorate("Write a Kotlin function", invalid)
        assertEquals(once, KotlinAnswerWarningPolicy.decorate("Write a Kotlin function", once))
    }

    @Test fun doesNotRewriteCorrectCode() {
        assertNull(KotlinSafeCorrectionPolicy.suggest(
            "fun f() { val names = mutableListOf(1); names.add(2) }"
        ))
    }

    @Test fun refusesMultipleFunctions() {
        assertNull(KotlinSafeCorrectionPolicy.suggest(
            "fun f() { val names = listOf(1); names.add(2) }\nfun g() {}"
        ))
    }

    @Test fun refusesNestedControlFlow() {
        assertNull(KotlinSafeCorrectionPolicy.suggest(
            "fun f() { val names = listOf(1); if (true) { names.add(2) } }"
        ))
    }

    @Test fun refusesReassignment() {
        assertNull(KotlinSafeCorrectionPolicy.suggest(
            "fun f() { var names = listOf(1); names = listOf(2); names.add(3) }"
        ))
    }

    @Test fun normalConversationIsUnchanged() {
        assertEquals(invalid, KotlinAnswerWarningPolicy.decorate("Hello Jarvis", invalid))
    }
}
