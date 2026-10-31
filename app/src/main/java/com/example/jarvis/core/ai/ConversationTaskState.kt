package com.example.jarvis.core.ai

internal object ConversationTaskState {

    enum class Kind {
        NAVIGATION,
        PLACE_DISCOVERY,
        SHOPPING,
        MARKET_COMPARISON,
        MEDICAL_ASSISTANCE,
        GENERAL
    }

    enum class Provenance {
        USER_SUPPLIED,
        DIRECTORY_LISTING,
        VERIFIED_EXTERNAL_SOURCE
    }

    enum class Field {
        DESTINATION,
        ORIGIN,
        SUGGESTED_AREA,
        SUBJECT,
        ITEM,
        PREFERENCE,
        COMPLETED_STOPS

    }

    data class Fact(
        val value: String,
        val provenance: Provenance
    )

    data class Task(
        val kind: Kind,
        val goal: String,
        val fields: Map<Field, Fact> = emptyMap(),
        val requiresUrgentCare: Boolean = false
    ) {
        fun get(field: Field): Fact? = fields[field]
    }

    sealed class Change {
        data class Start(
            val kind: Kind,
            val goal: String,
            val requiresUrgentCare: Boolean = false
        ) : Change()

        data class SetField(
            val field: Field,
            val value: String,
            val provenance: Provenance
        ) : Change()

        data class RemoveField(
            val field: Field
        ) : Change()

        object Clear : Change()
    }

    fun reduce(current: Task?, change: Change): Task? =
        when (change) {
            is Change.Start -> {
                val goal = change.goal.trim()

                if (goal.isBlank()) {
                    current
                } else {
                    Task(
                        kind = change.kind,
                        goal = goal,
                        requiresUrgentCare =
                            change.requiresUrgentCare
                    )
                }
            }

            is Change.SetField -> {
                if (current == null) {
                    null
                } else {
                    val value = change.value.trim()

                    if (value.isBlank()) {
                        current
                    } else {
                        current.copy(
                            fields = current.fields +
                                (
                                    change.field to Fact(
                                        value = value,
                                        provenance = change.provenance
                                    )
                                )
                        )
                    }
                }
            }

            is Change.RemoveField ->
                current?.copy(
                    fields = current.fields - change.field
                )

            Change.Clear -> null
        }

    fun apply(
        initial: Task? = null,
        changes: List<Change>
    ): Task? =
        changes.fold(initial) { state, change ->
            reduce(state, change)
        }

    /**
     * Continue an already recognized, user-supplied journey.
     * No coordinates, routes, bookings, or location verification.
     *
     * Only an existing NAVIGATION task is eligible.
     */
    /**
     * JARVIS-STANDALONE-ORIGIN
     *
     * Accept a user-stated location without requiring an
     * existing itinerary. This is not GPS verification.
     */
    fun standaloneOriginOrNull(message: String): String? {
        val text = message.trim()

        if (text.length !in 8..180) return null

        val pattern = Regex(
            """^(?:(?:i(?:'m| am)|im|am)\s+(?:currently\s+)?at|currently\s+at|my\s+(?:starting\s+|pickup\s+)?location\s+is)\s+(.+?)[.!]?$""",
            RegexOption.IGNORE_CASE
        )

        val match = pattern.matchEntire(text) ?: return null

        val place = match.groupValues[1].trim()

        if (place.length !in 3..120) return null

        return place
    }
    fun journeyFollowUpOrNull(
        current: Task?,
        message: String
    ): Pair<Task, String>? {
        if (current?.kind != Kind.NAVIGATION) return null
        if (current.goal.isBlank()) return null

        val text = message.trim()
        if (text.length !in 3..300) return null

        val originPattern = Regex(
            """^(?:(?:i(?:'m| am)|im|am)\s+(?:currently\s+)?at|currently\s+at|my\s+(?:starting\s+)?location\s+is)\s+(.+?)[.!]?$""",
            RegexOption.IGNORE_CASE
        )

        val originMatch = originPattern.matchEntire(text)

        if (originMatch != null) {
            val suppliedOrigin =
                originMatch.groupValues[1].trim()

            if (suppliedOrigin.length !in 3..120) return null

            val updated = reduce(
                current,
                Change.SetField(
                    field = Field.ORIGIN,
                    value = suppliedOrigin,
                    provenance = Provenance.USER_SUPPLIED
                )
            ) ?: return null

            return updated to journeySummary(updated)
        }

        val detailsPattern = Regex(
            """\b(?:what\s+details|what\s+(?:else|information)\s+do\s+you\s+need|what\s+is\s+missing)\b""",
            RegexOption.IGNORE_CASE
        )

        if (detailsPattern.containsMatchIn(text)) {
            return current to journeySummary(current)
        }

        // Explicit user confirmation only.
        // Never infer task completion from a place name,
        // GPS movement, or a generic conversation.
        val plan = KampalaP3B2ItineraryPlanner.extract(current.goal)

        if (plan != null && current.get(Field.COMPLETED_STOPS) == null) {
            val firstStop = plan.stops.firstOrNull()

            val pickupConfirmation = Regex(
                """^(?:i(?:'ve| have)?\s+)?(?:picked\s+up|collected)\s+(?:my\s+cousin|the\s+passenger)[.!]?$""",
                RegexOption.IGNORE_CASE
            )

            if (
                firstStop?.activity ==
                    KampalaP3B2ItineraryPlanner.Activity.PICK_UP_PERSON &&
                pickupConfirmation.matches(text)
            ) {
                val updated = reduce(
                    current,
                    Change.SetField(
                        Field.COMPLETED_STOPS,
                        "1",
                        Provenance.USER_SUPPLIED
                    )
                ) ?: return null

                return updated to journeySummary(updated)
            }
        }

        val nextPattern = Regex(
            """^(?:and\s+)?what(?:'s|\s+is)\s+next[.!?]?$""",
            RegexOption.IGNORE_CASE
        )

        if (nextPattern.matches(text)) {
            return current to journeySummary(current)
        }

        return null
    }

    private fun journeySummary(task: Task): String {
        val plan = KampalaP3B2ItineraryPlanner.extract(task.goal)
        val origin = task.get(Field.ORIGIN)?.value
        val completed = task.get(Field.COMPLETED_STOPS)
            ?.value
            ?.toIntOrNull()
            ?.coerceAtLeast(0) ?: 0

        return buildString {
            appendLine("Your current journey")
            appendLine()
            appendLine(
                "Starting point: ${origin ?: "Not provided"}"
            )
            if (origin != null) {
                appendLine(
                    "(User supplied; not GPS-verified)"
                )
            }
            appendLine()

            if (plan == null) {
                appendLine("Original journey:")
                appendLine(task.goal)
                appendLine()
                appendLine(
                    "The individual activities have not " +
                        "yet been extracted reliably."
                )
            } else {
                plan.stops.forEachIndexed { index, stop ->
                    val name =
                        stop.requestedPlace
                            ?: "Restaurant not yet selected"

                    val status =
                        if (index < completed) {
                            "COMPLETED"
                        } else {
                            "PENDING"
                        }

                    appendLine(
                        "${index + 1}. $name [$status]"
                    )
                    appendLine("   ${stop.purpose}")
                }

                appendLine()

                val next = plan.stops.getOrNull(completed)

                if (next != null) {
                    appendLine(
                        "Next planned stop: " +
                            (next.requestedPlace
                                ?: "Choose a restaurant")
                    )
                } else {
                    appendLine(
                        "All listed activities are " +
                            "marked completed."
                    )
                }

                appendLine()

                if (origin == null) {
                    appendLine(
                        "Still needed: your starting point."
                    )
                }

                if (
                    plan.stops.any {
                        it.needsSelection &&
                            plan.stops.indexOf(it) >= completed
                    }
                ) {
                    appendLine(
                        "Still needed: choose a restaurant " +
                            "for the food pickup."
                    )
                }
            }

            appendLine()
            appendLine(
                "Place names and entrances have not " +
                    "been independently verified."
            )
            append(
                "No route, booking, GPS fix or " +
                    "travel time has been confirmed."
            )
        }
    }
}