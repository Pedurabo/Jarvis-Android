package com.example.jarvis.skills

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingToolApprovalTest {

    private fun request(
        action: String = "PhoneAutomation",
        params: String = """{"action":"open_app","target":"com.example.one"}"""
    ) = ToolApprovalRequest(action, params)

    @Test
    fun requestRemainsPendingUntilDecision() {
        val approval = PendingToolApproval()
        val proposed = request()

        assertTrue(approval.propose(proposed))
        assertEquals(proposed, approval.pending)
    }

    @Test
    fun secondRequestCannotReplacePendingRequest() {
        val approval = PendingToolApproval()
        val first = request()
        val second = request(
            params = """{"action":"call","target":"123456"}"""
        )

        assertTrue(approval.propose(first))
        assertFalse(approval.propose(second))
        assertEquals(first, approval.pending)
    }

    @Test
    fun changedArgumentsCannotReceiveApproval() {
        val approval = PendingToolApproval()
        val original = request()
        val altered = request(
            params = """{"action":"open_app","target":"com.example.two"}"""
        )

        assertTrue(approval.propose(original))
        assertNull(approval.approve(altered))

        // Failed approval leaves the original request pending.
        assertEquals(original, approval.pending)
    }

    @Test
    fun rejectionPreventsExecutionAuthorization() {
        val approval = PendingToolApproval()
        val proposed = request()

        assertTrue(approval.propose(proposed))
        assertTrue(approval.reject())
        assertNull(approval.pending)
        assertNull(approval.approve(proposed))
    }

    @Test
    fun approvalCanBeConsumedOnlyOnce() {
        val approval = PendingToolApproval()
        val proposed = request()

        assertTrue(approval.propose(proposed))

        assertEquals(proposed, approval.approve(proposed))
        assertNull(approval.approve(proposed))
        assertNull(approval.pending)
    }

    @Test
    fun invalidRequestsAreRejected() {
        val approval = PendingToolApproval()

        assertFalse(
            approval.propose(request(action = " "))
        )

        assertFalse(
            approval.propose(request(params = "not-json"))
        )

        assertFalse(
            approval.propose(request(params = "[]"))
        )

        assertNull(approval.pending)
    }
}