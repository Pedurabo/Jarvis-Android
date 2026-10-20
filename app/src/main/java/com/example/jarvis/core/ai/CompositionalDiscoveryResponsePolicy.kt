package com.example.jarvis.core.ai

/**
 * CDI.4.2.2: Converts compositional evidence decisions into honest notices.
 *
 * Does not fetch network data, execute phone actions, calculate routes,
 * confirm market inventory, book rides, or invent local information.
 */
internal object CompositionalDiscoveryResponsePolicy {

    fun notice(query: String): String? {
        val plan = DiscoveryEvidenceCoordinator.interpretAndPlan(query)

        if (plan.decisions.isEmpty()) return null

        val needsInterception =
            plan.isCompound ||
                plan.decisions.any {
                    it.task.kind ==
                        CompositionalDiscoveryInterpreter.TaskKind.PRODUCT_AVAILABILITY
                } ||
                plan.decisions.any {
                    it.task.kind ==
                        CompositionalDiscoveryInterpreter.TaskKind.PLACE_DISCOVERY &&
                        it.task.cuisine != null
                }

        if (!needsInterception) return null

        return buildString {
            if (plan.isCompound) {
                appendLine("I understand several parts of your request:")
            } else {
                appendLine("Here's what I can verify about your request:")
            }

            appendLine()

            plan.decisions.forEachIndexed { index, decision ->
                val task = decision.task

                val label = when (task.kind) {
                    CompositionalDiscoveryInterpreter.TaskKind.PLACE_DISCOVERY ->
                        if (task.cuisine != null)
                            "${task.cuisine} restaurant discovery"
                        else
                            "Place discovery"

                    CompositionalDiscoveryInterpreter.TaskKind.PRODUCT_AVAILABILITY ->
                        "Market product availability"

                    CompositionalDiscoveryInterpreter.TaskKind.PLACE_INFORMATION ->
                        "Place details"

                    CompositionalDiscoveryInterpreter.TaskKind.DIRECTIONS ->
                        "Directions"

                    CompositionalDiscoveryInterpreter.TaskKind.TRANSPORT_REQUEST ->
                        "${task.provider ?: "Ride provider"} assistance"

                    CompositionalDiscoveryInterpreter.TaskKind.GEOGRAPHIC_KNOWLEDGE ->
                        "Geographic information"
                }

                append("${index + 1}. $label: ")
                appendLine(decision.explanation)
            }

            appendLine()

            append(
                "No inventory, route, ride booking, or unsupported " +
                    "place detail has been verified or completed by this response."
            )
        }.trim()
    }
}
