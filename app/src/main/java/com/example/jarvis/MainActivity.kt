package com.example.jarvis

import android.Manifest
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.saveable.rememberSaveable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.jarvis.core.ai.ModelRouter
import com.example.jarvis.ui.ChatScreen
import com.example.jarvis.ui.theme.JarvisTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var modelRouter: ModelRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JarvisTheme {
                MainContent(
                    onGemmaChanged = { enabled ->
                        modelRouter.setGemmaEnabledForTesting(enabled)
                    }
                )
            }
        }
    }
}

@Composable
fun MainContent(
    onGemmaChanged: (Boolean) -> Unit
) {
    var selectedGemma by rememberSaveable { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
    }

    LaunchedEffect(selectedGemma) {
        onGemmaChanged(selectedGemma)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    var selectorExpanded by remember { mutableStateOf(false) }

    ChatScreen(modelSelector = {
            Box {
                androidx.compose.material3.TextButton(
                    onClick = { selectorExpanded = true }
                ) {
                    Text(
                        text = if (selectedGemma) "Gemma 4" else "Llama 3.2",
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }

                DropdownMenu(
                    expanded = selectorExpanded,
                    onDismissRequest = {
                        selectorExpanded = false
                    }
                ) {
                    DropdownMenuItem(
                        text = { Text("Llama 3.2 1B") },
                        onClick = {
                            selectorExpanded = false
                            selectedGemma = false
                            onGemmaChanged(false)
                        },
                        leadingIcon = {
                            if (!selectedGemma) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Gemma 4 E2B") },
                        onClick = {
                            selectorExpanded = false
                            selectedGemma = true
                            onGemmaChanged(true)
                        },
                        leadingIcon = {
                            if (selectedGemma) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
    })
}