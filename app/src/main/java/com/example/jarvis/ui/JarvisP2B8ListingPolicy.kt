package com.example.jarvis.ui

/**
 * P2-B8A: Presentation-only interpretation of map records.
 *
 * This policy never asserts business operating status,
 * licensing, ownership, opening hours, or independent verification.
 *
 * Provider names are deliberately not rewritten.
 */
internal object JarvisP2B8ListingPolicy {

    const val verificationNotice =
        "Verification: these records identify mapped locations, " +
            "not confirmed operating services. Check the " +
            "relevant institution or official provider before visiting."

    fun cardSubtitle(category: String): String {
        val safeCategory = category
            .trim()
            .replace(
                Regex("""[\p{Cc}\p{Cf}]"""),
                " "
            )
            .replace(Regex("""\s+"""), " ")
            .take(80)
            .ifBlank { "place" }

        return "${safeCategory.replaceFirstChar { it.uppercase() }} · Unverified"
    }
}