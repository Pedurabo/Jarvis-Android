package com.example.jarvis.ui

import androidx.compose.ui.text.AnnotatedString

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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item { WelcomeMessage() }
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
fun WelcomeMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hello, I am Jarvis.",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your on-device assistant for Kampala.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Jarvis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Push-to-Talk Mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message, onLongClick: () -> Unit = {}) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.secondaryContainer

    val contentColor = if (message.isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    val shape = if (message.isUser)
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    else
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            shape = shape,
            tonalElevation = 2.dp,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() }
                )
            }
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = if (message.isUser) AnnotatedString(message.text) else JarvisMarkdownFormatter.format(message.text),
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (!message.isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    when (message.origin) {
                        MessageOrigin.DIRECTORY_RESULT -> {
                            Text(
                                text = "Directory Result",
                                color = contentColor,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        MessageOrigin.DIRECTORY_NOTICE -> {
                            Text(
                                text = "Directory Notice",
                                color = contentColor,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        MessageOrigin.AI_RESPONSE -> {
                            ConfidenceIndicator(message.confidence)
                        }
                    }
                }
            }
        }

        if (!message.isUser) {
            Text(
                text = "Long press to correct",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
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
    Surface(
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Ask Jarvis anything...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClicked() }),
                trailingIcon = {
                    if (text.isNotBlank()) {
                        IconButton(onClick = onSendClicked) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = onMicClicked,
                shape = CircleShape,
                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mic"
                )
            }
        }
    }
}
