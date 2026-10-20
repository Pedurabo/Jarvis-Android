package com.example.jarvis.core.ai

/**
 * Source-availability boundary for Kampala factual questions observed
 * failing during Samsung testing.
 *
 * This component does not supply geographic facts, approximate rankings,
 * verified addresses, historical details or live provider data.
 *
 * It prevents known unsupported factual intents from falling through
 * to the local generative model.
 *
 * This is not a general fact checker.
 */
internal object KampalaFactualReliabilityBoundary {

    private val protectedAction = Regex(
        """^(?:open|launch|call|dial|send|text|message|navigate|set|turn\s+on|turn\s+off|book|reserve|order|buy|purchase|hire)\b""",
        RegexOption.IGNORE_CASE
    )

    private val nearestPlace = Regex(
        """^(?:(?:what|which)\s+is\s+the\s+|where\s+is\s+the\s+)?(?:nearest|closest)\s+(?:market|supermarket|hospital|clinic|restaurant|hotel|cinema|football\s+ground|shopping\s+mall)(?:\s+to\s+me)?$"""
    )

    private val numberedKampalaHills = Regex(
        """\b(?:[0-9]{1,2}|one|two|three|four|five|six|seven|eight|nine|ten)\s+(?:historic\s+|traditional\s+)?hills?\s+(?:of|in|around)\s+kampala\b"""
    )
    private val sevenHills = Regex(
        """\b(?:seven|7)\s+hills?\s+of\s+kampala\b"""
    )

    private val hillsRequest = Regex(
        """^(?:list|name|describe|what\s+are|tell\s+me\s+about)\s+(?:the\s+)?(?:main\s+|different\s+)?hills?\s+(?:of|in)\s+kampala$"""
    )

    private val lakesRequest = Regex(
        """^(?:list|name|describe|what\s+are|tell\s+me\s+about)\s+(?:the\s+)?(?:main\s+|different\s+)?lakes?\s+(?:of|in|around)\s+kampala$"""
    )

    private val administrativeRequest = Regex(
        """^(?:(?:how\s+many|what|which)\s+(?:administrative\s+)?(?:divisions?|subcounties|sub-counties)|(?:list|name)\s+(?:the\s+)?(?:administrative\s+)?(?:divisions?|subcounties|sub-counties))(?:\s+(?:are|make\s+up))?(?:\s+(?:there\s+are|in|of|that\s+make\s+up))?\s+kampala$"""
    )

    private val kasubiRequest = Regex(
        """\b(?:kasubi|kasumi)\s+tombs?\b"""
    )

    private val kasubiQuestion = Regex(
        """^(?:how\s+about|tell\s+me\s+about|what\s+(?:are|is)|describe|where\s+(?:are|is)|give\s+me\s+information\s+about)\b"""
    )

    private val generalKampala = Regex(
        """^(?:what\s+can\s+you\s+tell\s+me\s+about|tell\s+me\s+about|give\s+me\s+(?:facts|information)\s+about)\s+kampala$"""
    )

    private val cuisineComparison = Regex(
        """^(?:which|what)\s+restaurants?\s+(?:has|have|serves?|offers?)\s+(?:the\s+)?(?:best|top|most\s+authentic)\s+.+\s+(?:food|cuisine|dishes?)\s+in\s+kampala$"""
    )

    private val namedRetailLocation = Regex(
        """^where\s+is\s+(?:the\s+)?(?:nakumat|nakumatt)(?:\s+(?:located|in\s+kampala))?$"""
    )

    private val palaceLocation = Regex(
        """^where\s+is\s+(?:the\s+)?(?:palace|kabaka'?s\s+palace|lubiri)(?:\s+located)?(?:\s*,?\s*(?:which|what)\s+division)?$"""
    )

    private val compoundMakindye = Regex(
        """\blandmarks?\s+in\s+makindye\b"""
    )

    private val turkishMeal = Regex(
        """\b(?:turkish\s+(?:meal|food|cuisine)|turkish\s+restaurants?)\b"""
    )

    fun notice(input: String): String? {
        val normalized = input.trim()
            .lowercase()
            .replace(Regex("""\s+"""), " ")
            .trimEnd('.', ',', '!', '?')
            .trim()

        if (normalized.isBlank()) return null
        if (protectedAction.containsMatchIn(normalized)) return null

        if (nearestPlace.matches(normalized)) {
            return "To identify the nearest place, I need a starting " +
                "location and reliable distance information. " +
                "Please tell me your Kampala neighbourhood or a nearby " +
                "landmark. I won't guess which place is closest."
        }

        if (compoundMakindye.containsMatchIn(normalized) &&
            turkishMeal.containsMatchIn(normalized)) {
            return "You're asking for both Makindye landmarks and " +
                "Turkish food options. I can't currently verify both " +
                "from my Kampala directory, so I shouldn't invent a " +
                "list. Please check trusted local listings while " +
                "verified coverage is expanded."
        }

        if (numberedKampalaHills.containsMatchIn(normalized) || sevenHills.containsMatchIn(normalized) ||
            hillsRequest.matches(normalized)) {
            return "I don't currently have a sufficiently verified " +
                "Kampala hills reference to provide an accurate " +
                "historical list and descriptions. I won't invent " +
                "hill names or geographic details."
        }

        if (lakesRequest.matches(normalized)) {
            return "I don't currently have a verified dataset of " +
                "lakes and water bodies specifically within Kampala. " +
                "I won't invent names or imply nearby lakes are " +
                "within the city. An authoritative geographic " +
                "source is needed."
        }

        if (administrativeRequest.matches(normalized)) {
            return "I can't currently verify the requested Kampala " +
                "administrative count and boundaries from my " +
                "published Knowledge service. Please use the " +
                "latest official KCCA administrative information."
        }

        if (kasubiRequest.containsMatchIn(normalized) &&
            kasubiQuestion.containsMatchIn(normalized)) {
            return "I don't yet have a verified Knowledge entry " +
                "for the Kasubi Tombs. I shouldn't invent historical, " +
                "cultural or geographic details. Please consult " +
                "authoritative heritage information while the " +
                "Knowledge directory is expanded."
        }

        if (generalKampala.matches(normalized)) {
            return "Kampala is Uganda's capital city. My detailed " +
                "Kampala Knowledge coverage is still limited, so " +
                "I can't responsibly provide unverified population " +
                "figures or geographic rankings. You can ask about " +
                "Kampala's history or search my verified places directory."
        }

        if (cuisineComparison.matches(normalized)) {
            return "I don't currently have verified cuisine-specific " +
                "restaurant comparisons for Kampala. My directory " +
                "can list supported restaurants, but it cannot " +
                "establish which serves the best cuisine without " +
                "reliable evidence."
        }

        if (namedRetailLocation.matches(normalized)) {
            return "I can't verify the current location or operating " +
                "status of Nakumatt from my Kampala directory. " +
                "I won't invent an address or imply it is operating."
        }

        if (palaceLocation.matches(normalized)) {
            return "I can't currently verify the requested palace " +
                "location and administrative division from my " +
                "published Kampala reference data. Please clarify " +
                "which palace you mean and consult an official " +
                "heritage or local authority source."
        }

        return null
    }
}
