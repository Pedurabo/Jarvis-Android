package com.example.jarvis.core.ai

import java.util.Locale

/**
 * P2 conversation stabilization.
 *
 * This does not resolve locations, invent travel times,
 * execute tools, or replace existing geographic discovery.
 */
internal object JarvisConversationStabilityPolicy {

    const val ROUTE_SAFETY_RESPONSE =
        "I haven't verified a route or travel time for this journey. " +
            "Please confirm your starting point and destinations " +
            "before travelling."

    const val OUTPUT_SAFETY_RESPONSE =
        "I couldn't produce a reliable answer to that request. " +
            "Please try rephrasing it."

    private const val categories =
        "(?:hospitals|clinics|pharmacies|schools|universities|" +
            "banks|fuel stations|hotels|guesthouses|guest houses|" +
            "police stations|embassies)"

    private val followUp = Regex(
        "(?i)^how about\\s+(" + categories + ")" +
            "\\s+in\\s+([a-z][a-z -]{0,55}?)" +
            "\\s*[,?.!]+\\s*can you find those\\s*[?.!]*$"
    )

    private val depositSearch = Regex(
        "(?i)^.{0,240}\\b(?:can you\\s+)?" +
            "find(?: me)?\\s+(" + categories + ")" +
            "\\s+in\\s+([a-z][a-z -]{0,55}?)\\s*[?.!]*$"
    )

    private val moneyContext = Regex(
        """(?i)\b(?:deposit|atms?|arms|cash machine|withdraw)\b"""
    )

    private val journeyIntent = Regex(
        """(?i)\b(?:journey|route|directions|navigate|travel|drive|driving|drop\s+(?:off|my)|head\s+to|get\s+me\s+to)\b"""
    )

    private val numericTravelTime = Regex(
        """(?i)\b\d{1,3}\s*(?:minutes?|mins?|hours?|hrs?)\b"""
    )

    private val unsolicitedCode = Regex(
        """(?i)\b(?:this code snippet|implementation of ai-powered navigation|suggestions for improvement|json object structure|optimization for performance)\b"""
    )

    private val technicalRequest = Regex(
        """(?i)\b(?:code|snippet|kotlin|json|programming|debug|review the implementation|source file)\b"""
    )

    fun normalizeGeographicRequest(input: String): String {
        val normalized = input.trim()
            .replace(Regex("""\s+"""), " ")

        // Explicitly establish a non-null MatchResult.
        // The previous implementation failed to do this.
        val followUpMatch = followUp.matchEntire(normalized)

        val match = if (followUpMatch != null) {
            followUpMatch
        } else if (moneyContext.containsMatchIn(normalized)) {
            depositSearch.matchEntire(normalized) ?: return input
        } else {
            return input
        }

        val category = match.groupValues[1]
            .lowercase(Locale.ROOT)
            .trim()

        val area = match.groupValues[2]
            .lowercase(Locale.ROOT)
            .trim()

        if (area.isBlank()) return input

        // Never discard a second geographic request.
        if (
            Regex("""(?i)\b(?:and|nd|plus)\b""")
                .containsMatchIn(area)
        ) {
            return input
        }

        return "find $category in $area"
    }

    /**
     * Applied only after existing deterministic journey handlers.
     *
     * This is not a route planner, GPS reader, or place resolver.
     * It prevents an unresolved "proceed" request from being
     * answered as a fabricated navigation instruction.
     */
    internal fun unresolvedJourneyFollowUpNotice(
        query: String
    ): String? {
        val normalized = query
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s+"""), " ")

        val proceed = Regex(
            """\b(?:let'?s|let us|please|can we|we can)?\s*""" +
                """(?:proceed|continue|start|resume)\s+""" +
                """(?:with\s+)?(?:the|our|my)\s+""" +
                """(?:journey|trip|route|navigation)\b"""
        )

        val hasJourneyReference = proceed.containsMatchIn(normalized)

        if (!hasJourneyReference) return null

        return "I understand you want to continue the journey. " +
            "I haven't verified the complete route or its travel time. " +
            "Please confirm the starting location and each destination " +
            "using a navigation service before travelling. " +
            "I won't invent directions or journey times."
    }
    fun reviewModelAnswer(
        query: String,
        answer: String,
        toolUsageCount: Int
    ): String {
        val technical = technicalRequest.containsMatchIn(query)

        if (
            !technical &&
            unsolicitedCode.containsMatchIn(answer)
        ) {
            return OUTPUT_SAFETY_RESPONSE
        }

        // Tool usage does not by itself prove a route was calculated.
        // However, preserve the existing tool-backed response path.
        // The final route-verification boundary must eventually use
        // an explicit verified route result instead of a count.
        if (
            toolUsageCount == 0 &&
            journeyIntent.containsMatchIn(query) &&
            numericTravelTime.containsMatchIn(answer)
        ) {
            return ROUTE_SAFETY_RESPONSE
        }

        return answer
    }
}