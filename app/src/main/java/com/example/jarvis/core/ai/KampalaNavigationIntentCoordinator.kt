package com.example.jarvis.core.ai

/**
 * CDI.5.58-A2: Central navigation-intent classification.
 *
 * Extracts user-provided place names without verifying locations,
 * calculating routes, or invoking Android actions.
 */
internal object KampalaNavigationIntentCoordinator {

    sealed class Intent {
        data class CompleteJourney(
            val journey: KampalaJourneyIntentResolver.Journey
        ) : Intent()

        data class OriginReply(
            val origin: String
        ) : Intent()

        data class DestinationOnly(
            val destination: String
        ) : Intent()

        object Other : Intent()
    }

    fun classify(
        message: String,
        awaitingOrigin: Boolean
    ): Intent {
        KampalaJourneyIntentResolver.resolve(message)?.let {
            return Intent.CompleteJourney(it)
        }

        // A new destination request takes priority over an older
        // pending origin question.
        KampalaJourneyIntentResolver
            .destinationWithoutOrigin(message)?.let {
                return Intent.DestinationOnly(it)
            }

        if (awaitingOrigin) {
            KampalaOriginFollowUpResolver.resolve(message)?.let {
                return Intent.OriginReply(it)
            }
        }

        return Intent.Other
    }
}