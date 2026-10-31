package com.example.jarvis.core.network

/**
 * P2-B6: Seven experimental Kampala geographic areas.
 *
 * Old Kampala remains inactive because live bank
 * subdivision did not achieve complete coverage.
 *
 * Boundaries are third-party parish-derived bounding
 * rectangles, not independently KCCA-verified polygons.
 */
internal object KampalaGeoP2ApprovedAreas {

    private val enabledNames = setOf(
        "Ntinda",
        "Nakawa",
        "Kansanga",
        "Kabalagala",
        "Rubaga",
        "Muyenga",
        "Bugoloobi"
    )

    val areas: List<KampalaGeoAreaResolver.Area> =
        KampalaGeoP2CandidateCatalog.areas
            .filter { it.label in enabledNames }
            .map { candidate ->
                candidate.copy(
                    provenance = candidate.provenance
                        .replace(
                            "not production-approved",
                            "experimentally enabled for search"
                        ) +
                        " Approximate bounding rectangle; " +
                        "not an official KCCA boundary."
                )
            }
            .also { enabled ->
                require(enabled.size == enabledNames.size)
                require(
                    enabled.map { it.label }.toSet() == enabledNames
                )
            }
}