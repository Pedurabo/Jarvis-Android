package com.example.jarvis.core.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CancellationException
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
        private const val TAG = "JarvisGemmaIPC"
    }

    suspend fun generate(prompt: String): String? {
        if (prompt.isBlank() || prompt.length > 8000) {
            Log.w(TAG, "request_rejected: invalid_prompt_length")
            return null
        }

        Log.i(TAG, "request_started")

        return try {
            var completedBeforeTimeout = false

            val result = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                val answer = withContext(Dispatchers.Main.immediate) {
                    requestFromCompanion(prompt)
                }
                completedBeforeTimeout = true
                answer
            }

            if (!completedBeforeTimeout) {
                Log.w(TAG, "request_timeout")
            } else if (result.isNullOrBlank()) {
                Log.w(TAG, "request_completed_without_answer")
            } else {
                Log.i(TAG, "request_success")
            }

            result
        } catch (cancelled: CancellationException) {
            Log.i(TAG, "request_cancelled")
            throw cancelled
        } catch (error: Exception) {
            Log.e(
                TAG,
                "request_exception: ${error.javaClass.simpleName}",
                error
            )
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
                    Log.i(TAG, "binder_connected")

                    val readiness = remote.getModelStatus()

                    if (readiness != "MODEL_FILE_READY") {
                        Log.w(TAG, "model_not_ready")
                        finish(null)
                        return
                    }

                    Log.i(TAG, "model_file_ready")

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
                                        Log.w(
                                            TAG,
                                            "model_initialization_failed"
                                        )
                                        finish(null)
                                        return@onMain
                                    }

                                    Log.i(TAG, "model_initialized")

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

                                                        if (answer == null) {
                                                            Log.w(
                                                                TAG,
                                                                "generation_empty_or_invalid"
                                                            )
                                                        } else {
                                                            Log.i(
                                                                TAG,
                                                                "generation_response_received"
                                                            )
                                                        }

                                                        finish(answer)
                                                    }
                                                }
                                            }
                                        )
                                    } catch (error: Exception) {
                                        Log.e(
                                            TAG,
                                            "generation_call_exception: " +
                                                error.javaClass.simpleName,
                                            error
                                        )
                                        finish(null)
                                    }
                                }
                            }
                        }
                    )
                } catch (error: Exception) {
                    Log.e(
                        TAG,
                        "model_setup_exception: " +
                            error.javaClass.simpleName,
                        error
                    )
                    finish(null)
                }
            }

            override fun onServiceDisconnected(
                name: ComponentName
            ) {
                Log.w(TAG, "binder_disconnected")
                finish(null)
            }

            override fun onBindingDied(name: ComponentName) {
                Log.w(TAG, "binder_died")
                finish(null)
            }

            override fun onNullBinding(name: ComponentName) {
                Log.w(TAG, "binder_null_binding")
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
                Log.w(TAG, "binder_bind_failed")
                finish(null)
            } else {
                Log.i(TAG, "binder_bind_requested")
            }
        } catch (error: Exception) {
            Log.e(
                TAG,
                "binder_bind_exception: " +
                    error.javaClass.simpleName,
                error
            )
            finish(null)
        }
    }
}