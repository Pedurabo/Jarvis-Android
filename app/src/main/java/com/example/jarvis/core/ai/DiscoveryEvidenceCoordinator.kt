package com.example.jarvis.core.ai

internal object DiscoveryEvidenceCoordinator {

    enum class DecisionKind {
        SEARCH_VERIFIED_DIRECTORY,
        NEED_VERIFIED_INVENTORY,
        NEED_VERIFIED_PLACE_DETAILS,
        NEED_ORIGIN_AND_ROUTE,
        NEED_AUTHORIZED_PROVIDER,
        NEED_VERIFIED_KNOWLEDGE
    }

    data class EvidenceCapabilities(
        val placesDirectory: Boolean = true,
        val inventoryService: Boolean = false,
        val placeDetailsService: Boolean = false,
        val routeService: Boolean = false,
        val originAvailable: Boolean = false,
        val knowledgeService: Boolean = false,
        val rideProviders: Set<String> = emptySet()
    )

    data class TaskDecision(
        val task: CompositionalDiscoveryInterpreter.Task,
        val kind: DecisionKind,
        val canAttempt: Boolean,
        val explanation: String
    )

    data class Plan(
        val originalQuery: String,
        val decisions: List<TaskDecision>,
        val isCompound: Boolean
    ) {
        val allTasksSupported: Boolean
            get() = decisions.isNotEmpty() &&
                decisions.all { it.canAttempt }
    }

    fun plan(
        interpretation: CompositionalDiscoveryInterpreter.Interpretation,
        capabilities: EvidenceCapabilities = EvidenceCapabilities()
    ): Plan {
        val decisions = interpretation.tasks.map { task ->
            when (task.evidence) {

                CompositionalDiscoveryInterpreter.EvidenceRequirement.VERIFIED_PLACES -> {
                    // Cuisine is not part of the existing Places record schema.
                    // A generic restaurant result cannot establish cuisine.
                    val cuisineUnsupported = task.cuisine != null

                    TaskDecision(
                        task = task,
                        kind = DecisionKind.SEARCH_VERIFIED_DIRECTORY,
                        canAttempt = capabilities.placesDirectory &&
                            !cuisineUnsupported &&
                            task.subject != "general places",
                        explanation = when {
                            cuisineUnsupported ->
                                "The current directory cannot verify " +
                                    "${task.cuisine} cuisine or restaurant rankings."
                            task.subject == "general places" ->
                                "This place category needs a specific verified " +
                                    "directory mapping before searching."
                            !capabilities.placesDirectory ->
                                "The verified Places directory is unavailable."
                            else ->
                                "A verified category search can be attempted. " +
                                    "Results and availability are not guaranteed."
                        }
                    )
                }

                CompositionalDiscoveryInterpreter.EvidenceRequirement.VERIFIED_INVENTORY ->
                    TaskDecision(
                        task,
                        DecisionKind.NEED_VERIFIED_INVENTORY,
                        capabilities.inventoryService,
                        if (capabilities.inventoryService)
                            "A verified inventory lookup may be attempted."
                        else
                            "The Places catalog cannot verify current stock " +
                                "or whether a market sells ${task.product ?: "the requested product"}."
                    )

                CompositionalDiscoveryInterpreter.EvidenceRequirement.VERIFIED_PLACE_DETAILS ->
                    TaskDecision(
                        task,
                        DecisionKind.NEED_VERIFIED_PLACE_DETAILS,
                        capabilities.placeDetailsService,
                        if (capabilities.placeDetailsService)
                            "Verified place details may be requested."
                        else
                            "No verified service for the requested place details " +
                                "is currently connected."
                    )

                CompositionalDiscoveryInterpreter.EvidenceRequirement.ROUTING_AND_ORIGIN -> {
                    val supported = capabilities.routeService &&
                        capabilities.originAvailable

                    TaskDecision(
                        task,
                        DecisionKind.NEED_ORIGIN_AND_ROUTE,
                        supported,
                        when {
                            !capabilities.originAvailable ->
                                "Directions require a confirmed starting location."
                            !capabilities.routeService ->
                                "A trusted route service is not connected."
                            else ->
                                "A route lookup may be attempted. " +
                                    "No route has been calculated."
                        }
                    )
                }

                CompositionalDiscoveryInterpreter.EvidenceRequirement.AUTHORIZED_PROVIDER -> {
                    val supported = task.provider != null &&
                        task.provider in capabilities.rideProviders

                    TaskDecision(
                        task,
                        DecisionKind.NEED_AUTHORIZED_PROVIDER,
                        supported,
                        if (supported)
                            "A provider workflow may be offered with explicit " +
                                "user approval. No ride has been booked."
                        else
                            "${task.provider ?: "This ride provider"} is not " +
                                "connected for verified booking. No ride has been booked."
                    )
                }

                CompositionalDiscoveryInterpreter.EvidenceRequirement.VERIFIED_KNOWLEDGE ->
                    TaskDecision(
                        task,
                        DecisionKind.NEED_VERIFIED_KNOWLEDGE,
                        capabilities.knowledgeService,
                        if (capabilities.knowledgeService)
                            "A verified Knowledge lookup may be attempted."
                        else
                            "The requested geographic fact needs an authoritative " +
                                "Knowledge record. No fact has been assumed."
                    )
            }
        }

        return Plan(
            originalQuery = interpretation.originalQuery,
            decisions = decisions,
            isCompound = interpretation.requiresMultipleSources
        )
    }

    fun interpretAndPlan(
        query: String,
        capabilities: EvidenceCapabilities = EvidenceCapabilities()
    ): Plan = plan(
        CompositionalDiscoveryInterpreter.interpret(query),
        capabilities
    )
}
