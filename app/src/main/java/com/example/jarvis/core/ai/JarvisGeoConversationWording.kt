package com.example.jarvis.core.ai

import java.util.Locale

/**
 * Narrow adaptation for single geographic search requests.
 *
 * Reuses the existing KampalaExplicitGeoDiscovery handler.
 * Does not invent map coverage or inspect conversation history.
 * The original user query remains unchanged outside routing.
 */
internal object JarvisGeoConversationWording {

    private val categories = setOf(
        "hospital", "hospitals",
        "clinic", "clinics",
        "pharmacy", "pharmacies",
        "school", "schools",
        "university", "universities",
        "bank", "banks",
        "fuel station", "fuel stations",
        "hotel", "hotels",
        "guest house", "guest houses",
        "guesthouse", "guesthouses",
        "police station", "police stations",
        "embassy", "embassies"
    )

    private val combinedRequest = Regex(
        """\b(?:and|nd)\s+(?:banks?|hospitals?|clinics?|pharmacies|schools?|universities|hotels?|embassies|police stations?|fuel stations?|guesthouses?|guest houses?)\s+in\b"""
    )

    private val singleRequest = Regex(
        """^find (.+?) in (.+)$"""
    )

    fun normalize(message: String): String {
        val text = message
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s+"""), " ")

        if (text.isBlank()) return message

        // Combined searches need their own routing support.
        // Never silently discard one requested category.
        if (combinedRequest.containsMatchIn(text)) {
            return message
        }

        val candidate = when {
            text.startsWith("can you find ") ->
                "find " + text.removePrefix("can you find ")

            text.startsWith("could you find ") ->
                "find " + text.removePrefix("could you find ")

            text.startsWith("please find ") ->
                "find " + text.removePrefix("please find ")

            text.startsWith("find me ") ->
                "find " + text.removePrefix("find me ")

            text.startsWith("how about ") -> {
                val remainder = text
                    .removePrefix("how about ")
                    .replace(
                        Regex("""\?\s*can you find those\??\s*$"""),
                        ""
                    )
                    .trimEnd('?', '.', '!', ' ')

                "find $remainder"
            }

            else -> return message
        }

        val cleaned = candidate
            .trimEnd('?', '.', '!', ' ')

        val match = singleRequest.matchEntire(cleaned)
            ?: return message

        val category = match.groupValues[1].trim()
        val area = match.groupValues[2].trim()

        if (category !in categories || area.isBlank()) {
            return message
        }

        // Do not require the area to be approved here.
        // The existing geographic handler must decide whether
        // to search or display its unsupported-area notice.
        return candidate
    }
}