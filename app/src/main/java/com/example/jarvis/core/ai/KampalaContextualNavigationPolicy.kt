package com.example.jarvis.core.ai

/**
 * Short-lived reference to a single directory destination.
 *
 * Does not verify an address, compute a route, or authorize
 * an external navigation action.
 */
internal object KampalaContextualNavigationPolicy {

    private val followUp = Regex(
        """^(?:how\s+(?:do|can)\s+i\s+get\s+there|take\s+me\s+there|directions\s+to\s+that\s+place)\s*[?.!]*$""",
        RegexOption.IGNORE_CASE
    )

    // CDI.6.0-A6.5-C:
    // Recognize an explicit first-person starting-point correction.
    // Do not interpret it as a destination correction.
    private val originCorrection = Regex(
        """(?i)^(?:actually,\s*)?(?:i(?:'m| am)|am)\s+starting\s+from\s+([\p{L}][\p{L}\s'-]{1,60})\s*[.!?]?$"""
    )

    fun correctedOriginOrNull(message: String): String? {
        val match = originCorrection.matchEntire(message.trim())
            ?: return null

        val origin = match.groupValues[1]
            .trim()
            .trimEnd('.', '!', '?')
            .trim()

        if (origin.length < 2 || origin.length > 60) return null

        return origin
    }

    // CDI.6.0-A6.6-A:
    // This extracts a user-supplied branch or area clarification.
    // It is never a verified place, address, or route.
    private val branchClarification = Regex(
        """(?i)^(?:(?:the|that)\s+)?([\p{L}][\p{L}\s'-]{1,50}\s+branch|(?:branch\s+(?:in|at)\s+)[\p{L}][\p{L}\s'-]{1,50}|(?:in|at)\s+[\p{L}][\p{L}\s'-]{1,50})\s*[.!?]?$"""
    )

    fun branchClarificationOrNull(message: String): String? {
        val match = branchClarification.matchEntire(message.trim())
            ?: return null

        val detail = match.groupValues[1].trim()
            .trimEnd('.', '!', '?')
            .trim()

        if (detail.length !in 3..60) return null

        return detail
    }

    // Only prepare another user-supplied directory search.
    // No inference or verification occurs here.
    fun clarifiedDestinationQueryOrNull(
        destination: String?,
        clarification: String
    ): String? {
        val base = destination?.trim() ?: return null
        if (base.isEmpty() || base.length > 120) return null

        val detail = branchClarificationOrNull(clarification)
            ?: return null

        return KampalaSourceBackedPlacePolicy.normalizeRequestedName(
            "$base $detail"
        )
    }

    fun isFollowUp(message: String): Boolean =
        followUp.matches(message.trim()) ||
            correctedOriginOrNull(message) != null

    fun uniqueDestination(
        entries: List<Pair<String, String>>
    ): String? {
        if (entries.size != 1) return null

        val (name, address) = entries.single()
        val cleanName = name.trim()
        val cleanAddress = address.trim()

        if (cleanName.isBlank() || cleanAddress.isBlank()) {
            return null
        }

        return "$cleanName, $cleanAddress"
    }
}