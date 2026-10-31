package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.9-D4E-R4
 *
 * Recognizes a fresh complete journey independently of
 * any previous navigation state.
 *
 * Recognition never verifies a location, invokes a tool,
 * contacts a provider, or launches an Android activity.
 */
internal object KampalaFreshJourneyPriority {

    fun resolveOrNull(
        message: String
    ): KampalaJourneyIntentResolver.Journey? {
        return (
            KampalaNavigationIntentCoordinator.classify(
                message = message,
                awaitingOrigin = false
            ) as? KampalaNavigationIntentCoordinator.Intent.CompleteJourney
        )?.journey
    }
}