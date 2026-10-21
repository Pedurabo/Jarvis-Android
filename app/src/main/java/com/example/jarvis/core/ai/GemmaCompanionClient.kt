package com.example.jarvis.core.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.gemmaruntimelab.ipc.ICompanionProbe
import com.example.gemmaruntimelab.ipc.IInferenceCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Experimental Jarvis-to-Gemma IPC adapter.
 *
 * Does not select Gemma automatically.
 * Returns null on transport, initialization, or generation failure.
 *
 * A new binding is created per request. This is intentionally
 * conservative for initial integration testing, not final lifecycle design.
 */
@Singleton
class GemmaCompanionClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val COMPANION_PACKAGE =
            "com.example.gemmacompanion"

        private const val COMPANION_SERVICE =
            "com.example.gemmaruntimelab.CompanionProbeService"

        private const val REQUEST_TIMEOUT_MS = 90_000L
    }

    suspend fun generate(prompt: String): String? {
        if (prompt.isBlank() || prompt.length > 8000) {
            return null
        }

        return try {
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                withContext(Dispatchers.Main.immediate) {
                    requestFromCompanion(prompt)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun requestFromCompanion(
        prompt: String
    ): String? = suspendCancellableCoroutine { continuation ->

        var connection: ServiceConnection? = null
        var bound = false
        var finished = false

        fun cleanup() {
            if (bound) {
                bound = false
                try {
                    connection?.let { context.unbindService(it) }
                } catch (_: Exception) {
                }
            }
        }

        fun finish(value: String?) {
            if (finished) return
            finished = true
            cleanup()

            if (continuation.isActive) {
                continuation.resume(value)
            }
        }

        fun onMain(action: () -> Unit) {
            mainHandler.post {
                if (!finished && continuation.isActive) {
                    action()
                }
            }
        }

        continuation.invokeOnCancellation {
            mainHandler.post {
                finished = true
                cleanup()
            }
        }

        val serviceConnection = object : ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName,
                binder: IBinder
            ) {
                val remote = ICompanionProbe.Stub.asInterface(binder)

                try {
                    val readiness = remote.getModelStatus()

                    if (readiness != "MODEL_FILE_READY") {
                        finish(null)
                        return
                    }

                    remote.initializeModel(
                        object : IInferenceCallback.Stub() {
                            override fun onResult(result: String?) {
                                onMain {
                                    val initialized =
                                        result?.startsWith(
                                            "MODEL_INITIALIZED:"
                                        ) == true ||
                                        result == "MODEL_ALREADY_INITIALIZED"

                                    if (!initialized) {
                                        finish(null)
                                        return@onMain
                                    }

                                    try {
                                        remote.generateResponse(
                                            prompt,
                                            object : IInferenceCallback.Stub() {
                                                override fun onResult(
                                                    result: String?
                                                ) {
                                                    onMain {
                                                        val answer =
                                                            result
                                                                ?.takeIf {
                                                                    it.startsWith(
                                                                        "RESPONSE:"
                                                                    )
                                                                }
                                                                ?.substringAfter('\n', "")
                                                                ?.trim()
                                                                ?.takeIf {
                                                                    it.isNotEmpty()
                                                                }

                                                        finish(answer)
                                                    }
                                                }
                                            }
                                        )
                                    } catch (_: Exception) {
                                        finish(null)
                                    }
                                }
                            }
                        }
                    )
                } catch (_: Exception) {
                    finish(null)
                }
            }

            override fun onServiceDisconnected(
                name: ComponentName
            ) {
                finish(null)
            }

            override fun onBindingDied(name: ComponentName) {
                finish(null)
            }

            override fun onNullBinding(name: ComponentName) {
                finish(null)
            }
        }

        connection = serviceConnection

        val intent = Intent().apply {
            component = ComponentName(
                COMPANION_PACKAGE,
                COMPANION_SERVICE
            )
        }

        try {
            bound = context.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            if (!bound) {
                finish(null)
            }
        } catch (_: Exception) {
            finish(null)
        }
    }
}