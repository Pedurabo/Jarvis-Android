package com.example.jarvis.core.ai

import org.junit.Assert.*
import org.junit.Test

class PublishedPlacePresentationA67CTest {

    private val place =
        requireNotNull(
            KampalaSourceBackedPlacePolicy.resolveOrNull(
                "Watoto Church Downtown"
            )
        )

    private fun sourceBackedMessage(): String =
        "${place.name} is listed at ${place.address}. " +
            "Official source: ${place.sourceUrl}. " +
            "No route has been calculated."

    @Test
    fun fullOfficialRecordGetsPublishedAddressLabel() {
        assertEquals(
            "Official published address · Confirm map details",
            KampalaPublishedPlacePresentationPolicy
                .directoryNoticeLabel(sourceBackedMessage())
        )
    }

    @Test
    fun missingAddressCannotReceiveSourceBackedLabel() {
        assertEquals(
            "Directory information unavailable or incomplete",
            KampalaPublishedPlacePresentationPolicy
                .directoryNoticeLabel(
                    "${place.name}. Official source: ${place.sourceUrl}"
                )
        )
    }

    @Test
    fun missingOfficialSourceCannotReceiveSourceBackedLabel() {
        assertEquals(
            "Directory information unavailable or incomplete",
            KampalaPublishedPlacePresentationPolicy
                .directoryNoticeLabel(
                    "${place.name}, ${place.address}"
                )
        )
    }

    @Test
    fun unrelatedDirectoryNoticeIsUnchanged() {
        assertEquals(
            "Directory information unavailable or incomplete",
            KampalaPublishedPlacePresentationPolicy
                .directoryNoticeLabel(
                    "I couldn't verify a directory listing."
                )
        )
    }

    @Test
    fun otherBusinessCannotBorrowTheLabel() {
        assertEquals(
            "Directory information unavailable or incomplete",
            KampalaPublishedPlacePresentationPolicy
                .directoryNoticeLabel(
                    "Different Church at ${place.address}. " +
                        "Official source: ${place.sourceUrl}"
                )
        )
    }

    @Test
    fun routeCalculationIsNotImplied() {
        val message = sourceBackedMessage()

        assertFalse(message.contains("Open directions:"))
        assertTrue(message.contains("No route has been calculated"))
    }

    @Test
    fun dogBiteEmergencyRemainsAvailable() {
        val response =
            KampalaEmergencyResponsePolicy.respondOrNull(
                "I was bitten by a dog. What should I do?"
            )

        assertNotNull(response)
        assertTrue(response!!.contains("rabies", true))
    }
}