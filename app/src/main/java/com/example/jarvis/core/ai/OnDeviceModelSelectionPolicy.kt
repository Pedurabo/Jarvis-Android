package com.example.jarvis.core.ai

/**
 * Pure model-selection policy.
 *
 * Does not download, initialize, or activate any model.
 */
internal object OnDeviceModelSelectionPolicy {

    const val DEFAULT_MODEL_ID = "llama-3.2-1b"
    const val EXPERIMENTAL_MODEL_ID = "gemma-3-1b"

    data class ModelDescriptor(
        val id: String,
        val displayName: String,
        val fileName: String,
        val experimental: Boolean
    )

    val defaultModel = ModelDescriptor(
        DEFAULT_MODEL_ID,
        "Llama 3.2 1B",
        "llama3_2_1b.bin",
        false
    )

    // Candidate filename only; compatibility is not established.
    val experimentalModel = ModelDescriptor(
        EXPERIMENTAL_MODEL_ID,
        "Gemma 3 1B",
        "gemma3_1b.task",
        true
    )

    val catalog = listOf(defaultModel, experimentalModel)

    enum class Reason {
        DEFAULT_SELECTED,
        EXPERIMENTAL_SELECTED,
        EXPERIMENTAL_NOT_APPROVED,
        EXPERIMENTAL_NOT_AVAILABLE,
        EXPERIMENTAL_NOT_VERIFIED,
        UNKNOWN_MODEL
    }

    data class Selection(
        val selected: ModelDescriptor,
        val reason: Reason
    )

    fun select(
        requestedModelId: String?,
        availableModelIds: Set<String>,
        verifiedCompatibleIds: Set<String>,
        allowExperimental: Boolean
    ): Selection {
        if (requestedModelId == null ||
            requestedModelId == DEFAULT_MODEL_ID
        ) {
            return Selection(defaultModel, Reason.DEFAULT_SELECTED)
        }

        val candidate = catalog.firstOrNull {
            it.id == requestedModelId
        } ?: return Selection(defaultModel, Reason.UNKNOWN_MODEL)

        if (candidate.experimental && !allowExperimental) {
            return Selection(
                defaultModel,
                Reason.EXPERIMENTAL_NOT_APPROVED
            )
        }

        if (candidate.id !in availableModelIds) {
            return Selection(
                defaultModel,
                Reason.EXPERIMENTAL_NOT_AVAILABLE
            )
        }

        if (candidate.id !in verifiedCompatibleIds) {
            return Selection(
                defaultModel,
                Reason.EXPERIMENTAL_NOT_VERIFIED
            )
        }

        return Selection(candidate, Reason.EXPERIMENTAL_SELECTED)
    }
}