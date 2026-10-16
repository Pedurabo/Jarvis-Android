package com.example.jarvis.core.ai

/**
 * Rejects malformed model outputs at the assistant boundary.
 * This policy does not authorize or execute tools.
 */
internal object ModelOutputGuard {

    private val protocolToken = Regex(
        """<\|(?:begin_of_text|end_of_text|start_header_id|end_header_id|eot_id|eom_id|im_start|im_end|fim_prefix|fim_middle)\|>""",
        RegexOption.IGNORE_CASE
    )

    fun isUsable(output: String): Boolean =
        output.isNotBlank() && !protocolToken.containsMatchIn(output)
}