package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class EvidenceConfidencePolicyTest {

    @Test
    fun verifiedDirectoryRecordDoesNotClaimHighConfidence() {
        assertEquals(
            ConfidenceLevel.MEDIUM,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.DIRECTORY_RESULT
            )
        )
    }

    @Test
    fun knowledgeRecordDoesNotClaimHighConfidence() {
        assertEquals(
            ConfidenceLevel.MEDIUM,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.KNOWLEDGE_RESULT
            )
        )
    }

    @Test
    fun emptyDirectoryNoticeIsLow() {
        assertEquals(
            ConfidenceLevel.LOW,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.DIRECTORY_NOTICE
            )
        )
    }

    @Test
    fun knowledgeCoverageNoticeIsLow() {
        assertEquals(
            ConfidenceLevel.LOW,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.KNOWLEDGE_NOTICE
            )
        )
    }

    @Test
    fun ordinaryModelAnswerIsUnverified() {
        assertEquals(
            ConfidenceLevel.LOW,
            EvidenceConfidencePolicy.level(
                AssistantController.ResponseOrigin.AI_RESPONSE
            )
        )
    }

    @Test
    fun noOriginReceivesHighWithoutAdditionalVerification() {
        AssistantController.ResponseOrigin.values().forEach { origin ->
            val result = EvidenceConfidencePolicy.level(origin)

            assertEquals(
                "Unexpected confidence category for $origin",
                false,
                result == ConfidenceLevel.HIGH
            )
        }
    }
}
