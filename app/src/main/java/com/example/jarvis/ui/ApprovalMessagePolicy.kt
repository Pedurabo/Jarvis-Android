package com.example.jarvis.ui

/**
 * Distinguishes temporary approval notices from final assistant replies.
 *
 * The confirmation dialog owns the pending state.
 * The approval/rejection handler owns the final outcome.
 */
object ApprovalMessagePolicy {

    fun isPendingNotice(message: String): Boolean {
        return message ==
            "Please review the Android Settings action in the approval dialog." ||
            message ==
            "I've requested your approval in the app. No phone action has been performed."
    }
}