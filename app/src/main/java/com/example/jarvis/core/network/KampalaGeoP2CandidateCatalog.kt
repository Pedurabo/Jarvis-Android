package com.example.jarvis.core.network

/**
 * P2-B3 candidate geographic areas.
 *
 * Source:
 * ArcGIS Map_2_June_WFL1 FeatureServer/1
 * Uganda_2024_Parishes
 *
 * Coordinates are WGS84 bounding rectangles derived
 * from polygon extents, not polygon containment checks.
 *
 * None of these candidates has been independently
 * verified against KCCA administrative boundary data.
 *
 * IMPORTANT:
 * This object is NOT part of the production DEFAULT
 * resolver until separately validated and approved.
 */
internal object KampalaGeoP2CandidateCatalog {

    const val productionApproved = false

    const val sourceUrl =
        "https://services1.arcgis.com/49FlB10KwAxJa07X/" +
        "ArcGIS/rest/services/Map_2_June_WFL1/FeatureServer/1"

    private fun candidate(
        name: String,
        featureId: Int,
        south: Double,
        west: Double,
        north: Double,
        east: Double
    ): KampalaGeoAreaResolver.Area =
        KampalaGeoAreaResolver.Area(
            label = name,
            bounds = KampalaGeoDiscoveryContract.Bounds(
                south = south,
                west = west,
                north = north,
                east = east
            ),
            provenance =
                "Candidate Uganda 2024 parish, source feature " +
                featureId +
                "; third-party polygon-derived bounding rectangle; " +
                "not KCCA-verified; not production-approved; " +
                "bbox may include places outside the parish polygon."
        )

    val areas: List<KampalaGeoAreaResolver.Area> = listOf(
        candidate(
            "Ntinda", 104,
            0.333769, 32.608678, 0.357310, 32.625731
        ),
        candidate(
            "Nakawa", 93,
            0.322134, 32.606678, 0.333170, 32.617612
        ),
        candidate(
            "Old Kampala", 38,
            0.313066, 32.563972, 0.322143, 32.574226
        ),
        candidate(
            "Kansanga", 64,
            0.278669, 32.597983, 0.300176, 32.621850
        ),
        candidate(
            "Kabalagala", 63,
            0.292822, 32.594669, 0.306226, 32.604870
        ),
        candidate(
            "Rubaga", 121,
            0.291924, 32.536740, 0.308931, 32.561291
        ),
        candidate(
            "Muyenga", 67,
            0.280825, 32.608884, 0.303363, 32.626165
        ),
        candidate(
            "Bugoloobi", 84,
            0.297699, 32.611044, 0.321395, 32.634273
        )
    )
}