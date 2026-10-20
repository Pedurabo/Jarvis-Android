package com.example.jarvis.core.ai

/**
 * Classifies Kampala Life requests without executing actions,
 * fetching data, or claiming provider availability.
 */
object KampalaLifeClassifier {

    enum class Category {
        MARKET_COMMODITIES,
        SHOPPING,
        TRANSPORT,
        FOOD,
        ACCOMMODATION,
        TOURISM_TRAVEL,
        CATERING,
        HOME_SERVICES,
        CHURCHES,
        NGOS,
        GOVERNMENT,
        EMBASSIES,
        RESIDENTIAL_BUILDINGS,
        HOTELS
    }

    enum class Intent {
        DISCOVER,
        PRICE_CHECK,
        TRANSACTION_REQUEST
    }

    data class Classification(
        val category: Category,
        val intent: Intent
    )

    private val categories = listOf(
        Category.MARKET_COMMODITIES to
            """\b(matooke|commodity|market prices?|nakasero market|nakawa market|kalerwe market|owino market)\b""",
        Category.SHOPPING to
            """\b(jumia|ubuy|rice cooker|online shopping)\b""",
        Category.TRANSPORT to
            """\b(safeboda|safe boda|faras|airport transfer|boda boda)\b""",
        Category.FOOD to
            """\b(cafe javas|café javas|kfc|restaurants?|food delivery)\b""",
        Category.ACCOMMODATION to
            """\b(airbnb|guesthouses?|serviced apartments?|short.stay accommodation)\b""",
        Category.TOURISM_TRAVEL to
            """\b(safari|tourist attractions?|tour operators?|tour guides?|travel agencies|guided tours?)\b""",
        Category.CATERING to
            """\b(catering|caterers?|event caterers?)\b""",
        Category.HOME_SERVICES to
            """\b(maids?|cleaners?|housekeeping|cleaning services?|laundry services?)\b""",
        Category.CHURCHES to
            """\b(churches?|catholic|anglican|protestant|pentecostal|born.again)\b""",
        Category.NGOS to
            """\b(ngos?|nonprofits?|non.profit organizations?|charities)\b""",
        Category.GOVERNMENT to
            """\b(government offices?|immigration offices?|ministries|ministry|kcca offices?|tax offices?)\b""",
        Category.EMBASSIES to
            """\b(embassies|embassy|consulates?|diplomatic missions?)\b""",
        Category.RESIDENTIAL_BUILDINGS to
            """\b(apartment blocks?|residential buildings?|apartment buildings?)\b""",
        Category.HOTELS to
            """\b(hotels?|hostels?)\b"""
    ).map { (category, pattern) ->
        category to Regex(pattern, RegexOption.IGNORE_CASE)
    }

    private val requestSignal = Regex(
        """\b(find|show|where|recommend|looking for|need|search|price|cost|how much|compare|book|order|buy|purchase|hire|reserve|arrange|get me)\b""",
        RegexOption.IGNORE_CASE
    )

    private val protectedCommand = Regex(
        """^\s*(call|dial|open|launch|send|text|message|set|turn on|turn off)\b""",
        RegexOption.IGNORE_CASE
    )

    private val transactionSignal = Regex(
        """\b(book|order|buy|purchase|hire|reserve|arrange|get me)\b""",
        RegexOption.IGNORE_CASE
    )

    private val priceSignal = Regex(
        """\b(price|prices|cost|fare|how much)\b""",
        RegexOption.IGNORE_CASE
    )

    fun classify(input: String): Classification? {
        val query = input.trim()
        if (query.isBlank()) return null
        if (protectedCommand.containsMatchIn(query)) return null
        if (!requestSignal.containsMatchIn(query)) return null

        val matches = categories.filter { (_, pattern) ->
            pattern.containsMatchIn(query)
        }

        // Multi-category requests need orchestration, not arbitrary selection.
        if (matches.size != 1) return null

        val intent = when {
            transactionSignal.containsMatchIn(query) ->
                Intent.TRANSACTION_REQUEST
            priceSignal.containsMatchIn(query) ->
                Intent.PRICE_CHECK
            else -> Intent.DISCOVER
        }

        return Classification(matches.single().first, intent)
    }
}
