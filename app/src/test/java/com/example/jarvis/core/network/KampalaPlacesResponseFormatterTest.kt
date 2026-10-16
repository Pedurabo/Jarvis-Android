package com.example.jarvis.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaPlacesResponseFormatterTest {

    private fun place(
        name: String = "Acacia Mall",
        area: String = "Kisementi",
        category: String = "shopping",
        address: String = "14-18 Cooper Road, Kampala",
        sourceUrl: String =
            "https://www.theacaciamall.com/contact-acacia-mall"
    ) = KampalaPlace(
        id = "test-place",
        name = name,
        category = category,
        area = area,
        address = address,
        sourceUrl = sourceUrl,
        verifiedAt = "2026-10-15"
    )

    @Test
    fun formatsSinglePlace() {
        val formatted = KampalaPlacesResponseFormatter.format(
            KampalaPlacesRepository.SearchResult.Success(
                listOf(place())
            )
        )

        assertTrue(formatted.contains("Acacia Mall"))
        assertTrue(formatted.contains("Kisementi - Shopping"))
        assertTrue(formatted.contains("14-18 Cooper Road"))
        assertTrue(formatted.contains("Acacia Mall official website"))
        assertTrue(formatted.contains("15 October 2026"))
        assertTrue(formatted.contains("Source link: https://"))
        assertTrue(formatted.contains("Opening hours"))
        assertFalse(formatted.contains("1. Acacia Mall"))
    }

    @Test
    fun formatsMultiplePlaces() {
        val formatted = KampalaPlacesResponseFormatter.format(
            KampalaPlacesRepository.SearchResult.Success(
                listOf(
                    place(),
                    place(
                        name = "Gaddafi National Mosque",
                        area = "Old Kampala",
                        category = "tourism",
                        address = "Old Kampala Road, Kampala",
                        sourceUrl =
                            "https://visitkampala.kcca.go.ug/business_details/old-kampala-national-mosque"
                    )
                )
            )
        )

        assertTrue(formatted.contains("1. Acacia Mall"))
        assertTrue(formatted.contains("2. Gaddafi National Mosque"))
        assertTrue(formatted.contains("Kampala tourism directory"))
    }

    @Test
    fun formatsEmptyResults() {
        val formatted = KampalaPlacesResponseFormatter.format(
            KampalaPlacesRepository.SearchResult.Success(emptyList())
        )

        assertTrue(formatted.contains("No matching places"))
    }

    @Test
    fun preservesFailureMessage() {
        val message = "Unable to reach the Kampala places service."

        assertEquals(
            message,
            KampalaPlacesResponseFormatter.format(
                KampalaPlacesRepository.SearchResult.Failure(message)
            )
        )
    }
}