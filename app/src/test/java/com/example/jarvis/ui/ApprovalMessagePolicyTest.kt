package com.example.jarvis.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalMessagePolicyTest {

    @Test
    fun settingsApprovalNoticeIsInterim() {
        assertTrue(
            ApprovalMessagePolicy.isPendingNotice(
                "Please review the Android Settings action in the approval dialog."
            )
        )
    }

    @Test
    fun modelToolApprovalNoticeIsInterim() {
        assertTrue(
            ApprovalMessagePolicy.isPendingNotice(
                "I've requested your approval in the app. No phone action has been performed."
            )
        )
    }

    @Test
    fun finalOutcomesAreNotSuppressed() {
        assertFalse(ApprovalMessagePolicy.isPendingNotice("Action cancelled."))
        assertFalse(ApprovalMessagePolicy.isPendingNotice("Opening com.android.settings"))
        assertFalse(ApprovalMessagePolicy.isPendingNotice("Hello Jarvis"))
    }
}