package com.example.jarvis.core.ai

/**
 * CDI.6.0-A6.6-C
 *
 * Prevents an explicit destination clarification from
 * entering the navigation-origin reply path.
 *
 * This policy never verifies locations or executes actions.
 */
internal object KampalaNavigationTurnSafetyPolicy {

    fun canHandleOriginReply(
        pendingVerifiedDestination: String?,
        pendingDestinationClarification: String?
    ): Boolean =
        !pendingVerifiedDestination.isNullOrBlank() &&
            pendingDestinationClarification == null
}