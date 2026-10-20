package com.example.jarvis.core.ai

/**
 * Honest fallback messages for recognized Kampala Life discovery
 * requests where no verified live provider is connected.
 *
 * This policy performs no searches, bookings, payments or phone actions.
 */
object KampalaLifeResponsePolicy {

    fun notice(
        classification: KampalaLifeClassifier.Classification
    ): String? {
        if (classification.intent ==
            KampalaLifeClassifier.Intent.TRANSACTION_REQUEST) {
            return null
        }

        val subject = when (classification.category) {
            KampalaLifeClassifier.Category.MARKET_COMMODITIES ->
                "market prices and commodity availability"
            KampalaLifeClassifier.Category.SHOPPING ->
                "shopping products and seller offers"
            KampalaLifeClassifier.Category.TRANSPORT ->
                "transport providers and fares"
            KampalaLifeClassifier.Category.FOOD ->
                "restaurants, menus and food ordering"
            KampalaLifeClassifier.Category.ACCOMMODATION ->
                "Airbnb and short-stay accommodation"
            KampalaLifeClassifier.Category.TOURISM_TRAVEL ->
                "tourism and travel services"
            KampalaLifeClassifier.Category.CATERING ->
                "catering providers and quotes"
            KampalaLifeClassifier.Category.HOME_SERVICES ->
                "cleaning, maid and housekeeping services"
            KampalaLifeClassifier.Category.CHURCHES ->
                "churches and worship services"
            KampalaLifeClassifier.Category.NGOS ->
                "NGOs and nonprofit organizations"
            KampalaLifeClassifier.Category.GOVERNMENT ->
                "government offices and public services"
            KampalaLifeClassifier.Category.EMBASSIES ->
                "embassies and consular services"
            KampalaLifeClassifier.Category.RESIDENTIAL_BUILDINGS ->
                "apartment blocks and residential buildings"
            KampalaLifeClassifier.Category.HOTELS ->
                "hotels and hospitality services"
        }

        val introduction = when (classification.intent) {
            KampalaLifeClassifier.Intent.PRICE_CHECK ->
                "I cannot confirm current prices for $subject."
            KampalaLifeClassifier.Intent.DISCOVER ->
                "I cannot provide verified live listings for $subject yet."
            KampalaLifeClassifier.Intent.TRANSACTION_REQUEST ->
                return null
        }

        return introduction + " " +
            "This Kampala Life service category is recognized, " +
            "but a verified provider data source has not been connected. " +
            "Please check an official provider or trusted local source " +
            "before relying on availability, prices, opening hours, " +
            "or contact information. No booking or purchase has been made."
    }
}
