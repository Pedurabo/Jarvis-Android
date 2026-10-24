package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KampalaOriginFollowUpResolverTest {

    @Test
    fun acceptsBareNtinda() {
        assertEquals(
            "ntinda",
            KampalaOriginFollowUpResolver.resolve("ntinda")
        )
    }

    @Test
    fun acceptsBareKololo() {
        assertEquals(
            "kololo",
            KampalaOriginFollowUpResolver.resolve("kololo")
        )
    }

    @Test
    fun acceptsFromNtinda() {
        assertEquals(
            "Ntinda",
            KampalaOriginFollowUpResolver.resolve("from Ntinda")
        )
    }

    @Test
    fun acceptsImInKatwe() {
        assertEquals(
            "Katwe",
            KampalaOriginFollowUpResolver.resolve("I'm in Katwe")
        )
    }

    @Test
    fun acceptsStartingFromRubaga() {
        assertEquals(
            "Rubaga",
            KampalaOriginFollowUpResolver.resolve(
                "starting from Rubaga"
            )
        )
    }

    @Test
    fun rejectsTechnicalQuestion() {
        assertNull(
            KampalaOriginFollowUpResolver.resolve(
                "Explain Kotlin"
            )
        )
    }

    @Test
    fun rejectsUnrelatedQuestion() {
        assertNull(
            KampalaOriginFollowUpResolver.resolve(
                "How does Android work?"
            )
        )
    }

    @Test
    fun rejectsCancel() {
        assertNull(
            KampalaOriginFollowUpResolver.resolve("cancel")
        )
    }

    @Test
    fun rejectsLongConversationalMessage() {
        assertNull(
            KampalaOriginFollowUpResolver.resolve(
                "Tell me about the history of Kampala Uganda"
            )
        )
    }

    @Test
    fun rejectsEmptyInput() {
        assertNull(
            KampalaOriginFollowUpResolver.resolve(" ")
        )
    }

    @Test
    fun preservesMultiwordOrigin() {
        assertEquals(
            "Old Kampala",
            KampalaOriginFollowUpResolver.resolve(
                "Old Kampala"
            )
        )
    }
}