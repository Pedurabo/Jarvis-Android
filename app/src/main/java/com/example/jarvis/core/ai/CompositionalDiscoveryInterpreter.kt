package com.example.jarvis.core.ai

/**
 * CDI.4: Pure Kotlin interpretation of compositional discovery requests.
 *
 * Describes evidence requirements without claiming that evidence exists.
 * Does not access networks, launch apps, book rides or execute payments.
 */
internal object CompositionalDiscoveryInterpreter {

    enum class TaskKind {
        PLACE_DISCOVERY,
        PRODUCT_AVAILABILITY,
        PLACE_INFORMATION,
        DIRECTIONS,
        TRANSPORT_REQUEST,
        GEOGRAPHIC_KNOWLEDGE
    }

    enum class EvidenceRequirement {
        VERIFIED_PLACES,
        VERIFIED_INVENTORY,
        VERIFIED_PLACE_DETAILS,
        ROUTING_AND_ORIGIN,
        AUTHORIZED_PROVIDER,
        VERIFIED_KNOWLEDGE
    }

    data class Task(
        val kind: TaskKind,
        val subject: String,
        val area: String? = null,
        val cuisine: String? = null,
        val product: String? = null,
        val provider: String? = null,
        val evidence: EvidenceRequirement
    )

    data class Interpretation(
        val originalQuery: String,
        val tasks: List<Task>,
        val requiresMultipleSources: Boolean
    )

    private fun pattern(value: String) =
        Regex(value, RegexOption.IGNORE_CASE)

    private val protectedAction = pattern(
        """^\s*(?:open|launch|call|dial|send|text|message|set|turn\s+on|turn\s+off)\b"""
    )

    private val areaPattern = pattern(
        """\b(?:in|around|near)\s+(kololo|nakasero|makindye|ntinda|kisementi|bugolobi|old\s+kampala|kampala)\b"""
    )

    private val cuisinePattern = pattern(
        """\b(chinese|turkish|indian|italian|japanese|korean|ethiopian)\b"""
    )

    private val restaurantPattern = pattern(
        """\b(restaurants?|places?\s+to\s+eat|where\s+can\s+i\s+(?:go\s+to\s+)?(?:have|eat)|cuisine|meal|dining)\b"""
    )

    private val marketPattern = pattern(
        """\b(markets?|spice\s+stalls?|spice\s+shops?)\b"""
    )

    private val productPattern = pattern(
        """\b(spices?|vegetables?|fruits?|matooke|rice|beans|fish|meat|food)\b"""
    )

    // CDI.4.1-R: "which market has spices" expresses inventory intent.
    private val inventoryPattern = pattern(
        """\b(has|have|carries|carry|sell|sells|sold|selling|stock|stocks|stocked|available|find|buy|purchase|kinds?|types?|products?|need\s+for\s+cooking)\b"""
    )

    private val directionPattern = pattern(
        """\b(how\s+do\s+i\s+get\s+(?:there|to)|directions?|route\s+to|navigate\s+to|how\s+to\s+reach)\b"""
    )

    private val ridePattern = pattern(
        """\b(safe\s*boda|safeboda|uber|faras|boda\s*boda|taxi)\b"""
    )

    private val rideIntent = pattern(
        """\b(order|book|request|get\s+me|arrange|hire|call|can\s+i\s+get|can\s+i\s+order)\b"""
    )

    private val geographicPattern = pattern(
        """\b(hills?|lakes?|divisions?|subcounties|sub-counties|population|landmarks?)\b"""
    )

    private val generalPlacePattern = pattern(
        """\b(hotels?|guesthouses?|shopping\s+(?:malls?|places?|centres?)|cinemas?|football\s+grounds?|museums?)\b"""
    )

    fun interpret(input: String): Interpretation {
        val query = input.trim().replace(Regex("""\s+"""), " ")

        if (query.isBlank() || protectedAction.containsMatchIn(query)) {
            return Interpretation(query, emptyList(), false)
        }

        val normalized = query.lowercase()

        val area = areaPattern.find(normalized)
            ?.groupValues?.get(1)
            ?.replace(Regex("""\s+"""), " ")

        val product = productPattern.find(normalized)?.value
        val cuisine = cuisinePattern.find(normalized)?.value

        val tasks = mutableListOf<Task>()

        fun add(task: Task) {
            if (tasks.none {
                    it.kind == task.kind &&
                    it.subject == task.subject &&
                    it.provider == task.provider
                }) {
                tasks.add(task)
            }
        }

        val hasMarket = marketPattern.containsMatchIn(normalized)

        val inventoryRequested =
            hasMarket &&
            product != null &&
            inventoryPattern.containsMatchIn(normalized)

        if (inventoryRequested) {
            add(
                Task(
                    kind = TaskKind.PRODUCT_AVAILABILITY,
                    subject = "market inventory",
                    area = area,
                    product = product,
                    evidence = EvidenceRequirement.VERIFIED_INVENTORY
                )
            )
        }

        if (restaurantPattern.containsMatchIn(normalized) &&
            (cuisine != null ||
                pattern("""\brestaurants?\b""").containsMatchIn(normalized))) {
            add(
                Task(
                    kind = TaskKind.PLACE_DISCOVERY,
                    subject = "restaurant",
                    area = area,
                    cuisine = cuisine,
                    evidence = EvidenceRequirement.VERIFIED_PLACES
                )
            )
        }

        if (hasMarket &&
            !inventoryRequested &&
            pattern("""\b(find|where|list|recommend|show|nearest|closest)\b""")
                .containsMatchIn(normalized)) {
            add(
                Task(
                    kind = TaskKind.PLACE_DISCOVERY,
                    subject = "market",
                    area = area,
                    evidence = EvidenceRequirement.VERIFIED_PLACES
                )
            )
        }

        if (directionPattern.containsMatchIn(normalized)) {
            add(
                Task(
                    kind = TaskKind.DIRECTIONS,
                    subject = "destination in request",
                    area = area,
                    evidence = EvidenceRequirement.ROUTING_AND_ORIGIN
                )
            )
        }

        if (ridePattern.containsMatchIn(normalized) &&
            rideIntent.containsMatchIn(normalized)) {

            listOf("safeboda", "uber", "faras").forEach { provider ->
                val present = when (provider) {
                    "safeboda" ->
                        pattern("""\bsafe\s*boda\b""")
                            .containsMatchIn(normalized)
                    else ->
                        pattern("""\b${provider}\b""")
                            .containsMatchIn(normalized)
                }

                if (present) {
                    add(
                        Task(
                            kind = TaskKind.TRANSPORT_REQUEST,
                            subject = "ride",
                            provider = provider,
                            evidence = EvidenceRequirement.AUTHORIZED_PROVIDER
                        )
                    )
                }
            }
        }

        if (geographicPattern.containsMatchIn(normalized) &&
            (normalized.contains("kampala") ||
                normalized.contains("makindye"))) {
            add(
                Task(
                    kind = TaskKind.GEOGRAPHIC_KNOWLEDGE,
                    subject = "Kampala geographic information",
                    area = area,
                    evidence = EvidenceRequirement.VERIFIED_KNOWLEDGE
                )
            )
        }

        if (hasMarket &&
            pattern("""\b(nakasero|nakawa|owino|kalerwe)\s+market\b""")
                .containsMatchIn(normalized) &&
            normalized.contains("what") &&
            !inventoryRequested) {
            add(
                Task(
                    kind = TaskKind.PLACE_INFORMATION,
                    subject = "named market",
                    area = area,
                    evidence = EvidenceRequirement.VERIFIED_PLACE_DETAILS
                )
            )
        }

        if (generalPlacePattern.containsMatchIn(normalized) &&
            pattern("""\b(find|show|list|recommend|where|what\s+are)\b""")
                .containsMatchIn(normalized)) {
            add(
                Task(
                    kind = TaskKind.PLACE_DISCOVERY,
                    subject = "general places",
                    area = area,
                    evidence = EvidenceRequirement.VERIFIED_PLACES
                )
            )
        }

        return Interpretation(
            originalQuery = query,
            tasks = tasks.toList(),
            requiresMultipleSources = tasks.size > 1
        )
    }
}
