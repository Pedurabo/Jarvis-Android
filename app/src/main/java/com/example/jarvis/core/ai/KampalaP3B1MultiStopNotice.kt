package com.example.jarvis.core.ai

/**
 * P3-B1B: Conservative multi-stop journey presentation.
 *
 * This is a conversation notice, not geographic resolution.
 * The parser supplies names and order only.
 *
 * No provider records, entrances, routes, travel times,
 * verified addresses or map URLs are generated here.
 */
internal object KampalaP3B1MultiStopNotice {

    fun format(
        journey: KampalaJourneyIntentResolver.Journey
    ): String {
        require(journey.stops.isNotEmpty()) {
            "Multi-stop notice requires at least one stop."
        }

        return buildString {
            appendLine("Your requested journey")
            appendLine()
            appendLine("From: ${journey.origin}")

            journey.stops.forEachIndexed { index, stop ->
                appendLine("Stop ${index + 1}: $stop")
            }

            appendLine("To: ${journey.destination}")
            appendLine()

            appendLine(
                "I understand the order of your journey, " +
                    "but these location names have not yet " +
                    "been confirmed as exact map destinations."
            )

            appendLine(
                "The starting point, each intermediate stop, " +
                    "and the destination need geographic " +
                    "confirmation before directions are available."
            )

            appendLine(
                "I have not calculated a road route, " +
                    "travel time or distance."
            )

            append(
                "Please confirm the exact locations or " +
                    "campuses, including a nearby landmark " +
                    "or address where needed."
            )
        }
    }
}