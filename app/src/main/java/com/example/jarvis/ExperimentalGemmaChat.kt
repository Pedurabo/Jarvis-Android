package com.example.jarvis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class ExperimentalChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ExperimentalGemmaChat(
    messages: List<ExperimentalChatMessage>,
    input: String,
    generating: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onClearConversation: () -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboardManager.current
    val uiScope = rememberCoroutineScope()

    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisible = scrollState.layoutInfo.visibleItemsInfo
                .lastOrNull()?.index ?: -1

            val lastItem = scrollState.layoutInfo.totalItemsCount - 1

            lastItem >= 0 && lastVisible < lastItem
        }
    }

    LaunchedEffect(messages.size, generating) {
        val count = messages.size + if (generating) 1 else 0
        if (count > 0) {
            scrollState.animateScrollToItem(count - 1)
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirmation = false
            },
            title = {
                Text("Clear conversation?")
            },
            text = {
                Text(
                    "This will permanently delete the current " +
                        "conversation from this device. " +
                        "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearConversation()
                    }
                ) {
                    Text("Clear chat")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jarvis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Your Kampala Life Assistant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (messages.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            showClearConfirmation = true
                        }
                    ) {
                        Text("Clear chat")
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 55.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "How can I help you?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Ask a question, explore an idea, " +
                                    "or discover Kampala.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                itemsIndexed(messages) { _, message ->
                    if (message.isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                shape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomEnd = 5.dp,
                                    bottomStart = 20.dp
                                ),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = message.text,
                                    modifier = Modifier.padding(15.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    text = "Jarvis",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                SelectionContainer {
                                    JarvisMarkdownContent(message.text)
                                }
                                TextButton(
                                    onClick = {
                                        clipboard.setText(
                                            buildAnnotatedString {
                                                append(message.text)
                                            }
                                        )
                                    }
                                ) {
                                    Text("Copy response")
                                }
                            }
                        }
                    }
                }

                if (generating) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Jarvis is thinking...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }

            if (showScrollToBottom) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            uiScope.launch {
                                val lastIndex =
                                    scrollState.layoutInfo.totalItemsCount - 1

                                if (lastIndex >= 0) {
                                    scrollState.animateScrollToItem(lastIndex)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Latest",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = 12.dp
                        ),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier.weight(1f),
                        enabled = !generating,
                        shape = RoundedCornerShape(28.dp),
                        placeholder = { Text("Message Jarvis...") },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (input.isNotBlank() && !generating) {
                                    onSend()
                                    keyboard?.hide()
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(
                                enabled = input.isNotBlank() && !generating,
                                onClick = {
                                    onSend()
                                    keyboard?.hide()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = if (input.isNotBlank() && !generating) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        },
                        maxLines = 5
                    )
                }
            }
        }
    }
}

/**
 * Presentation-only Markdown renderer for experimental Jarvis.
 * It does not modify model output, prompts, or directory results.
 */
@Composable
private fun JarvisMarkdownContent(source: String) {
    val clipboard = LocalClipboardManager.current
    val blocks = remember(source) { parseJarvisBlocks(source) }

    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        blocks.forEach { block ->
            when (block.kind) {
                "heading" -> {
                    Text(
                        text = jarvisInlineMarkdown(block.text),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                "list" -> {
                    Text(
                        text = jarvisInlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        )
                    )
                }

                "code" -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 12.dp,
                                        end = 6.dp,
                                        top = 4.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (block.language.isBlank()) {
                                        "Code"
                                    } else {
                                        block.language
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )

                                TextButton(
                                    onClick = {
                                        clipboard.setText(
                                            buildAnnotatedString {
                                                append(block.text)
                                            }
                                        )
                                    }
                                ) {
                                    Text("Copy code")
                                }
                            }

                            Text(
                                text = "Swipe sideways to view long lines",
                                modifier = Modifier.padding(
                                    horizontal = 12.dp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = block.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                else -> {
                    Text(
                        text = jarvisInlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        )
                    )
                }
            }
        }
    }
}

private data class JarvisMarkdownBlock(
    val kind: String,
    val text: String,
    val language: String = ""
)

private fun parseJarvisBlocks(source: String): List<JarvisMarkdownBlock> {
    val blocks = mutableListOf<JarvisMarkdownBlock>()
    val lines = source.replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')

    val paragraph = StringBuilder()
    val code = StringBuilder()
    var insideCode = false
    var codeLanguage = ""

    fun flushParagraph() {
        val value = paragraph.toString().trim()
        if (value.isNotEmpty()) {
            blocks.add(JarvisMarkdownBlock("paragraph", value))
        }
        paragraph.clear()
    }

    fun flushCode() {
        blocks.add(
            JarvisMarkdownBlock(
                "code",
                code.toString().trimEnd('\n'),
                codeLanguage
            )
        )
        code.clear()
        codeLanguage = ""
    }

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (insideCode) {
                flushCode()
                insideCode = false
            } else {
                flushParagraph()
                codeLanguage = trimmed.removePrefix("```").trim()
                insideCode = true
            }
            continue
        }

        if (insideCode) {
            code.append(line).append('\n')
            continue
        }

        if (trimmed.isBlank()) {
            flushParagraph()
            continue
        }

        val heading = Regex("""^#{1,6}\s+(.+)$""").matchEntire(trimmed)
        if (heading != null) {
            flushParagraph()
            blocks.add(
                JarvisMarkdownBlock(
                    "heading",
                    heading.groupValues[1]
                )
            )
            continue
        }

        val numbered = Regex("""^\d+[.)]\s+.+$""").matches(trimmed)
        val bulleted = Regex("""^[-+]\s+.+$""").matches(trimmed)

        if (numbered || bulleted) {
            flushParagraph()
            blocks.add(JarvisMarkdownBlock("list", trimmed))
            continue
        }

        if (paragraph.isNotEmpty()) paragraph.append(' ')
        paragraph.append(trimmed)
    }

    flushParagraph()

    if (insideCode) {
        flushCode()
    }

    return blocks
}

/**
 * Tolerant inline formatting.
 *
 * Supports balanced **bold**, *italic*, `code`, and ***combined***.
 * For model-generated headings with a single star left on each side,
 * renders the content as emphasis instead of showing stray markers.
 */
private fun jarvisInlineMarkdown(source: String): AnnotatedString =
    buildAnnotatedString {
        var cursor = 0

        while (cursor < source.length) {
            val delimiter = when {
                source.startsWith("***", cursor) -> "***"
                source.startsWith("**", cursor) -> "**"
                source.startsWith("`", cursor) -> "`"
                source.startsWith("*", cursor) -> "*"
                else -> null
            }

            if (delimiter == null) {
                append(source[cursor])
                cursor++
                continue
            }

            val start = cursor + delimiter.length
            val end = source.indexOf(delimiter, start)

            if (end <= start) {
                append(source[cursor])
                cursor++
                continue
            }

            val span = when (delimiter) {
                "***" -> SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
                "**" -> SpanStyle(fontWeight = FontWeight.Bold)
                "`" -> SpanStyle(fontFamily = FontFamily.Monospace)
                else -> SpanStyle(fontStyle = FontStyle.Italic)
            }

            withStyle(span) {
                append(source.substring(start, end))
            }

            cursor = end + delimiter.length
        }
    }
