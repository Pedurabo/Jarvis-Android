package com.example.jarvis.core.ai

/**
 * Deterministic initial guidance for an acute dog-bite situation.
 *
 * Does not diagnose, verify facility addresses, initiate navigation,
 * call emergency services, or perform any external action.
 */
internal object KampalaEmergencyResponsePolicy {

    private val dogBite = Regex(
        """(?i)\b(?:bitten\s+by\s+(?:a\s+)?dog|dog\s+(?:has\s+)?bit(?:ten)?|dog\s+bite|dog\s+attack)\b"""
    )

    private val recentInjury = Regex(
        """(?i)\b(?:just|now|today|recently|bleeding|injured|hurt|emergency|urgent|help|need|please|clinic|hospital|doctor|rabies)\b"""
    )

    private val educationalQuery = Regex(
        """(?i)^\s*(?:what\s+is|define|explain|describe|write\s+about|history\s+of)\b"""
    )

    fun respondOrNull(query: String): String? {
        val message = query.trim()

        if (educationalQuery.containsMatchIn(message)) return null
        if (!dogBite.containsMatchIn(message)) return null
        // CDI.6.0-A6.5-C:
        // A direct request for help after a dog bite is urgent
        // even without explicit words such as "emergency".
        val asksWhatToDo = Regex(
            """(?i)\bwhat\s+should\s+i\s+do\b"""
        ).containsMatchIn(message)

        if (!recentInjury.containsMatchIn(message) &&
            !asksWhatToDo
        ) return null

        return "A dog bite needs prompt medical attention. " +
            "If it is safe to do so, wash the wound thoroughly " +
            "with soap and running water for about 15 minutes. " +
            "If there is heavy bleeding, apply firm direct pressure " +
            "with a clean cloth and seek emergency help immediately. " +
            "Go to a healthcare facility urgently for assessment of " +
            "rabies post-exposure prevention, tetanus protection, " +
            "infection risk, and wound care. " +
            "Do not wait for symptoms. " +
            "I have not verified any clinic address or contacted " +
            "emergency services. If you need directions, please " +
            "confirm the clinic destination and your starting point " +
            "in a maps application."
    }
}