package com.example.jarvis.core.ai

/**
 * CDI.5.57-F
 *
 * Interprets a short user reply as an origin ONLY when the
 * controller is already awaiting a starting location.
 *
 * Does not verify locations or retrieve device GPS coordinates.
 */
internal object KampalaOriginFollowUpResolver {

    private val whitespace = Regex("""\s+""")

    private val explicitOrigin = Regex(
        """^(?:(?:i'm|i\s+am|am)\s+(?:currently\s+)?(?:in|at)\s+|(?:i'm|i\s+am|am)\s+starting\s+from\s+|starting\s+from\s+|from\s+)(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val placeCharacters = Regex(
        """[A-Za-z][A-Za-z ,'\-]{1,79}"""
    )

    private val excludedWords = setOf(
        "what", "why", "when", "where", "who", "which",
        "how", "can", "could", "would", "should",
        "do", "does", "did", "please", "explain",
        "tell", "show", "find", "search", "help",
        "cancel", "stop", "thanks", "thank", "hello",
        "hi", "yes", "no", "okay", "ok", "take",
        "navigate", "directions", "need", "want"
    )

    fun resolve(input: String): String? {
        val normalized = input.trim()
            .replace(whitespace, " ")
            .trimEnd('.', '?', '!')
            .trim()

        if (normalized.isBlank()) return null

        val explicit = explicitOrigin.matchEntire(normalized)
        val candidate = (
            explicit?.groupValues?.get(1) ?: normalized
        ).trim().trimEnd(',').trim()

        if (
            candidate.length !in 2..80 ||
            !placeCharacters.matches(candidate)
        ) {
            return null
        }

        val words = candidate.split(whitespace)

        if (words.size !in 1..6) return null

        if (words.first().lowercase() in excludedWords) {
            return null
        }

        if (
            candidate.contains(
                Regex(
                    """\b(?:need|want|explain|navigate|directions|please|how|why|what|where)\b""",
                    RegexOption.IGNORE_CASE
                )
            )
        ) {
            return null
        }

        return candidate
    }
}