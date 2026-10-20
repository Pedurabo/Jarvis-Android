package com.example.jarvis.core.ai

/**
 * Detects clear structural leaks in model-generated final answers.
 *
 * This is not factual verification and does not authorize tool actions.
 * Verified directory and Knowledge responses bypass this model-only check.
 */
internal object ModelOutputIntegrityPolicy {

    private val protocolLeak = Regex(
        """(?i)<\|(?:system|developer|assistant|tool|tool_call|tool_response|function_call|function_response)\|>|</?function_call>"""
    )

    private val internalHeading = Regex(
        """(?im)^\s*(?:developer\s+instructions?|system\s+prompt|available\s+tools|tool\s+schema|tool\s+selection|provided\s+context|assistant's\s+answer)\s*:"""
    )

    private val bareRoleHeading = Regex(
        """(?im)^\s*(?:system|developer|assistant|tool)\s*:\s*$"""
    )

    private val htmlDocument = Regex(
        """(?is)<!doctype\s+html\b|</?html(?:\s|>)|</?body(?:\s|>)"""
    )

    private val htmlFragment = Regex(
        """(?is)</?(?:div|span|script|iframe|style|section|article|nav|header|footer|form)(?:\s[^<>]*)?>"""
    )

    private val technicalRequest = Regex(
        """(?i)\b(?:html|xml|css|javascript|typescript|kotlin|python|java|json|sql|markdown|code|program|script|markup|webpage|website|web\s+page|web\s+site|regex)\b"""
    )

    fun isSafe(query: String, answer: String): Boolean {
        if (!ModelOutputGuard.isUsable(answer)) return false

        if (protocolLeak.containsMatchIn(answer)) return false
        if (internalHeading.containsMatchIn(answer)) return false
        if (bareRoleHeading.containsMatchIn(answer)) return false

        if (!technicalRequest.containsMatchIn(query)) {
            if (htmlDocument.containsMatchIn(answer)) return false
            if (htmlFragment.containsMatchIn(answer)) return false
        }

        return true
    }
}
