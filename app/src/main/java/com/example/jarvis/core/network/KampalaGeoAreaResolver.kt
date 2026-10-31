package com.example.jarvis.core.network

/**
 * P2-B1 geographic area catalog.
 *
 * Area bounds are not acquired from GPS.
 * Registry entries must identify their boundary provenance.
 *
 * The current Central Kampala entry is a predefined
 * approximate search rectangle, not an official boundary.
 *
 * Unknown areas fail closed rather than receiving
 * fabricated coordinates.
 */
internal class KampalaGeoAreaResolver(
    private val areas: List<Area>
) {
    data class Area(
        val label: String,
        val bounds: KampalaGeoDiscoveryContract.Bounds,
        val provenance: String
    )

    init {
        require(areas.all {
            it.label.isNotBlank() &&
                it.provenance.isNotBlank() &&
                KampalaGeoDiscoveryContract.validBounds(it.bounds)
        }) {
            "Area entries require labels, provenance and valid bounds."
        }

        require(
            areas.map {
                it.label.trim().lowercase(java.util.Locale.ROOT)
            }.distinct().size == areas.size
        ) {
            "Duplicate geographic area labels are not allowed."
        }
    }

    fun resolve(name: String): Area? {
        val normalized = name
            .trim()
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("""\s+"""), " ")

        return areas.firstOrNull {
            it.label.lowercase(java.util.Locale.ROOT) == normalized
        }
    }

    companion object {
        val DEFAULT = KampalaGeoAreaResolver(
            listOf(
                Area(
                    label = "Central Kampala",
                    bounds = KampalaGeoDiscoveryContract.Bounds(
                        south = 0.300000,
                        west = 32.550000,
                        north = 0.350000,
                        east = 32.600000
                    ),
                    provenance = "Existing approximate predefined " +
                        "development search rectangle; " +
                        "not an official administrative boundary."
                )
            ) + KampalaGeoP2ApprovedAreas.areas
        )
    }
}