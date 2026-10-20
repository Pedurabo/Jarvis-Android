package com.example.jarvis.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CloudInferenceAvailabilityPolicyTest {

    @Test
    fun cloudIsDisabledUntilExplicitlyConfigured() {
        assertFalse(CloudInferenceAvailabilityPolicy.isEnabled)
    }

    @Test
    fun localRequestsRemainLocal() {
        assertEquals(
            ModelRouter.Route.LOCAL,
            CloudInferenceAvailabilityPolicy.effectiveRoute(
                ModelRouter.Route.LOCAL
            )
        )
    }

    @Test
    fun cloudRequiredIntentUsesLocalModel() {
        assertEquals(
            ModelRouter.Route.LOCAL,
            CloudInferenceAvailabilityPolicy.effectiveRoute(
                ModelRouter.Route.CLOUD
            )
        )
    }

    @Test
    fun retryEscalationCannotReachDevelopmentBackend() {
        val retryRoute = ModelRouter.Route.CLOUD

        assertEquals(
            ModelRouter.Route.LOCAL,
            CloudInferenceAvailabilityPolicy.effectiveRoute(retryRoute)
        )
    }
}
