package com.example.jarvis.ui

import org.junit.Assert.*
import org.junit.Test

class JarvisP2B8ListingPolicyTest {

    @Test
    fun banksHaveExplicitUnverifiedStatus() {
        assertEquals(
            "Bank · Unverified",
            JarvisP2B8ListingPolicy.cardSubtitle("bank")
        )
    }

    @Test
    fun hospitalsHaveExplicitUnverifiedStatus() {
        assertEquals(
            "Hospital · Unverified",
            JarvisP2B8ListingPolicy.cardSubtitle("hospital")
        )
    }

    @Test
    fun allCategoriesUseTheSameVerificationRule() {
        for (category in listOf(
            "school", "hotel", "embassy", "pharmacy",
            "clinic", "university", "fuel_station"
        )) {
            val subtitle =
                JarvisP2B8ListingPolicy.cardSubtitle(category)

            assertTrue(subtitle.isNotBlank())
            assertTrue(subtitle.endsWith(" · Unverified"))
        }
    }

    @Test
    fun unsafeCategoryControlCharactersAreSanitized() {
        val subtitle =
            JarvisP2B8ListingPolicy.cardSubtitle(
                "bank\nservice"
            )

        assertFalse(subtitle.contains('\n'))
        assertTrue(subtitle.contains("Unverified"))
    }

    @Test
    fun emptyCategoriesHaveNeutralFallback() {
        assertEquals(
            "Place · Unverified",
            JarvisP2B8ListingPolicy.cardSubtitle("  ")
        )
    }

    @Test
    fun noticeDoesNotClaimIndependentVerification() {
        val notice =
            JarvisP2B8ListingPolicy.verificationNotice

        assertTrue(notice.contains("not confirmed operating services"))
        assertTrue(notice.contains("official provider"))
        assertFalse(notice.contains("Verified business"))
    }
}