package com.example.jarvis.core.ai

import java.util.Locale

/**
 * Repairs a narrow boundary in existing geographic routing.
 *
 * The existing wording policy and registered-area resolver
 * remain responsible for their normal commands.
 *
 * This helper never chooses geographic bounds or calls a provider.
 */
internal object JarvisGeoRoutingRepair {

    private const val categoryPattern =
        "(?:hospitals|clinics|pharmacies|schools|universities|" +
            "banks|fuel stations|hotels|guesthouses|guest houses|" +
            "police stations|embassies)"

    private val depositRelatedSearch = Regex(
        "(?i)^" +
            "(?:trying to deposit\\b.{0,180}|" +
            "i(?:'m| am) trying to deposit\\b.{0,180}|" +
            "the atms? (?:are|aren't|isn't|is not)\\b.{0,180})" +
            "\\b(?:can you\\s+)?find(?: me)?\\s+" +
            "($categoryPattern)\\s+in\\s+" +
            "([a-z][a-z -]{0,55}?)\\s*[?.!]*$"
    )

    private val conversationalFollowUp = Regex(
        "(?i)^how about\\s+" +
            "($categoryPattern)\\s+in\\s+" +
            "([a-z][a-z -]{0,55}?)" +
            "\\s*[,?.!]\\s*can you find those\\s*[?.!]*$"
    )

    fun normalize(message: String): String {
        val normalized = message
            .trim()
            .replace(Regex("""\s+"""), " ")

        val match = depositRelatedSearch.matchEntire(normalized)
            ?: conversationalFollowUp.matchEntire(normalized)
            ?: return message

        val category = match.groupValues[1]
            .lowercase(Locale.ROOT)
            .trim()

        val area = match.groupValues[2]
            .lowercase(Locale.ROOT)
            .trim()

        // A conjunction could denote multiple independent searches.
        // Never silently discard one of them.
        if (
            Regex("""(?i)\b(?:and|nd|plus)\b""")
                .containsMatchIn(area)
        ) {
            return message
        }

        return "find $category in $area"
    }
}