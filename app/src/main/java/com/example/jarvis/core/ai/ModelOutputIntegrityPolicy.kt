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

    // CDI.5.59-C2B: Reject internal workflow directions accidentally
    // emitted as final answers. Require multiple signals to avoid
    // rejecting ordinary discussion of tools or JSON.
    private val internalWorkflowInstruction = Regex(
        """(?i)\b(?:first|then|next|before\s+responding)\b.{0,140}\b(?:call|invoke|perform|return|provide)\b.{0,120}\b(?:tool|json|coordinates|assistant's\s+final\s+answer)\b"""
    )

    private val directToolInstruction = Regex(
        """(?i)\b(?:call|invoke|perform)\s+(?:a|the)?\s*(?:(?:location|search|navigation|maps)\s+)?tool(?:\s+call)?\b"""
    )

    private val machineOutputDirective = Regex(
        """(?i)\b(?:return|provide|output)\s+(?:the\s+)?(?:result|location\s+data|response)\s+(?:as|in)\s+(?:a\s+)?json\b"""
    )
    // CDI.5.62: Reject unfilled location/address templates.
    // These are not verified destination details.
    private val unfilledLocationPlaceholder = Regex(
        """(?i)\[\s*(?:(?:street\s+)?address|city|country|location|coordinates)(?:\s*,\s*(?:(?:street\s+)?address|city|country|location|coordinates))*\s*\]"""
    )
    // CDI.5.63-A4: Recognize leaked transcript boundary markers.
    // This is a final-answer check, not a general content sanitizer.
    private val transcriptBoundaryMarker = Regex(
        """(?i)\[\s*(?:end\s+of\s+(?:transcript|conversation|dialogue)|(?:begin|start)\s+of\s+transcript|assistant\s+turn\s+(?:begins|ends))\s*\]"""
    )

    // Detect explicit requests to another assistant embedded in
    // what should be a final answer, not ordinary instructional content.
    private val assistantInstructionLeak = Regex(
        """(?im)^\s*(?:instructions\s+for\s+(?:the\s+)?assistant|assistant\s+instructions|system\s+instructions)\s*:"""
    )
    fun isSafe(query: String, answer: String): Boolean {
        if (!ModelOutputGuard.isUsable(answer)) return false

        if (protocolLeak.containsMatchIn(answer)) return false
        if (internalHeading.containsMatchIn(answer)) return false
        if (bareRoleHeading.containsMatchIn(answer)) return false

        // Do not expose internal transcript terminators in ordinary
        // answers. Permit discussion of syntax in technical queries.
        if (!technicalRequest.containsMatchIn(query) &&
            transcriptBoundaryMarker.containsMatchIn(answer)
        ) {
            return false
        }

        if (!technicalRequest.containsMatchIn(query) &&
            assistantInstructionLeak.containsMatchIn(answer)
        ) {
            return false
        }

        // Placeholder addresses are unsafe as final navigation answers.
        // Allow technical/code discussions to describe templates.
        if (!technicalRequest.containsMatchIn(query) &&
            unfilledLocationPlaceholder.containsMatchIn(answer)
        ) {
            return false
        }

        // Procedural tool directives are not suitable user-facing
        // navigation answers. Technical explanations remain allowed.
        if (!technicalRequest.containsMatchIn(query)) {
            if (internalWorkflowInstruction.containsMatchIn(answer)) return false
            if (directToolInstruction.containsMatchIn(answer)) return false
            if (machineOutputDirective.containsMatchIn(answer)) return false
        }

        if (!technicalRequest.containsMatchIn(query)) {
            if (htmlDocument.containsMatchIn(answer)) return false
            if (htmlFragment.containsMatchIn(answer)) return false
        }

        return true
    }
}
