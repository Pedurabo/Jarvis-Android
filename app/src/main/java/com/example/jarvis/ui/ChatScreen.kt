package com.example.jarvis.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jarvis.core.ai.ConfidenceLevel

@Composable
fun ChatScreen(viewModel: JarvisViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingAction by viewModel.pendingToolAction.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()
    val llmError by viewModel.llmError.collectAsState()
    val isProcessing = uiState.isProcessing
    val listState = rememberLazyListState()

    var showFeedbackDialog by remember { mutableStateOf<Message?>(null) }

    // Check if keyboard is visible to scroll to bottom
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    LaunchedEffect(uiState.messages.size, isKeyboardVisible) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    if (showFeedbackDialog != null) {
        FeedbackDialog(
            failedResponse = showFeedbackDialog!!.text,
            onDismiss = { showFeedbackDialog = null },
            onSubmitFeedback = { correction ->
                val index = uiState.messages.indexOf(showFeedbackDialog)
                if (index > 0) {
                    val userQuery = uiState.messages[index - 1].text
                    viewModel.submitFeedback(userQuery, showFeedbackDialog!!.text, correction)
                }
                showFeedbackDialog = null
            }
        )
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { viewModel.rejectPendingToolAction() },
            title = { Text("Approve Jarvis action?") },
            text = {
                Column {
                    Text("Tool: ${action.action}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Exact parameters: ${action.parametersJson}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Only approve if you requested this operation.")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.approvePendingToolAction() }) { Text("Approve once") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.rejectPendingToolAction() }) { Text("Reject") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                if (llmError != null) {
                    ErrorMessageBar(llmError!!)
                } else if (!isModelReady) {
                    ModelLoadingBar()
                } else {
                    ChatInputArea(
                        text = uiState.currentInputText,
                        onTextChanged = { viewModel.onTextChanged(it) },
                        onSendClicked = { viewModel.sendTextMessage() },
                        isListening = isListening,
                        onMicClicked = { viewModel.toggleListening() }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ChatHeader()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item { WelcomeMessage(onSuggestionClick = { viewModel.onTextChanged(it) }) }
                }

                items(uiState.messages) { message ->
                    ChatBubble(
                        message = message,
                        onLongClick = {
                            if (!message.isUser) {
                                showFeedbackDialog = message
                            }
                        }
                    )
                }

                if (isProcessing) {
                    item {
                        TypingIndicator(uiState.processingStatus ?: "Jarvis is thinking...")
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    failedResponse: String,
    onDismiss: () -> Unit,
    onSubmitFeedback: (String) -> Unit
) {
    var correctionText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Help Jarvis Improve",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Was this response incorrect? Tell me what the right answer should have been.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = correctionText,
                    onValueChange = { correctionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correct information") },
                    placeholder = { Text("e.g. Actually, market prices are...") },
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmitFeedback(correctionText) },
                        enabled = correctionText.isNotBlank()
                    ) {
                        Text("Save Correction")
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorMessageBar(error: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun WelcomeMessage(onSuggestionClick: (String) -> Unit) {
    // M1F-I3C.119C: practical Kampala-focused empty state.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Explore Kampala with Jarvis",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Discover places, explore the city's history, and find useful local information.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "TRY ASKING",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        val suggestions = listOf(
            "Tell me about Kampala history",
            "Find shopping places in Kisementi",
            "Find a mosque in Old Kampala"
        )

        suggestions.forEach { suggestion ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { onSuggestionClick(suggestion) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 15.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = suggestion,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "↗",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Text(
            text = "Tap a suggestion to add it to your message.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ModelLoadingBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Initializing local brain...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ChatHeader() {
    // M1F-I3C.119C: compact conversational identity.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "J",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column {
                Text(
                    text = "Jarvis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your Kampala assistant",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message, onLongClick: () -> Unit = {}) {
    // Modern Jarvis conversation layout:
    // compact user messages and open, readable assistant responses.
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            Surface(
                modifier = Modifier.widthIn(max = 304.dp),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomEnd = 6.dp,
                    bottomStart = 20.dp
                ),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(onLongClick) {
                        detectTapGestures(
                            onLongPress = { onLongClick() }
                        )
                    }
                    .padding(vertical = 4.dp)
            ) {
                // M1F-I3C.119N.2: compact assistant identity.
                Text(
                    text = "Jarvis",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                JarvisLinkedAnswer(
                    messageText = message.text,
                    onLongClick = onLongClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (message.origin) {
                        MessageOrigin.KNOWLEDGE_RESULT -> {
                            Text(
                                text = "Knowledge Result",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        MessageOrigin.KNOWLEDGE_NOTICE -> {
                            Text(
                                text = "Knowledge Notice",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        MessageOrigin.DIRECTORY_RESULT -> {
                            Text(
                                text = "Directory Result",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        MessageOrigin.DIRECTORY_NOTICE -> {
                            Text(
                                text = "Directory Notice",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        MessageOrigin.AI_RESPONSE -> {
                            ConfidenceIndicator(message.confidence)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Long press to correct",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun JarvisLinkedAnswer(
    messageText: String,
    onLongClick: () -> Unit
) {
    // M1F-I3C.119J: compact institutional source references.
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary

    val urlPattern = remember {
        Regex("""https://[^\s<>()]+""")
    }

    val sourceLinks = remember(messageText) {
        urlPattern.findAll(messageText)
            .map { match ->
                match.value.trimEnd(
                    '.', ',', ';', ':', '!', '?', ')', ']'
                )
            }
            .filter { candidate ->
                try {
                    val uri = java.net.URI(candidate)
                    uri.scheme.equals("https", ignoreCase = true) &&
                        !uri.host.isNullOrBlank() &&
                        uri.userInfo == null
                } catch (_: Exception) {
                    false
                }
            }
            .distinct()
            .map { url ->
                val host = java.net.URI(url).host
                    ?.lowercase()
                    ?.removePrefix("www.")
                    .orEmpty()

                val title = when (host) {
                    "kcca.go.ug" -> "KCCA"
                    "loc.gov" -> "Library of Congress"
                    "vicepresident.go.ug" -> "Government of Uganda"
                    else -> host.ifBlank { "Original source" }
                }

                url to title
            }
            .toList()
    }

    // Remove only recognizable source-reference lines.
    // Leave ordinary answer paragraphs and review dates intact.
    val visibleAnswer = remember(messageText, sourceLinks) {
        if (sourceLinks.isEmpty()) {
            messageText
        } else {
            val sourceNames = setOf(
                "",
                "source",
                "sources",
                "kcca",
                "kampala capital city authority",
                "library of congress",
                "government of uganda",
                "office of the vice president",
                "office of vice president",
                "office of the vice president of uganda"
            )

            messageText.lines()
                .filterNot { line ->
                    val trimmed = line.trim()

                    // M1F-I3C.120J: the compact chip already opens
                    // this exact source. Avoid displaying its raw URL
                    // a second time in directory answers.
                    val duplicateSourceLink =
                        trimmed.startsWith(
                            "Source link:",
                            ignoreCase = true
                        ) &&
                            sourceLinks.any { (url, _) ->
                                trimmed.substringAfter(":").trim() == url
                            }

                    if (duplicateSourceLink) {
                        true
                    } else if (
                        trimmed.equals("Sources:", ignoreCase = true) ||
                        trimmed.equals("Sources", ignoreCase = true)
                    ) {
                        true
                    } else {
                        val hasUrl = sourceLinks.any { (url, _) ->
                            line.contains(url)
                        }

                        if (!hasUrl) {
                            false
                        } else {
                            val withoutUrls = sourceLinks.fold(line) {
                                current, link ->
                                current.replace(link.first, "")
                            }

                            val remaining = withoutUrls
                                .trim()
                                .trim(
                                    ' ', '-', '*', ':', '•', '—',
                                    '1', '2', '3', '.', ')', ']'
                                )
                                .trim()
                                .lowercase()
                                .removePrefix("source: ")
                                .trim()

                            remaining in sourceNames
                        }
                    }
                }
                .joinToString("\n")
                .trim()
        }
    }

    // M1F-I3C.119M: separate review metadata from answer text.
    val reviewLine = remember(visibleAnswer) {
        visibleAnswer.lines()
            .firstOrNull { line ->
                val clean = line.trim()
                clean.startsWith("Knowledge reviewed:", ignoreCase = true) ||
                    clean.startsWith("Source checked:", ignoreCase = true)
            }
            ?.trim()
    }

    val answerBody = remember(visibleAnswer, reviewLine) {
        if (reviewLine == null) {
            visibleAnswer
        } else {
            visibleAnswer.lines()
                .filterNot { it.trim() == reviewLine }
                .joinToString("\n")
                .trim()
        }
    }

    // M1F-I3C.119N.2: promote the verified history heading.
    // All other answer types retain their existing formatting.
    val firstAnswerLine = answerBody.lineSequence()
        .firstOrNull()
        ?.trim()
        .orEmpty()

    val hasHistoryHeading = firstAnswerLine
        .removePrefix("#")
        .trim()
        .removeSurrounding("**")
        .trim()
        .removeSuffix(":")
        .equals("Kampala History", ignoreCase = true)

    val headingTitle = when {
        hasHistoryHeading -> "Kampala History"
        firstAnswerLine.equals(
            "Kampala Places Directory",
            ignoreCase = true
        ) -> "Kampala Places Directory"
        else -> null
    }

    val displayBody = remember(answerBody, headingTitle) {
        if (headingTitle != null) {
            answerBody.substringAfter("\n", "").trimStart()
        } else {
            answerBody
        }
    }

    val formatted = remember(displayBody) {
        JarvisMarkdownFormatter.format(displayBody)
    }

    val linkedText = remember(formatted, linkColor, headingTitle) {
        buildAnnotatedString {
            append(formatted)

            if (headingTitle == "Kampala Places Directory") {
                // M1F-I3C.120J: emphasize the first result name.
                val firstLineEnd = formatted.text
                    .indexOf('\n')
                    .let { index ->
                        if (index >= 0) index else formatted.text.length
                    }

                if (firstLineEnd > 0) {
                    addStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.SemiBold
                        ),
                        start = 0,
                        end = firstLineEnd
                    )
                }
            }

            urlPattern.findAll(formatted.text).forEach { match ->
                val candidate = match.value.trimEnd(
                    '.', ',', ';', ':', '!', '?', ')', ']'
                )

                val valid = try {
                    val uri = java.net.URI(candidate)

                    uri.scheme.equals("https", ignoreCase = true) &&
                        !uri.host.isNullOrBlank() &&
                        uri.userInfo == null
                } catch (_: Exception) {
                    false
                }

                if (valid) {
                    val start = match.range.first
                    val end = start + candidate.length

                    addStringAnnotation(
                        tag = "JARVIS_HTTPS_SOURCE",
                        annotation = candidate,
                        start = start,
                        end = end
                    )

                    addStyle(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    var textLayout by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (headingTitle != null) {
            Text(
                text = headingTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (displayBody.isNotBlank()) {
            Text(
                text = linkedText,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(linkedText, uriHandler, onLongClick) {
                        detectTapGestures(
                            onTap = { position ->
                                val layout = textLayout

                                if (layout != null) {
                                    val index =
                                        layout.getOffsetForPosition(position)

                                    val match =
                                        linkedText.getStringAnnotations(
                                            tag = "JARVIS_HTTPS_SOURCE",
                                            start = index,
                                            end = index
                                        ).firstOrNull()

                                    if (match != null) {
                                        try {
                                            uriHandler.openUri(match.item)
                                        } catch (_: Exception) {
                                            // No browser available.
                                        }
                                    }
                                }
                            },
                            onLongPress = { onLongClick() }
                        )
                    },
                onTextLayout = { textLayout = it },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                )
            )
        }

        if (reviewLine != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = reviewLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sourceLinks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SOURCES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sourceLinks.forEach { (url, title) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            try {
                                uriHandler.openUri(url)
                            } catch (_: Exception) {
                                // No browser available.
                            }
                        }
                    ) {
                        Text(
                            text = "↗  $title",
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 9.dp
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = linkColor
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun ConfidenceIndicator(level: ConfidenceLevel) {
    val (color, icon, label) = when (level) {
        ConfidenceLevel.HIGH -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "High Confidence")
        ConfidenceLevel.MEDIUM -> Triple(Color(0xFFFF9800), Icons.Default.Info, "Medium Confidence")
        ConfidenceLevel.LOW -> Triple(Color(0xFFF44336), Icons.AutoMirrored.Filled.Help, "Low Confidence")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TypingIndicator(status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun ChatInputArea(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    isListening: Boolean,
    onMicClicked: () -> Unit
) {
    // M1F-I3C.119C: compact expandable composer, with voice preserved.
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Ask about Kampala...",
                        maxLines = 1
                    )
                },
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) onSendClicked()
                    }
                ),
                trailingIcon = {
                    if (text.isNotBlank()) {
                        IconButton(onClick = onSendClicked) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )

            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onMicClicked() },
                shape = CircleShape,
                color = if (isListening)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                contentColor = if (isListening)
                    MaterialTheme.colorScheme.onError
                else
                    MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isListening)
                            Icons.Default.MicOff
                        else
                            Icons.Default.Mic,
                        contentDescription = if (isListening)
                            "Stop listening"
                        else
                            "Start listening",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
