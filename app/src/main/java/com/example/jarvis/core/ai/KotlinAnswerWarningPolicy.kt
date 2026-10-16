package com.example.jarvis.core.ai

/** Displays a conservative Kotlin fix ahead of the preserved model output. */
internal object KotlinAnswerWarningPolicy {
    private val kotlinKeyword = Regex("""\bkotlin\b""", RegexOption.IGNORE_CASE)
    private const val WARNING =
        "**Kotlin accuracy warning:** This example may call " +
        "`add()` on a read-only list created with `listOf()`. " +
        "Use `mutableListOf()` for mutation or create a new list. " +
        "This code has not been compiler-verified."

    fun decorate(query: String, answer: String): String {
        if (TechnicalTaskPolicy.classify(query) != TechnicalTaskPolicy.Kind.CODE_TASK) return answer
        if (!kotlinKeyword.containsMatchIn(query)) return answer
        if (!KotlinCodeAccuracyPolicy.hasReadOnlyListMutation(answer)) return answer
        if (answer.contains(WARNING)) return answer

        val suggestedFunction = KotlinSafeCorrectionPolicy.suggest(answer)
        if (suggestedFunction == null) {
            // No safe correction can be proposed: retain the original warning behavior.
            return answer.trimEnd() + "\n\n" + WARNING
        }

        return buildString {
            append("**Suggested correction (not compiler-verified):**\n\n")
            append("```kotlin\n")
            append(suggestedFunction.trim())
            append("\n```\n\n")
            append(WARNING)
            append("\n\n**Original generated answer (may be incorrect):**\n\n")
            append(answer.trimEnd())
        }
    }
}
