package com.example.jarvis.ui

import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                            .align(Alignment.End)
                            .padding(
                                horizontal = 16.dp,
                                vertical = 4.dp
                            )
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
    val greyBackground = androidx.compose.ui.graphics.Color(0xFF191C21)
    val greyIcon = androidx.compose.ui.graphics.Color(0xFFD6D9DE)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = greyBackground,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(9.dp),
                color = androidx.compose.ui.graphics.Color(0xFF353940)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "J",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = greyIcon
                    )
                }
            }

            Text(
                text = "Jarvis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = greyIcon
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                androidx.compose.material3.MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        primary = greyIcon
                    )
                ) {
                    modelSelector()
                }
            }

            androidx.compose.material3.IconButton(
                onClick = onClearChat,
                enabled = canClearChat,
                modifier = Modifier.size(48.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(24.dp)
                ) {
                    val iconColor = if (canClearChat) {
                        greyIcon
                    } else {
                        greyIcon.copy(alpha = 0.4f)
                    }

                    val stroke = 1.8.dp.toPx()
                    val w = size.width
                    val h = size.height

                    fun line(
                        x1: Float, y1: Float,
                        x2: Float, y2: Float
                    ) {
                        drawLine(
                            color = iconColor,
                            start = androidx.compose.ui.geometry.Offset(x1, y1),
                            end = androidx.compose.ui.geometry.Offset(x2, y2),
                            strokeWidth = stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }

                    line(w * .22f, h * .28f, w * .78f, h * .28f)
                    line(w * .40f, h * .18f, w * .60f, h * .18f)
                    line(w * .29f, h * .34f, w * .34f, h * .82f)
                    line(w * .71f, h * .34f, w * .66f, h * .82f)
                    line(w * .34f, h * .82f, w * .66f, h * .82f)
                    line(w * .44f, h * .43f, w * .44f, h * .70f)
                    line(w * .56f, h * .43f, w * .56f, h * .70f)
                }
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
                color = androidx.compose.ui.graphics.Color(0xFF353A41),
                contentColor = androidx.compose.ui.graphics.Color(0xFFE4E7EB)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 10.dp
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
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF242A30)
                ) {
                    Box(
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 11.dp
                        )
                    ) {
                        JarvisLinkedAnswer(
                            messageText = message.text,
                            onLongClick = onLongClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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
                                text = if (
                                    JarvisGeoResultCardsModel.parse(message.text) != null
                                ) {
                                    "OpenStreetMap listings · Not independently verified"
                                } else {
                                    if (
                                        message.text.contains(
                                            "Open directions: https://www.google.com/maps/dir/?api=1"
                                        )
                                    ) {
                                        "Google Maps handoff · Confirm route"
                                    } else {
                                        // JARVIS-GEO-FOOTER-STATUS-V1
                                        if (message.text.startsWith("Named map search: ")) {
                                            "OSM map listing · Current details not verified"
                                        } else {
                                            com.example.jarvis.core.ai.KampalaPublishedPlacePresentationPolicy
                                                .directoryNoticeLabel(message.text)
                                        }
                                    }
                                },
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
    // JARVIS-NAMED-PLACE-CARDS-V1
    // Presentation only. Never alter retrieved place identities or coordinates.
    if (messageText.startsWith("Named map search: ")) {
        val namedLines = remember(messageText) {
            messageText.lines()
        }

        val namedPlaceStarts = remember(messageText) {
            namedLines.indices.filter { index ->
                Regex("""^\d+\.\s+\S+""").containsMatchIn(
                    namedLines[index].trim()
                )
            }
        }

        if (namedPlaceStarts.isNotEmpty()) {
            val namedUriHandler = LocalUriHandler.current

            val searchScope = namedLines.firstOrNull {
                it.startsWith("Search scope: ") ||
                    it.startsWith("Search area: ")
            }?.substringAfter(": ")?.trim().orEmpty()

            val searchTitle = namedLines.first()
                .substringAfter("Named map search: ")
                .trim()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Places matching $searchTitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (searchScope.isNotEmpty()) {
                    Text(
                        text = "Map search · $searchScope",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                namedPlaceStarts.forEachIndexed { index, start ->
                    val end = namedPlaceStarts.getOrNull(index + 1)
                        ?: namedLines.size

                    val section = namedLines.subList(start, end)

                    val name = section.first().trim()
                        .replaceFirst(Regex("""^\d+\.\s+"""), "")

                    fun field(label: String): String =
                        section.firstOrNull {
                            it.startsWith(label)
                        }?.removePrefix(label)?.trim().orEmpty()

                    val address = field("Listed address: ")
                    val category = field("Category: ")
                        .replace('_', ' ')
                    val coordinates = field("Map coordinates: ")
                    val editedAt = field("OSM last edited: ")

                    val mapsUrl = field("Open in Google Maps: ")
                    val sourceUrl = field("Source record: ")

                    fun trusted(url: String, host: String): Boolean =
                        runCatching {
                            val uri = java.net.URI(url)
                            uri.scheme == "https" &&
                                uri.host == host &&
                                uri.userInfo == null
                        }.getOrDefault(false)

                    val validMaps = trusted(
                        mapsUrl,
                        "www.google.com"
                    ) && mapsUrl.startsWith(
                        "https://www.google.com/maps/search/?api=1&query="
                    )

                    val validSource = trusted(
                        sourceUrl,
                        "www.openstreetmap.org"
                    ) && Regex(
                        """https://www\.openstreetmap\.org/(?:node|way|relation)/\d+"""
                    ).matches(sourceUrl)

                    var showDetails by remember(messageText, index) {
                        mutableStateOf(false)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (address.isNotBlank()) {
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (category.isNotBlank()) {
                                Text(
                                    text = category.replaceFirstChar {
                                        it.uppercase()
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (validMaps) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                namedUriHandler.openUri(mapsUrl)
                                            }
                                        }
                                    ) {
                                        Text("Open Maps")
                                    }
                                }

                                if (validSource) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                namedUriHandler.openUri(sourceUrl)
                                            }
                                        }
                                    ) {
                                        Text("OSM source")
                                    }
                                }
                            }

                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showDetails = !showDetails
                                }
                            ) {
                                Text(
                                    if (showDetails) "Hide map details"
                                    else "Map details"
                                )
                            }

                            if (showDetails) {
                                if (coordinates.isNotBlank()) {
                                    Text(
                                        text = "Coordinates: $coordinates",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (editedAt.isNotBlank()) {
                                    Text(
                                        text = "OSM record edited: " +
                                            editedAt.take(10),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Text(
                                    text = "Map listing only. Current opening " +
                                        "status and navigation entrances " +
                                        "have not been verified.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Map data: OpenStreetMap contributors (ODbL). " +
                        "Listings may be outdated.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            return
        }
    }
    // P1-B: Render only successfully parsed geographic results as cards.
    // Other messages retain the established linked-answer renderer.
    // JARVIS-MULTI-AREA-CARDS-V1
    // Recognize only Jarvis's structured multi-area responses.
    // Never interpret ordinary AI text as map results.
    if (messageText.trimStart().startsWith(
            "Kampala multi-area map discovery"
        )
    ) {
        val uriHandler = LocalUriHandler.current

        val sections = remember(messageText) {
            messageText.split(
                Regex("""(?m)^\s*---\s*$""")
            )
        }

        fun trustedUrl(
            value: String,
            allowedHost: String
        ): String? = runCatching {
            val uri = java.net.URI(value.trim())

            if (
                uri.scheme == "https" &&
                uri.host == allowedHost &&
                uri.userInfo == null &&
                uri.fragment == null
            ) {
                value.trim()
            } else {
                null
            }
        }.getOrNull()

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Kampala places",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            for (section in sections) {
                val category = section.lineSequence()
                    .firstOrNull {
                        it.startsWith("Category: ")
                    }
                    ?.removePrefix("Category: ")
                    ?.trim()
                    .orEmpty()

                if (category.isNotEmpty()) {
                    Text(
                        text = category.replaceFirstChar {
                            it.uppercase()
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val headings = Regex(
                    """(?m)^(\d+)\.\s+(.+)$"""
                ).findAll(section).toList()

                if (headings.isEmpty()) {
                    Text(
                        text = "No place listings were returned " +
                            "for this category.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                for ((position, heading) in headings.withIndex()) {
                    val nextStart =
                        headings.getOrNull(position + 1)
                            ?.range?.first
                            ?: section.length

                    val details = section.substring(
                        heading.range.last + 1,
                        nextStart
                    )

                    fun detail(prefix: String): String? =
                        details.lineSequence()
                            .map { it.trim() }
                            .firstOrNull {
                                it.startsWith(prefix)
                            }
                            ?.removePrefix(prefix)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }

                    val name = heading.groupValues[2].trim()
                    val searchArea = detail("Search area:")
                    val address = detail("Listed address:")

                    // JARVIS-GEO-SNAPSHOT-CARD-V1
                    val snapshotGenerated =
                        detail("Snapshot generated:")

                    // JARVIS-OSM-RECORD-DATES-V1
                    val osmLastEdited =
                        detail("OSM last edited:")

                    // Hide generic category/area labels, not
                    // legitimately named businesses.
                    val normalizedName = name
                        .lowercase(java.util.Locale.ROOT)
                        .replace(Regex("""\s+"""), " ")
                        .trim()

                    val normalizedCategory = category
                        .lowercase(java.util.Locale.ROOT)
                        .trim()

                    val normalizedArea = searchArea
                        ?.lowercase(java.util.Locale.ROOT)
                        ?.trim()

                    val genericCategoryNames = setOf(
                        "hospital", "hospitals",
                        "school", "schools",
                        "bank", "banks",
                        "hotel", "hotels",
                        "clinic", "clinics",
                        "pharmacy", "pharmacies",
                        "university", "universities",
                        "embassy", "embassies"
                    )

                    if (
                        normalizedName.isBlank() ||
                        (
                            normalizedName in genericCategoryNames &&
                            (
                                normalizedName == normalizedCategory ||
                                normalizedName.removeSuffix("s") ==
                                    normalizedCategory.removeSuffix("s")
                            )
                        ) ||
                        (
                            normalizedArea != null &&
                            normalizedName == normalizedArea
                        )
                    ) {
                        continue
                    }

                    // JARVIS-GEO-CARD-ACTIONS-V2
                    // The provider formatter writes label and URL
                    // on the same line. Extract that labeled URL.
                    val googleMaps = Regex(
                        """(?m)^\s*Open in Google Maps:\s*(https://[^\s]+)\s*$"""
                    ).find(details)
                        ?.groupValues?.get(1)
                        ?.let {
                            trustedUrl(it, "www.google.com")
                        }
                        ?.takeIf { url ->
                            runCatching {
                                val uri = java.net.URI(url)
                                uri.path == "/maps/search/" &&
                                    uri.rawQuery
                                        ?.startsWith("api=1&query=") == true
                            }.getOrDefault(false)
                        }

                    val source = Regex(
                        """(?m)^\s*Source record:\s*(https://[^\s]+)\s*$"""
                    ).find(details)
                        ?.groupValues?.get(1)
                        ?.let {
                            trustedUrl(
                                it,
                                "www.openstreetmap.org"
                            )
                        }
                        ?.takeIf { url ->
                            runCatching {
                                java.net.URI(url).path.matches(
                                    Regex(
                                        """/(?:node|way|relation)/\d+"""
                                    )
                                )
                            }.getOrDefault(false)
                        }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme
                            .surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (!searchArea.isNullOrBlank()) {
                                Text(
                                    text = searchArea,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                )
                            }

                            if (!address.isNullOrBlank()) {
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (!snapshotGenerated.isNullOrBlank()) {
                                Text(
                                    text = "Map snapshot: " +
                                        snapshotGenerated.take(10),
                                    style =
                                        MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                )
                            }

                            if (!osmLastEdited.isNullOrBlank()) {
                                Text(
                                    text = "OSM record last edited: " +
                                        osmLastEdited.take(10),
                                    style =
                                        MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                )
                            }

                            if (googleMaps != null) {
                                androidx.compose.material3.Button(
                                    onClick = {
                                        runCatching {
                                            uriHandler.openUri(googleMaps)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open in Google Maps")
                                }
                            }

                            if (source != null) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        runCatching {
                                            uriHandler.openUri(source)
                                        }
                                    }
                                ) {
                                    Text("View map-data source")
                                }
                            }
                        }
                    }
                }

                val coverage = section.lineSequence()
                    .map { it.trim() }
                    .filter {
                        it.startsWith("Unavailable searches:") ||
                            it.startsWith("Incomplete coverage:") ||
                            it.startsWith("Additional listings")
                    }
                    .toList()

                for (warning in coverage) {
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Public map-data listings. " +
                    "A map snapshot date is not the date an individual " +
                    "place was last edited. Current business operation " +
                    "is unverified. Addresses, opening hours and " +
                    "routes require confirmation. Coverage remains limited.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        return
    }
    val geoResult = remember(messageText) {
        JarvisGeoResultCardsModel.parse(messageText)
    }

    if (geoResult != null) {
        JarvisGeoResultCards(geoResult)
        return
    }

    // M1F-I3C.119J: compact institutional source references.
    val uriHandler = LocalUriHandler.current

    // CDI.6.0-A6.8-B2:
    // Convert only a validated, Jarvis-generated Maps handoff
    // into a user-controlled action.
    // Keep institutional source formatting unchanged.
    val mapsAction = remember(messageText) {
        JarvisMapsActionPresentation.parseOrNull(messageText)
    }

    if (mapsAction != null) {
        androidx.compose.foundation.layout.Column {
            JarvisLinkedAnswer(
                messageText = mapsAction.visibleText,
                onLongClick = onLongClick
            )

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(10.dp)
            )

            androidx.compose.material3.OutlinedButton(
                onClick = {
                    runCatching {
                        uriHandler.openUri(mapsAction.url)
                    }
                }
            ) {
                androidx.compose.material3.Text(
                    text = mapsAction.label
                )
            }
        }

        return
    }

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

        val providerSuggestion = remember(messageText) {
            messageText.lineSequence()
                .firstOrNull {
                    it.startsWith("Provider suggestion: ")
                }
                ?.removePrefix("Provider suggestion: ")
                ?.trim()
                .orEmpty()
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            if (providerSuggestion.isNotBlank()) {
                Text(
                    text = "Place lookup: $providerSuggestion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Check both locations and the route in Google Maps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    try {
                        uriHandler.openUri(directionsUrl)
                    } catch (_: Exception) {
                        // No compatible maps application or browser available.
                    }
                }
            ) {
                Text("Open in Google Maps")
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val composerGrey = androidx.compose.ui.graphics.Color(0xFF292D33)
    val micGrey = androidx.compose.ui.graphics.Color(0xFF353A41)
    val softWhite = androidx.compose.ui.graphics.Color(0xFFE3E6EA)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color(0xFF191C21),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(27.dp),
                color = composerGrey
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = text,
                        onValueChange = onTextChanged,
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = softWhite
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(
                            softWhite
                        ),
                        minLines = 1,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (enabled && text.isNotBlank()) {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onSendClicked()
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (text.isEmpty()) {
                                    Text(
                                        text = "Message Jarvis...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = androidx.compose.ui.graphics.Color(
                                            0xFFAEB5BE
                                        ),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    androidx.compose.material3.IconButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onSendClicked()
                        },
                        enabled = enabled && text.isNotBlank(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(50),
                            color = if (enabled && text.isNotBlank()) {
                                softWhite
                            } else {
                                androidx.compose.ui.graphics.Color(0xFF454B53)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (enabled && text.isNotBlank()) {
                                        composerGrey
                                    } else {
                                        androidx.compose.ui.graphics.Color(
                                            0xFF9BA2AA
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            FilledTonalIconButton(
                onClick = onMicClicked,
                enabled = enabled,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isListening) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        micGrey
                    },
                    contentColor = if (isListening) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        softWhite
                    }
                )
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
