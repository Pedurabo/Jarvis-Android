package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OnDeviceModelSelectionPolicyTest {

    private val policy = OnDeviceModelSelectionPolicy

    @Test
    fun defaultModelRemainsLlama() {
        val result = policy.select(
            null, emptySet(), emptySet(), false
        )

        assertEquals(policy.DEFAULT_MODEL_ID, result.selected.id)
        assertEquals(
            OnDeviceModelSelectionPolicy.Reason.DEFAULT_SELECTED,
            result.reason
        )
    }

    @Test
    fun approvalIsRequired() {
        val result = policy.select(
            policy.EXPERIMENTAL_MODEL_ID,
            setOf(policy.EXPERIMENTAL_MODEL_ID),
            setOf(policy.EXPERIMENTAL_MODEL_ID),
            false
        )

        assertEquals(policy.DEFAULT_MODEL_ID, result.selected.id)
        assertEquals(
            OnDeviceModelSelectionPolicy.Reason.EXPERIMENTAL_NOT_APPROVED,
            result.reason
        )
    }

    @Test
    fun localAvailabilityIsRequired() {
        val result = policy.select(
            policy.EXPERIMENTAL_MODEL_ID,
            emptySet(),
            setOf(policy.EXPERIMENTAL_MODEL_ID),
            true
        )

        assertEquals(policy.DEFAULT_MODEL_ID, result.selected.id)
        assertEquals(
            OnDeviceModelSelectionPolicy.Reason.EXPERIMENTAL_NOT_AVAILABLE,
            result.reason
        )
    }

    @Test
    fun compatibilityVerificationIsRequired() {
        val result = policy.select(
            policy.EXPERIMENTAL_MODEL_ID,
            setOf(policy.EXPERIMENTAL_MODEL_ID),
            emptySet(),
            true
        )

        assertEquals(policy.DEFAULT_MODEL_ID, result.selected.id)
        assertEquals(
            OnDeviceModelSelectionPolicy.Reason.EXPERIMENTAL_NOT_VERIFIED,
            result.reason
        )
    }

    @Test
    fun verifiedAvailableApprovedCandidateCanBeSelected() {
        val result = policy.select(
            policy.EXPERIMENTAL_MODEL_ID,
            setOf(policy.EXPERIMENTAL_MODEL_ID),
            setOf(policy.EXPERIMENTAL_MODEL_ID),
            true
        )

        assertEquals(policy.EXPERIMENTAL_MODEL_ID, result.selected.id)
        assertEquals(
            OnDeviceModelSelectionPolicy.Reason.EXPERIMENTAL_SELECTED,
            result.reason
        )
    }

    @Test
    fun unknownModelFallsBack() {
        val result = policy.select(
            "unknown-model",
            setOf("unknown-model"),
            setOf("unknown-model"),
            true
        )

        assertEquals(policy.DEFAULT_MODEL_ID, result.selected.id)
        assertEquals(
            OnDeviceModelSelectionPolicy.Reason.UNKNOWN_MODEL,
            result.reason
        )
    }

    @Test
    fun installedModelFilenameIsPreserved() {
        assertEquals(
            "llama3_2_1b.bin",
            policy.defaultModel.fileName
        )
        assertFalse(policy.defaultModel.experimental)
    }
}