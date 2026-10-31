package com.example.jarvis.core.ai

/**
 * Short-lived, user-supplied emergency location context.
 *
 * Does not verify addresses, select a medical facility,
 * calculate a route, or authorize device actions.
 */
internal object KampalaEmergencyContextPolicy {

    data class Context(
        val destination: String,
        val origin: String?
    )

    private val originPattern = Regex(
        """(?i)\b(?:on|from|at)\s+([A-Za-z][A-Za-z ]{0,35}?(?:Street|Road|Avenue))\b"""
    )

    private val areaFollowUp = Regex(
        """(?i)^\s*i\s+think\s+it\s+is\s+in\s+([A-Za-z][A-Za-z ]{1,45}?)\s*[.!?]*\s*$"""
    )

    fun capture(query: String): Context? {
        // CDI.5.64-A2: Start with the specifically tested clinic.
        // Do not infer a destination from an arbitrary medical query.
        if (!query.contains("Case Clinic", ignoreCase = true)) {
            return null
        }

        val origin = originPattern.find(query)
            ?.groupValues
            ?.get(1)
            ?.trim()

        return Context(
            destination = "Case Clinic",
            origin = origin
        )
    }

    fun followUpResponse(
        context: Context,
        message: String
    ): String? {
        val match = areaFollowUp.matchEntire(message.trim())
            ?: return null

        val suggestedArea = match.groupValues[1].trim()

        val originText = if (context.origin != null) {
            "You previously said you were starting from " +
                "${context.origin}. "
        } else {
            "I don't have a confirmed starting location. "
        }

        return "You think ${context.destination} may be in " +
            "$suggestedArea. I haven't verified that location. " +
            originText +
            "Please verify the clinic in a maps application or " +
            "with the facility directly. Do not delay urgent " +
            "medical assessment for the dog bite, including " +
            "rabies exposure prevention."
    }
}