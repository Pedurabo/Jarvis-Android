package com.example.jarvis.core.ai

/**
 * Suggests a correction for a deliberately narrow shape of Kotlin function.
 * This is not a compiler, and never executes or changes the original answer.
 */
internal object KotlinSafeCorrectionPolicy {
    private val simpleFunction = Regex(
        """\bfun\s+[A-Za-z_]\w*\s*\(\s*\)\s*\{[^{}]*\}""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val declaration = Regex(
        """(?m)^(\s*)(val|var)\s+([A-Za-z_]\w*)\s*=\s*listOf\s*\("""
    )

    /** Returns only a proposed corrected function, never the rewritten answer. */
    fun suggest(answer: String): String? {
        val functions = simpleFunction.findAll(answer).toList()
        if (functions.size != 1) return null
        val original = functions.single().value
        val declarations = declaration.findAll(original).toList()
        if (declarations.size != 1) return null

        val declarationMatch = declarations.single()
        val variable = declarationMatch.groupValues[3]
        val bodyAfterDeclaration = original.substring(declarationMatch.range.last + 1)
        val mutation = Regex("""\b${Regex.escape(variable)}\s*\.\s*add\s*\(""")
        if (!mutation.containsMatchIn(bodyAfterDeclaration)) return null

        // Do not suggest changes when this name is assigned again or redeclared.
        val laterDeclaration = Regex("""\b(?:val|var)\s+${Regex.escape(variable)}\b""")
        val reassignment = Regex("""\b${Regex.escape(variable)}\s*=(?!=)""")
        if (laterDeclaration.containsMatchIn(bodyAfterDeclaration) ||
            reassignment.containsMatchIn(bodyAfterDeclaration)) return null

        // Refuse complex content. A suggestion is safe only for simple, plain Kotlin lines.
        if (original.contains("/*") || original.contains("\"\"\"") ||
            original.contains("->") || original.contains("@") ||
            original.contains("try ") || original.contains("for (") ||
            original.contains("while (") || original.contains("if (")) return null

        val listOfStart = declarationMatch.range.last - "listOf(".length + 1
        if (listOfStart < 0 || original.substring(listOfStart, listOfStart + 7) != "listOf(") {
            return null
        }
        return original.replaceRange(listOfStart, listOfStart + 6, "mutableListOf")
    }
}
