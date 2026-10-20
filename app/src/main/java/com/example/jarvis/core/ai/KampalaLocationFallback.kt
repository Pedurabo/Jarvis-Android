package com.example.jarvis.core.ai

/**
 * M1F-I3C.120K.2
 *
 * Safe fallback for explicit Kampala place-discovery requests
 * whose neighbourhood is not recognized by the existing router.
 *
 * This policy does not query the network, invent places, or
 * silently replace the requested area with another location.
 */
object KampalaLocationFallback {

    private val discoveryPattern = Regex(
        """^(?:find|show me|search for|list|where can i find)\s+(?:a\s+|an\s+|the\s+|some\s+)?(?:shopping places|shopping centres?|shopping centers?|shops?|malls?|restaurants?|cafes?|hotels?|guesthouses?|hospitals?|clinics?|markets?|supermarkets?|museums?|parks?|embass(?:y|ies)|government offices?|church(?:es)?|mosques?|places)\s+in\s+([a-z][a-z\s-]{1,60})\??$""",
        RegexOption.IGNORE_CASE
    )

    private val supportedAreas = setOf(
        "kampala",
        "kisementi",
        "old kampala",
        "kololo",
        "nakasero",
        "ntinda",
        "bugolobi",
        "makindye"
    )

    private val spellingSuggestions = mapOf(
        "ntingd" to "Ntinda",
        "ntingda" to "Ntinda",
        "ntind" to "Ntinda",
        "ntindaa" to "Ntinda",
        "kololoo" to "Kololo",
        "kolollo" to "Kololo",
        "kolol" to "Kololo",
        "bugolobii" to "Bugolobi",
        "bugolob" to "Bugolobi",
        "nakaseroo" to "Nakasero",
        "nakesero" to "Nakasero",
        "makindyee" to "Makindye"
    )

    fun notice(input: String): String? {
        val match = discoveryPattern.matchEntire(input.trim())
            ?: return null

        val requestedArea = match.groupValues[1]
            .trim()
            .trimEnd('?')
            .trim()

        val normalized = requestedArea
            .lowercase()
            .replace(Regex("""\s+"""), " ")

        // Valid locations belong to the normal directory router.
        if (normalized in supportedAreas) {
            return null
        }

        val suggestion = spellingSuggestions[normalized]

        return if (suggestion != null) {
            "I couldn't confidently identify \"$requestedArea\" " +
                "as a Kampala neighbourhood. Did you mean $suggestion? " +
                "Please try your search again using $suggestion. " +
                "I haven't searched a different location automatically."
        } else {
            "I couldn't identify \"$requestedArea\" as a " +
                "currently supported Kampala search area. " +
                "Please check the spelling or try Ntinda, Kololo, " +
                "Nakasero, Bugolobi, Makindye, Kisementi or Old Kampala. " +
                "No directory results have been assumed."
        }
    }
}
