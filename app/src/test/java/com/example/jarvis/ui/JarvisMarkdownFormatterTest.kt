package com.example.jarvis.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.unit.sp

class JarvisMarkdownFormatterTest {

    @Test
    fun preservesPlainText() {
        val result = JarvisMarkdownFormatter.format("Hello Jarvis")
        assertEquals("Hello Jarvis", result.text)
    }

    @Test
    fun rendersBoldText() {
        val result = JarvisMarkdownFormatter.format("Hello **Jarvis**")

        assertEquals("Hello Jarvis", result.text)
        assertTrue(result.spanStyles.any {
            it.item.fontWeight == FontWeight.Bold
        })
    }

    @Test
    fun rendersItalicText() {
        val result = JarvisMarkdownFormatter.format("This is *important*")

        assertEquals("This is important", result.text)
        assertTrue(result.spanStyles.any {
            it.item.fontStyle == FontStyle.Italic
        })
    }

    @Test
    fun rendersInlineCode() {
        val result = JarvisMarkdownFormatter.format("Use `val`")

        assertEquals("Use val", result.text)
        assertTrue(result.spanStyles.any {
            it.item.fontFamily == FontFamily.Monospace
        })
    }

    @Test
    fun rendersBulletLists() {
        val result = JarvisMarkdownFormatter.format(
            "- First\n- Second"
        )

        assertEquals("• First\n• Second", result.text)
    }

    @Test
    fun preservesNumberedLists() {
        val result = JarvisMarkdownFormatter.format(
            "1. First\n2. Second"
        )

        assertEquals("1. First\n2. Second", result.text)
    }

    @Test
    fun rendersFencedCode() {
        val result = JarvisMarkdownFormatter.format(
            "```\nval answer = 42\n```"
        )

        assertEquals("val answer = 42", result.text)
        assertTrue(result.spanStyles.any {
            it.item.fontFamily == FontFamily.Monospace
        })
    }

    @Test
    fun preservesEmptyInput() {
        val result = JarvisMarkdownFormatter.format("")
        assertEquals("", result.text)
    }

    @Test
    fun doesNotTreatOrdinaryAsterisksAsFormatting() {
        val result = JarvisMarkdownFormatter.format("2 * 3 = 6")
        assertEquals("2 * 3 = 6", result.text)
    }

    @Test
    fun rendersMarkdownHeadings() {
        val result = JarvisMarkdownFormatter.format(
            "## Benefits of Kotlin"
        )

        assertEquals("Benefits of Kotlin", result.text)

        assertTrue(result.spanStyles.any {
            it.item.fontWeight == FontWeight.Bold &&
                it.item.fontSize == 19.sp
        })
    }

    @Test
    fun preservesHeadingMarkersInsideCodeBlocks() {
        val result = JarvisMarkdownFormatter.format(
            "```\n## Literal heading\n```"
        )

        assertEquals("## Literal heading", result.text)
    }

    @Test
    fun preservesHashSymbolsWithoutHeadingSyntax() {
        val result = JarvisMarkdownFormatter.format(
            "Use # for a comment"
        )

        assertEquals("Use # for a comment", result.text)
    }

    @Test
    fun headingCanContainBoldText() {
        val result = JarvisMarkdownFormatter.format(
            "### **Important** information"
        )

        assertEquals("Important information", result.text)

        assertTrue(result.spanStyles.any {
            it.item.fontSize == 17.sp
        })
    }

    @Test
    fun unmatchedFenceDoesNotConsumeRemainingResponse() {
        val result = JarvisMarkdownFormatter.format(
            "```kotlin\nval x = 42\n## Summary\n**Important**"
        )

        assertTrue(result.text.contains("## Summary").not())
        assertTrue(result.text.contains("Summary"))
        assertTrue(result.text.contains("Important"))
        assertTrue(result.spanStyles.any {
            it.item.fontWeight == FontWeight.Bold
        })
    }

    @Test
    fun validFenceStillRendersCode() {
        val result = JarvisMarkdownFormatter.format(
            "```kotlin\nval x = 42\n```\n## Summary"
        )

        assertTrue(result.text.contains("val x = 42"))
        assertTrue(result.text.contains("Summary"))
        assertTrue(result.text.contains("```").not())
        assertTrue(result.spanStyles.any {
            it.item.fontFamily == FontFamily.Monospace
        })
    }

    @Test
    fun markdownResumesAfterClosedFence() {
        val result = JarvisMarkdownFormatter.format(
            "```\nval x = 42\n```\n**Ready**"
        )

        assertTrue(result.text.contains("Ready"))
        assertTrue(result.spanStyles.any {
            it.item.fontWeight == FontWeight.Bold
        })
    }
}