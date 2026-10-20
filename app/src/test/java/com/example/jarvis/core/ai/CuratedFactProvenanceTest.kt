package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CuratedFactProvenanceTest {

    @Test
    fun curatedFactHasMediumConfidence() {
        assertEquals(
            ConfidenceLevel.MEDIUM,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.CURATED_FACT
            )
        )
    }

    @Test
    fun modelAnswerRemainsLow() {
        assertEquals(
            ConfidenceLevel.LOW,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.AI_RESPONSE
            )
        )
    }

    @Test
    fun knowledgeAndDirectoryRemainMedium() {
        assertEquals(
            ConfidenceLevel.MEDIUM,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.KNOWLEDGE_RESULT
            )
        )
        assertEquals(
            ConfidenceLevel.MEDIUM,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.DIRECTORY_RESULT
            )
        )
    }

    @Test
    fun noticesRemainLow() {
        assertEquals(
            ConfidenceLevel.LOW,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.KNOWLEDGE_NOTICE
            )
        )
        assertEquals(
            ConfidenceLevel.LOW,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.DIRECTORY_NOTICE
            )
        )
    }

    @Test
    fun locationIsCuratedButHillsAreNot() {
        assertEquals(
            "Kampala is the capital city of Uganda, " +
                "a country in East Africa.",
            KampalaTrustedFactPolicy.answer("Where is Kampala?")
        )

        assertNull(
            KampalaTrustedFactPolicy.answer(
                "What are the six hills of Kampala?"
            )
        )
    }

    @Test
    fun actionsAndTechnicalQueriesAreNotCurated() {
        assertNull(
            KampalaTrustedFactPolicy.answer("Open Android Settings")
        )
        assertNull(
            KampalaTrustedFactPolicy.answer(
                "Explain dependency injection in Kotlin"
            )
        )
    }
}