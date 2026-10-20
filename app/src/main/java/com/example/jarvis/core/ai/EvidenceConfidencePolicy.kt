package com.example.jarvis.core.ai

/**
 * Conservative confidence presentation based on evidence provenance.
 *
 * These categories are not calibrated probabilities of correctness.
 * A catalog-backed result is not independent verification of
 * operating hours, prices, inventory or present-day availability.
 *
 * The current ConfidenceLevel enum has no NOT_ASSESSED value.
 * LOW is used for unverified outputs and coverage notices until
 * the UI can display evidence status directly instead of a score.
 */
internal object EvidenceConfidencePolicy {

    fun level(
        origin: AssistantController.ResponseOrigin
    ): ConfidenceLevel = when (origin) {

        AssistantController.ResponseOrigin.DIRECTORY_RESULT ->
            ConfidenceLevel.MEDIUM

        AssistantController.ResponseOrigin.CURATED_FACT ->
            ConfidenceLevel.MEDIUM

        AssistantController.ResponseOrigin.KNOWLEDGE_RESULT ->
            ConfidenceLevel.MEDIUM

        AssistantController.ResponseOrigin.DIRECTORY_NOTICE,
        AssistantController.ResponseOrigin.KNOWLEDGE_NOTICE,
        AssistantController.ResponseOrigin.AI_RESPONSE ->
            ConfidenceLevel.LOW
    }
}
