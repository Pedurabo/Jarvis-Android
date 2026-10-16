package com.example.jarvis.core.ai

import org.json.JSONObject

/**
 * Parses model-generated responses without executing tools.
 *
 * Tool candidates are recognized only when explicitly permitted
 * by the caller. Execution authorization remains external.
 */
internal object ModelActionParser {

    sealed class Result {
        data class FinalAnswer(val text: String) : Result()

        data class ToolCall(
            val name: String,
            val params: JSONObject
        ) : Result()

        data class Rejected(val reason: String) : Result()
    }

    fun parse(
        response: String,
        allowToolCalls: Boolean
    ): Result {
        val text = response.trim()

        if (text.isBlank()) {
            return Result.Rejected("Empty model response")
        }

        // Never interpret embedded JSON or fenced examples as tools.
        if (!text.startsWith("{") || !text.endsWith("}")) {
            return Result.FinalAnswer(text)
        }

        if (!StrictActionJsonValidator.isValid(text)) {
            return Result.Rejected("Invalid structured response")
        }

        val json = try {
            JSONObject(text)
        } catch (e: Exception) {
            return Result.Rejected("Invalid structured response")
        }

        // A complete structured response must contain a tool key.
        if (!json.has("tool")) {
            return Result.FinalAnswer(text)
        }

        val toolName = json.optString("tool").trim()

        if (toolName.isBlank()) {
            return Result.Rejected("Missing tool name")
        }

        val params = json.optJSONObject("params")
            ?: return Result.Rejected("Missing or invalid parameters")

        if (toolName == "FINAL_ANSWER") {
            val content = params.optString("content").trim()

            return if (content.isNotBlank()) {
                Result.FinalAnswer(content)
            } else {
                Result.Rejected("Empty final answer")
            }
        }

        if (!allowToolCalls) {
            return Result.Rejected(
                "Tool calls are not permitted in conversation mode"
            )
        }

        // This is a candidate, never an execution authorization.
        return Result.ToolCall(toolName, params)
    }
}