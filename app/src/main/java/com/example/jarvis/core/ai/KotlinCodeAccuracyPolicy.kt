package com.example.jarvis.core.ai

/**
 * A conservative heuristic for one known model-generated Kotlin mistake:
 * calling add() on a variable declared using listOf().
 *
 * This is not a compiler. It never executes, rewrites, or approves code.
 * The caller must decide whether a finding is useful in context.
 */
internal object KotlinCodeAccuracyPolicy {

    private val declaration = Regex(
        """\b(?:val|var)\s+([A-Za-z_]\w*)\s*=\s*listOf\s*\("""
    )

    fun hasReadOnlyListMutation(code: String): Boolean {
        val analyzed = sanitizeInput(code)

        for (match in declaration.findAll(analyzed)) {
            val name = match.groupValues[1]
            val remaining = analyzed.substring(match.range.last + 1)
            val mutation = Regex(
                """\b${Regex.escape(name)}\s*\.\s*add\s*\("""
            ).find(remaining) ?: continue
            val between = remaining.substring(0, mutation.range.first)

            // Conservative exemption for a later explicit mutable declaration.
            // Scope and type inference are not fully modeled here.
            val mutableAssignment = Regex(
                """\b${Regex.escape(name)}\s*=\s*mutableListOf\s*\("""
            )
            if (!mutableAssignment.containsMatchIn(between)) return true
        }
        return false
    }

    private fun sanitizeInput(input: String): String {
        // First remove fenced examples explicitly identified as incorrect.
        val lines = input.lines()
        val output = mutableListOf<String>()
        var insideFence = false
        var suppressFence = false
        for (index in lines.indices) {
            val line = lines[index]
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (!insideFence) {
                    val context = lines.take(index).takeLast(3).joinToString(" ").lowercase()
                    suppressFence = context.contains("incorrect") ||
                        context.contains("invalid") ||
                        context.contains("does not compile")
                    insideFence = true
                } else {
                    insideFence = false
                    suppressFence = false
                }
                output.add("")
            } else {
                output.add(if (insideFence && suppressFence) "" else line)
            }
        }
        return maskNonCode(output.joinToString("\n"))
    }

    /** Retain code/newlines, mask strings, characters and comments. */
    private fun maskNonCode(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        var blockDepth = 0
        while (i < source.length) {
            val c = source[i]
            val next = source.getOrNull(i + 1)
            if (blockDepth > 0) {
                when {
                    c == '/' && next == '*' -> {
                        blockDepth++
                        out.append("  ")
                        i += 2
                    }
                    c == '*' && next == '/' -> {
                        blockDepth--
                        out.append("  ")
                        i += 2
                    }
                    else -> {
                        out.append(if (c == '\n') '\n' else ' ')
                        i++
                    }
                }
                continue
            }
            if (c == '/' && next == '/') {
                while (i < source.length && source[i] != '\n') {
                    out.append(' ')
                    i++
                }
                continue
            }
            if (c == '/' && next == '*') {
                blockDepth = 1
                out.append("  ")
                i += 2
                continue
            }
            if (source.startsWith("\"\"\"", i)) {
                out.append("   ")
                i += 3
                while (i < source.length && !source.startsWith("\"\"\"", i)) {
                    out.append(if (source[i] == '\n') '\n' else ' ')
                    i++
                }
                if (i < source.length) {
                    out.append("   ")
                    i += 3
                }
                continue
            }
            if (c == '"' || c == '\'') {
                val delimiter = c
                out.append(' ')
                i++
                var escaped = false
                while (i < source.length) {
                    val part = source[i]
                    out.append(if (part == '\n') '\n' else ' ')
                    i++
                    if (escaped) escaped = false
                    else if (part == '\\') escaped = true
                    else if (part == delimiter) break
                }
                continue
            }
            out.append(c)
            i++
        }
        return out.toString()
    }
}
