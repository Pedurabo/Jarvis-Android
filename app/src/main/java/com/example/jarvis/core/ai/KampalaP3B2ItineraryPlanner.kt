package com.example.jarvis.core.ai

/**
 * P3-B2A: Limited structured itinerary extraction.
 * No geocoding, route creation, restaurant selection or booking.
 */
internal object KampalaP3B2ItineraryPlanner {

    enum class Activity {
        PICK_UP_PERSON,
        DROP_OFF_PERSON,
        PICK_UP_FOOD,
        FINAL_DESTINATION
    }

    data class Stop(
        val activity: Activity,
        val requestedPlace: String?,
        val purpose: String,
        val needsSelection: Boolean = false
    )

    data class Plan(
        val origin: String?,
        val stops: List<Stop>
    )

    private val destinationPattern = Regex(
        """\b(?:need|want|have)\s+to\s+get\s+to\s+([^.!?]{2,100}?)\s+for\s+(?:a|an|the)\s+([^.!?,]{2,100})(?:,|\.| but\b)""",
        RegexOption.IGNORE_CASE
    )

    private val pickupPattern = Regex(
        """\bhead\s+to\s+([^.!?]{2,100}?)\s+to\s+pick\s+up\s+([^.!?,]{2,100})(?:[.!?]|,\s*then\b)""",
        RegexOption.IGNORE_CASE
    )

    private val dropoffPattern = Regex(
        """\bdrop\s+(?:him|her|them|my\s+\w+)\s+at\s+([^.!?]{2,100}?)\s+before\b""",
        RegexOption.IGNORE_CASE
    )

    private val foodPattern = Regex(
        """\bpick\s+up\s+([^.!?]{2,80}?\bfood)\s+from\s+(?:a|an|any)\s+(?:\w+\s+)?restaurant\b""",
        RegexOption.IGNORE_CASE
    )

    private fun clean(value: String): String =
        value.trim()
            .trimEnd('.', ',', '!', '?')
            .replace(Regex("""\s+"""), " ")

    fun extract(message: String): Plan? {
        val text = message.trim()
        if (text.length !in 60..1000) return null

        val destination = destinationPattern.find(text) ?: return null
        val pickup = pickupPattern.find(text) ?: return null
        val dropoff = dropoffPattern.find(text) ?: return null
        val food = foodPattern.find(text) ?: return null

        if (
            pickup.range.first >= dropoff.range.first ||
            dropoff.range.first >= food.range.first
        ) return null

        val school = clean(pickup.groupValues[1])
        val person = clean(pickup.groupValues[2])
        val mall = clean(dropoff.groupValues[1])
        val foodType = clean(food.groupValues[1])
        val destinationName = clean(destination.groupValues[1])
        val purpose = clean(destination.groupValues[2])

        if (
            listOf(
                school, person, mall, foodType,
                destinationName, purpose
            ).any { it.length !in 2..120 }
        ) return null

        return Plan(
            origin = null,
            stops = listOf(
                Stop(
                    Activity.PICK_UP_PERSON,
                    school,
                    "Pick up $person"
                ),
                Stop(
                    Activity.DROP_OFF_PERSON,
                    mall,
                    "Drop off passenger"
                ),
                Stop(
                    Activity.PICK_UP_FOOD,
                    null,
                    "Pick up $foodType",
                    needsSelection = true
                ),
                Stop(
                    Activity.FINAL_DESTINATION,
                    destinationName,
                    "Attend $purpose"
                )
            )
        )
    }

    fun describe(plan: Plan): String = buildString {
        // Retain the earlier P3-B1F response contract.
        appendLine("I understand this as a multi-stop journey.")
        appendLine()
        appendLine("Your planned journey")
        appendLine()
        appendLine("Starting point: ${plan.origin ?: "Not provided"}")
        appendLine()

        plan.stops.forEachIndexed { index, stop ->
            appendLine(
                "${index + 1}. " +
                    (stop.requestedPlace ?: "Restaurant not yet selected")
            )
            appendLine("   ${stop.purpose}")

            if (stop.needsSelection) {
                appendLine(
                    "   A specific restaurant needs to be chosen."
                )
            }
            appendLine()
        }

        appendLine("I have preserved the requested activity order.")
        appendLine(
            "The starting point and exact map entrances " +
                "are not yet confirmed."
        )
        appendLine(
            "The restaurant pickup is part of this journey, " +
                "not a request to place a food order."
        )
        appendLine()
        appendLine(
            "Please provide your starting point and choose " +
                "a restaurant for the food pickup."
        )
        appendLine(
            "No route, distance or travel time has been calculated."
        )
        append(
            "No restaurant booking or food purchase has been made."
        )
    }
}