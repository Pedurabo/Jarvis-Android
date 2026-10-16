package com.example.jarvis.skills

import org.json.JSONObject

/**
 * Immutable description of a proposed tool operation.
 *
 * This type represents a request, not permission to execute it.
 */
data class ToolApprovalRequest(
    val action: String,
    val parametersJson: String
)

/**
 * Holds one proposed action awaiting an explicit user decision.
 *
 * This class never invokes a tool.
 *
 * Approval returns the stored request once and consumes it.
 */
class PendingToolApproval {

    var pending: ToolApprovalRequest? = null
        private set

    fun propose(request: ToolApprovalRequest): Boolean {
        if (pending != null) return false

        if (request.action.isBlank()) return false

        val isValidJson = try {
            JSONObject(request.parametersJson)
            true
        } catch (e: org.json.JSONException) {
            false
        }

        if (!isValidJson) return false

        pending = request
        return true
    }

    /**
     * Approve only if the supplied request exactly matches
     * the one currently pending.
     *
     * Different arguments or action names are rejected.
     */
    fun approve(
        expected: ToolApprovalRequest
    ): ToolApprovalRequest? {
        val current = pending ?: return null

        if (current != expected) return null

        pending = null
        return current
    }

    fun reject(): Boolean {
        if (pending == null) return false

        pending = null
        return true
    }
}