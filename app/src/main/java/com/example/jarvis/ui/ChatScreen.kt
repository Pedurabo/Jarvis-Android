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
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: JarvisViewModel = hiltViewModel(),
    modelSelector: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingAction by viewModel.pendingToolAction.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()
    val llmError by viewModel.llmError.collectAsState()
    val isProcessing = uiState.isProcessing
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showFeedbackDialog by remember { mutableStateOf<Message?>(null) }
    var showClearChatDialog by remember { mutableStateOf(false) }

    // Follow new messages only when the reader is near the bottom.
    // Opening the keyboard must not interrupt older-message reading.
    var previousMessageCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.messages.size) {
        val currentCount = uiState.messages.size
        val previousCount = previousMessageCount

        if (currentCount > previousCount) {
            val lastVisibleIndex =
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            val shouldFollow =
                previousCount == 0 ||
                lastVisibleIndex >= previousCount - 2

            previousMessageCount = currentCount

            if (shouldFollow && currentCount > 0) {
                listState.animateScrollToItem(currentCount - 1)
            }
        } else {
            previousMessageCount = currentCount
        }
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear chat?") },
            text = {
                Text(
                    "Delete the current conversation from this device? " +
                        "This cannot be undone. Historical cloud copies " +
                        "are not deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatDialog = false
                        showFeedbackDialog = null
                        viewModel.clearChat()
                    }
                ) {
                    Text(
                        "Clear chat",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearChatDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
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
                }

                ChatInputArea(
                    text = uiState.currentInputText,
                    onTextChanged = { viewModel.onTextChanged(it) },
                    onSendClicked = { viewModel.sendTextMessage() },
                    isListening = isListening,
                    onMicClicked = { viewModel.toggleListening() },
                    enabled = isModelReady && llmError == null
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ChatHeader(
                modelSelector = modelSelector,
                onClearChat = { showClearChatDialog = true },
                canClearChat = uiState.messages.isNotEmpty() &&
                    !uiState.isClearingChat
            )

            if (uiState.clearChatError != null) {
                Text(
                    text = uiState.clearChatError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

                val showJumpToLatest by remember {
                    derivedStateOf {
                        val total = listState.layoutInfo.totalItemsCount
                        val lastVisible =
                            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                ?: 0
                        total > 0 && lastVisible < total - 2
                    }
                }

                if (showJumpToLatest) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val lastIndex =
                                    listState.layoutInfo.totalItemsCount - 1
                                if (lastIndex >= 0) {
                                    listState.animateScrollToItem(lastIndex)
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Text("↓  Latest")
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
fun ChatHeader(
    modelSelector: @Composable () -> Unit = {},
    onClearChat: () -> Unit = {},
    canClearChat: Boolean = false
) {
    // Unified identity and model controls in one header surface.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
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

            Column(modifier = Modifier.weight(1f)) {
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

            TextButton(
                onClick = onClearChat,
                enabled = canClearChat
            ) {
                Text("Clear chat")
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
        modelSelector()
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
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Box(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        )
                    ) {
                        JarvisLinkedAnswer(
                            messageText = message.text,
                            onLongClick = onLongClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (message.origin) {
                        MessageOrigin.CURATED_FACT -> {
                            Text(
                                text = "Curated Fact",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        MessageOrigin.KNOWLEDGE_RESULT -> {
                            Text(
                                text = "Knowledge source · Check review date and references",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        MessageOrigin.KNOWLEDGE_NOTICE -> {
                            Text(
                                text = "Verified knowledge unavailable",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        MessageOrigin.DIRECTORY_RESULT -> {
                            Text(
                                text = "Directory lookup · Confirm current details",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        MessageOrigin.DIRECTORY_NOTICE -> {
                            Text(
                                text = "Directory information unavailable or incomplete",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        MessageOrigin.AI_RESPONSE -> {
                            Column {
                                ConfidenceIndicator(message.confidence)
                                Text(
                                    text = "AI-generated · Verify current information",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

    // CDI.5.52-E1: Recognize only Jarvis-generated Google Maps
    // directions handoffs. Never treat arbitrary links as journeys.
    val journey = remember(messageText) {
        runCatching {
            val prefix = "Open directions: "
            val link = messageText.lineSequence()
                .firstOrNull { it.startsWith(prefix) }
                ?.removePrefix(prefix)
                ?.trim()
                ?: return@runCatching null

            val uri = java.net.URI(link)

            if (
                uri.scheme != "https" ||
                uri.host != "www.google.com" ||
                uri.path != "/maps/dir/" ||
                uri.userInfo != null
            ) {
                return@runCatching null
            }

            val parameters = uri.rawQuery
                ?.split("&")
                ?.mapNotNull { part ->
                    val pieces = part.split("=", limit = 2)
                    if (pieces.size == 2)
                        pieces[0] to pieces[1]
                    else null
                }
                ?.toMap()
                ?: return@runCatching null

            if (parameters["api"] != "1") {
                return@runCatching null
            }

            val origin = java.net.URLDecoder.decode(
                parameters["origin"] ?: return@runCatching null,
                "UTF-8"
            ).removeSuffix(", Kampala, Uganda")

            val destination = java.net.URLDecoder.decode(
                parameters["destination"] ?: return@runCatching null,
                "UTF-8"
            ).removeSuffix(", Kampala, Uganda")

            if (
                origin.isBlank() ||
                destination.isBlank() ||
                origin.length > 80 ||
                destination.length > 80
            ) {
                return@runCatching null
            }

            Triple(origin, destination, link)
        }.getOrNull()
    }

    if (journey != null) {
        val (origin, destination, directionsUrl) = journey

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Your journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FROM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = origin,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = destination,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Text(
                text = "Jarvis has not verified this route. " +
                    "Confirm the locations and directions in Google Maps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            OutlinedButton(
                onClick = {
                    try {
                        uriHandler.openUri(directionsUrl)
                    } catch (_: Exception) {
                        // No compatible maps application or browser available.
                    }
                }
            ) {
                Text("↗  Open directions")
            }
        }

        return
    }

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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Jarvis is working",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onMicClicked: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Message Jarvis...")
                },
                minLines = 1,
                maxLines = 5,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (enabled && text.isNotBlank()) onSendClicked()
                    }
                )
            )

            FilledIconButton(
                onClick = onSendClicked,
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message"
                )
            }

            FilledTonalIconButton(
                onClick = onMicClicked,
                enabled = enabled,
                modifier = Modifier.size(48.dp),
                colors = if (isListening) {
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    IconButtonDefaults.filledTonalIconButtonColors()
                }
            ) {
                Icon(
                    imageVector = if (isListening)
                        Icons.Default.MicOff
                    else
                        Icons.Default.Mic,
                    contentDescription = if (isListening)
                        "Stop listening"
                    else
                        "Start listening"
                )
            }
        }
        }
    }
}
