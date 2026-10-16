package com.example.jarvis.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal object JarvisMarkdownFormatter {

    private val inlinePattern =
        Regex("""(\*\*([^*]+)\*\*|\*([^*]+)\*|`([^`]+)`)""")

    private val headingPattern =
        Regex("""^ {0,3}(#{1,3})[ \t]+(.+?)\s*#*\s*$""")

    private val bulletPattern =
        Regex("""^(\s*)[-*+] (.+)$""")

    private val numberedPattern =
        Regex("""^(\s*)(\d+)[.)] (.+)$""")

    fun format(source: String): AnnotatedString {
        val output = AnnotatedString.Builder()
        var inCodeBlock = false
        var emittedLine = false

        val lines = source.lines()

        val fencePattern = Regex("""^ {0,3}`{3,}(?:[^\s`]*)[ \t]*$""")
        val closingFencePattern = Regex("""^ {0,3}`{3,}[ \t]*$""")

        lines.forEachIndexed { index, line ->
            if (inCodeBlock && closingFencePattern.matches(line)) {
                inCodeBlock = false
                return@forEachIndexed
            }

            if (!inCodeBlock && fencePattern.matches(line)) {
                val hasClosingFence = lines
                    .drop(index + 1)
                    .any { closingFencePattern.matches(it) }

                if (hasClosingFence) {
                    inCodeBlock = true
                    return@forEachIndexed
                }
            }

            if (emittedLine) {
                output.append("\n")
            }
            emittedLine = true

            if (inCodeBlock) {
                val start = output.length
                output.append(line)
                output.addStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace),
                    start,
                    output.length
                )
                return@forEachIndexed
            }

            val bullet = bulletPattern.matchEntire(line)
            val numbered = numberedPattern.matchEntire(line)

            val heading = headingPattern.matchEntire(line)

            when {
                heading != null -> {
                    val level = heading.groupValues[1].length
                    val headingText = heading.groupValues[2]

                    val size = when (level) {
                        1 -> 22.sp
                        2 -> 19.sp
                        else -> 17.sp
                    }

                    val start = output.length
                    appendInline(output, headingText)

                    output.addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = size
                        ),
                        start,
                        output.length
                    )
                }

                bullet != null -> {
                    output.append(bullet.groupValues[1])
                    output.append("• ")
                    appendInline(output, bullet.groupValues[2])
                }

                numbered != null -> {
                    output.append(numbered.groupValues[1])
                    output.append(numbered.groupValues[2])
                    output.append(". ")
                    appendInline(output, numbered.groupValues[3])
                }

                else -> appendInline(output, line)
            }
        }

        return output.toAnnotatedString()
    }

    private fun appendInline(
        output: AnnotatedString.Builder,
        line: String
    ) {
        var cursor = 0

        inlinePattern.findAll(line).forEach { match ->
            output.append(line.substring(cursor, match.range.first))

            val bold = match.groupValues[2]
            val italic = match.groupValues[3]
            val code = match.groupValues[4]

            val content = when {
                bold.isNotEmpty() -> bold
                italic.isNotEmpty() -> italic
                else -> code
            }

            val start = output.length
            output.append(content)

            val style = when {
                bold.isNotEmpty() ->
                    SpanStyle(fontWeight = FontWeight.Bold)

                italic.isNotEmpty() ->
                    SpanStyle(fontStyle = FontStyle.Italic)

                else ->
                    SpanStyle(fontFamily = FontFamily.Monospace)
            }

            output.addStyle(style, start, output.length)
            cursor = match.range.last + 1
        }

        output.append(line.substring(cursor))
    }
}