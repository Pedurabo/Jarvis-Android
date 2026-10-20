package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KampalaLifeClassifierTest {

    private fun assertRoute(
        query: String,
        category: KampalaLifeClassifier.Category,
        intent: KampalaLifeClassifier.Intent
    ) {
        assertEquals(
            KampalaLifeClassifier.Classification(category, intent),
            KampalaLifeClassifier.classify(query)
        )
    }

    @Test
    fun recognizesMarketAndShopping() {
        assertRoute(
            "How much is matooke in Nakawa Market?",
            KampalaLifeClassifier.Category.MARKET_COMMODITIES,
            KampalaLifeClassifier.Intent.PRICE_CHECK
        )
        assertRoute(
            "Find a rice cooker on Jumia",
            KampalaLifeClassifier.Category.SHOPPING,
            KampalaLifeClassifier.Intent.DISCOVER
        )
    }

    @Test
    fun recognizesTransportAndFood() {
        assertRoute(
            "Book a SafeBoda",
            KampalaLifeClassifier.Category.TRANSPORT,
            KampalaLifeClassifier.Intent.TRANSACTION_REQUEST
        )
        assertRoute(
            "Find Cafe Javas in Kampala",
            KampalaLifeClassifier.Category.FOOD,
            KampalaLifeClassifier.Intent.DISCOVER
        )
    }

    @Test
    fun recognizesAccommodationTravelAndHotels() {
        assertRoute(
            "Find an Airbnb in Kololo",
            KampalaLifeClassifier.Category.ACCOMMODATION,
            KampalaLifeClassifier.Intent.DISCOVER
        )
        assertRoute(
            "Find a safari tour operator",
            KampalaLifeClassifier.Category.TOURISM_TRAVEL,
            KampalaLifeClassifier.Intent.DISCOVER
        )
        assertRoute(
            "Find hotels in Kampala",
            KampalaLifeClassifier.Category.HOTELS,
            KampalaLifeClassifier.Intent.DISCOVER
        )
    }

    @Test
    fun recognizesCateringAndHomeServices() {
        assertRoute(
            "Find catering for 50 guests",
            KampalaLifeClassifier.Category.CATERING,
            KampalaLifeClassifier.Intent.DISCOVER
        )
        assertRoute(
            "I need a cleaner twice a week",
            KampalaLifeClassifier.Category.HOME_SERVICES,
            KampalaLifeClassifier.Intent.DISCOVER
        )
    }

    @Test
    fun recognizesFaithAndInstitutions() {
        assertRoute(
            "Find a Catholic church in Kampala",
            KampalaLifeClassifier.Category.CHURCHES,
            KampalaLifeClassifier.Intent.DISCOVER
        )
        assertRoute(
            "Find a Pentecostal church in Ntinda",
            KampalaLifeClassifier.Category.CHURCHES,
            KampalaLifeClassifier.Intent.DISCOVER
        )
        assertRoute(
            "Find NGOs working in education",
            KampalaLifeClassifier.Category.NGOS,
            KampalaLifeClassifier.Intent.DISCOVER
        )
        assertRoute(
            "Where is the immigration office?",
            KampalaLifeClassifier.Category.GOVERNMENT,
            KampalaLifeClassifier.Intent.DISCOVER
        )
        assertRoute(
            "Find the Canadian embassy",
            KampalaLifeClassifier.Category.EMBASSIES,
            KampalaLifeClassifier.Intent.DISCOVER
        )
    }

    @Test
    fun recognizesResidentialBuildings() {
        assertRoute(
            "Find apartment blocks in Kololo",
            KampalaLifeClassifier.Category.RESIDENTIAL_BUILDINGS,
            KampalaLifeClassifier.Intent.DISCOVER
        )
    }

    @Test
    fun doesNotHijackUnrelatedOrProtectedRequests() {
        listOf(
            "Tell me about Kampala history",
            "What is a mosque?",
            "Call Acacia Mall",
            "Open settings",
            "Find an Airbnb and a Catholic church",
            "Find places",
            ""
        ).forEach {
            assertNull(it, KampalaLifeClassifier.classify(it))
        }
    }
}
