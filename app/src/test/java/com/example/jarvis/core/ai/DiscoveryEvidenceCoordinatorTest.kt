package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryEvidenceCoordinatorTest {

    private fun plan(query: String) =
        DiscoveryEvidenceCoordinator.interpretAndPlan(query)

    @Test
    fun spiceTripPreservesAllFourRequirements() {
        val result = plan(
            "Which market has spices for cooking, how do I get there, " +
                "and can I order a SafeBoda or get me an Uber?"
        )

        assertEquals(4, result.decisions.size)
        assertTrue(result.isCompound)
        assertFalse(result.allTasksSupported)
    }

    @Test
    fun marketInventoryNeedsVerifiedStockData() {
        val result = plan(
            "Which market has spices for cooking?"
        )

        assertEquals(
            DiscoveryEvidenceCoordinator.DecisionKind.NEED_VERIFIED_INVENTORY,
            result.decisions.single().kind
        )
        assertFalse(result.decisions.single().canAttempt)
    }

    @Test
    fun directionsRequireOriginAndRouteEvidence() {
        val result = plan(
            "How do I get there?"
        )

        assertEquals(
            DiscoveryEvidenceCoordinator.DecisionKind.NEED_ORIGIN_AND_ROUTE,
            result.decisions.single().kind
        )
        assertFalse(result.decisions.single().canAttempt)
    }

    @Test
    fun rideProvidersRemainSeparateAndUnbooked() {
        val result = plan(
            "Can I order a SafeBoda or get me an Uber?"
        )

        assertEquals(2, result.decisions.size)
        assertTrue(result.decisions.all {
            it.kind ==
                DiscoveryEvidenceCoordinator.DecisionKind.NEED_AUTHORIZED_PROVIDER
        })
        assertTrue(result.decisions.all { !it.canAttempt })
        assertTrue(result.decisions.all {
            it.explanation.contains("No ride has been booked")
        })
    }

    @Test
    fun cuisineCannotBeInferredFromGenericRestaurantRecord() {
        val result = plan(
            "Where can I go to have Chinese cuisine?"
        )

        assertEquals(
            "chinese",
            result.decisions.single().task.cuisine
        )
        assertFalse(result.decisions.single().canAttempt)
    }

    @Test
    fun ordinaryMarketDiscoveryCanUseVerifiedDirectory() {
        val result = plan("Find markets in Kampala")

        assertEquals(
            DiscoveryEvidenceCoordinator.DecisionKind.SEARCH_VERIFIED_DIRECTORY,
            result.decisions.single().kind
        )
        assertTrue(result.decisions.single().canAttempt)
    }

    @Test
    fun unavailableDirectoryCannotBeAttempted() {
        val result = DiscoveryEvidenceCoordinator.interpretAndPlan(
            "Find markets in Kampala",
            DiscoveryEvidenceCoordinator.EvidenceCapabilities(
                placesDirectory = false
            )
        )

        assertFalse(result.decisions.single().canAttempt)
    }

    @Test
    fun inventoryCapabilityCanBeExplicitlyEnabled() {
        val result = DiscoveryEvidenceCoordinator.interpretAndPlan(
            "Which market has spices?",
            DiscoveryEvidenceCoordinator.EvidenceCapabilities(
                inventoryService = true
            )
        )

        assertTrue(result.decisions.single().canAttempt)
    }

    @Test
    fun routeRequiresBothCapabilities() {
        val query = "How do I get there?"

        val withoutOrigin = DiscoveryEvidenceCoordinator.interpretAndPlan(
            query,
            DiscoveryEvidenceCoordinator.EvidenceCapabilities(
                routeService = true
            )
        )

        assertFalse(withoutOrigin.decisions.single().canAttempt)

        val withBoth = DiscoveryEvidenceCoordinator.interpretAndPlan(
            query,
            DiscoveryEvidenceCoordinator.EvidenceCapabilities(
                routeService = true,
                originAvailable = true
            )
        )

        assertTrue(withBoth.decisions.single().canAttempt)
    }

    @Test
    fun providerCapabilityDoesNotClaimBooking() {
        val result = DiscoveryEvidenceCoordinator.interpretAndPlan(
            "Can I order a SafeBoda?",
            DiscoveryEvidenceCoordinator.EvidenceCapabilities(
                rideProviders = setOf("safeboda")
            )
        )

        assertTrue(result.decisions.single().canAttempt)
        assertTrue(
            result.decisions.single().explanation
                .contains("No ride has been booked")
        )
    }

    @Test
    fun geographicalFactsNeedVerifiedKnowledge() {
        val result = plan("What are the 6 hills of Kampala?")

        assertEquals(
            DiscoveryEvidenceCoordinator.DecisionKind.NEED_VERIFIED_KNOWLEDGE,
            result.decisions.single().kind
        )
        assertFalse(result.decisions.single().canAttempt)
    }

    @Test
    fun phoneCommandsRemainOutsideDiscoveryPlanning() {
        assertTrue(
            plan("Open Android Settings").decisions.isEmpty()
        )
        assertTrue(
            plan("Call the palace").decisions.isEmpty()
        )
    }

    @Test
    fun programmingRequestsRemainOutsideDiscoveryPlanning() {
        assertTrue(
            plan("Explain dependency injection in Kotlin")
                .decisions.isEmpty()
        )
    }

    @Test
    fun unrecognizedRequestsAreNotMarkedSupported() {
        val result = plan("Hello Jarvis")

        assertTrue(result.decisions.isEmpty())
        assertFalse(result.allTasksSupported)
    }
}
