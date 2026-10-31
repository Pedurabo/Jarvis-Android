package com.example.jarvis.core.network

import org.junit.Assert.*
import org.junit.Test

class KampalaGeoP2CandidateCatalogTest {

    @Test
    fun candidateCatalogContainsEightDistinctAreas() {
        val areas = KampalaGeoP2CandidateCatalog.areas

        assertEquals(8, areas.size)

        val names = areas.map {
            it.label.lowercase(java.util.Locale.ROOT)
        }

        assertEquals(names.size, names.distinct().size)

        assertEquals(
            setOf(
                "ntinda", "nakawa", "old kampala",
                "kansanga", "kabalagala", "rubaga",
                "muyenga", "bugoloobi"
            ),
            names.toSet()
        )
    }

    @Test
    fun everyCandidateHasValidBoundsAndProvenance() {
        for (area in KampalaGeoP2CandidateCatalog.areas) {
            assertTrue(
                area.label,
                KampalaGeoDiscoveryContract.validBounds(
                    area.bounds
                )
            )

            assertTrue(
                area.label,
                area.provenance.contains("source feature")
            )

            assertTrue(
                area.label,
                area.provenance.contains("not KCCA-verified")
            )

            assertTrue(
                area.label,
                area.provenance.contains(
                    "not production-approved"
                )
            )
        }
    }

    @Test
    fun candidateBoundsMatchReviewedSourceValues() {
        val areas = KampalaGeoP2CandidateCatalog.areas
            .associateBy { it.label }

        val ntinda = areas["Ntinda"]!!
        assertEquals(
            0.333769,
            ntinda.bounds.south,
            0.0000001
        )
        assertEquals(
            32.625731,
            ntinda.bounds.east,
            0.0000001
        )

        val bugoloobi = areas["Bugoloobi"]!!
        assertEquals(
            0.297699,
            bugoloobi.bounds.south,
            0.0000001
        )
        assertEquals(
            32.634273,
            bugoloobi.bounds.east,
            0.0000001
        )
    }

    @Test
    fun candidateResolverCanUseEachAreaIndependently() {
        val resolver = KampalaGeoAreaResolver(
            KampalaGeoP2CandidateCatalog.areas
        )

        for (area in KampalaGeoP2CandidateCatalog.areas) {
            val resolved = resolver.resolve(area.label)

            assertNotNull(area.label, resolved)
            assertEquals(area.bounds, resolved!!.bounds)
        }
    }

    @Test
    fun productionRegistryDoesNotActivateCandidates() {
        assertFalse(
            KampalaGeoP2CandidateCatalog.productionApproved
        )

        val existing = KampalaExplicitGeoDiscovery.parseOrNull(
            "find hospitals in central kampala"
        )

        assertNotNull(existing)
        assertEquals("hospital", existing!!.category)

        for (area in KampalaGeoP2CandidateCatalog.areas) {
            val result = KampalaExplicitGeoDiscovery.parseOrNull(
                "find hospitals in ${area.label}"
            )

            if (area.label == "Old Kampala") {
                assertNull(
                    "Old Kampala must remain inactive",
                    result
                )
            } else {
                assertNotNull(
                    "Enabled area missing: ${area.label}",
                    result
                )
                assertEquals(area.bounds, result!!.bounds)
            }
        }
    }

    @Test
    fun invalidOrUnknownAreasStillFailClosed() {
        val resolver = KampalaGeoAreaResolver(
            KampalaGeoP2CandidateCatalog.areas
        )

        assertNull(resolver.resolve("Imaginary Kampala Area"))
        assertNull(resolver.resolve(""))
    }
}