package com.example.jarvis

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    var selectedGemma by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        onGemmaChanged(false)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = !selectedGemma,
                onClick = {
                    selectedGemma = false
                    onGemmaChanged(false)
                },
                label = { Text("Llama 3.2 1B") }
            )

            FilterChip(
                selected = selectedGemma,
                onClick = {
                    selectedGemma = true
                    onGemmaChanged(true)
                },
                label = { Text("Gemma 4 E2B") }
            )
        }

        ChatScreen()
    }
}