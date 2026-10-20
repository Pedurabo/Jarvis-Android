package com.example.jarvis.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KampalaLifeResponsePolicyTest {

    private fun notice(query: String): String? {
        val classification =
            KampalaLifeClassifier.classify(query)
                ?: return null

        return KampalaLifeResponsePolicy.notice(classification)
    }

    @Test
    fun marketPriceNoticeDoesNotInventPrices() {
        val result = requireNotNull(
            notice("How much is matooke in Nakawa Market?")
        )

        assertTrue(result.contains("cannot confirm current prices"))
        assertTrue(result.contains("market prices"))
        assertFalse(result.contains("30,000"))
        assertFalse(result.contains("45,000"))
    }

    @Test
    fun accommodationAndChurchesHaveHonestNotices() {
        val accommodation = requireNotNull(
            notice("Find an Airbnb in Kololo")
        )

        val church = requireNotNull(
            notice("Find a Catholic church in Kampala")
        )

        assertTrue(accommodation.contains("short-stay accommodation"))
        assertTrue(church.contains("churches and worship services"))
        assertTrue(accommodation.contains("not been connected"))
        assertTrue(church.contains("not been connected"))
    }

    @Test
    fun transactionRequestsAreNotHandledByFallback() {
        assertNull(notice("Book a SafeBoda"))
        assertNull(notice("Hire a cleaner"))
    }

    @Test
    fun protectedAndUnrelatedRequestsAreNotHandled() {
        assertNull(notice("Call Acacia Mall"))
        assertNull(notice("Open settings"))
        assertNull(notice("Tell me about Kampala history"))
        assertNull(notice("Find an Airbnb and a Catholic church"))
    }

    @Test
    fun noBookingOrPurchaseIsClaimed() {
        val result = requireNotNull(
            notice("Find catering for 50 guests")
        )

        assertTrue(result.contains("No booking or purchase has been made"))
    }
}
