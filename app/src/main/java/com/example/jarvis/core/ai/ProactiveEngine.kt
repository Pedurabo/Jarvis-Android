package com.example.jarvis.core.ai

import android.content.Context
import android.util.Log
import com.example.jarvis.core.context.AssistantContext
import com.example.jarvis.core.context.ContextManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProactiveEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contextManager: ContextManager,
    private val llmManager: LLMManager,
    private val firebaseMemoryRepository: FirebaseMemoryRepository
) {
    private val TAG = "ProactiveEngine"
    private val PREFS_NAME = "proactive_prefs"
    private val KEY_LAST_NOTIFICATION = "last_notification_time"
    private val MIN_INTERVAL_MS = TimeUnit.HOURS.toMillis(1)

    suspend fun runAnalysis(): ProactiveResult {
        val currentContext = contextManager.getCurrentContext()

        // 1. Rule-Based Triggers (Fast & Battery Efficient)
        val ruleResult = checkRules(currentContext)
        if (ruleResult != null) return ruleResult

        // 2. Rate Limiting Check
        if (!shouldTriggerLLM()) return ProactiveResult.None

        // 3. LLM-Based Analysis (Hybrid)
        return analyzeContextWithLLM(currentContext)
    }

    private fun checkRules(ctx: AssistantContext): ProactiveResult? {
        // Example: Battery Low
        if (ctx.deviceState.batteryLevel < 15 && !ctx.deviceState.isCharging) {
            return ProactiveResult.Suggestion("Battery is getting low (15%). Should I enable power saving mode?", 8)
        }

        // Example: Upcoming Calendar Event
        ctx.calendarEvents.firstOrNull()?.let { event ->
            val startTime = event.startTime.toLongOrNull() ?: 0L
            val diffMinutes = (startTime - System.currentTimeMillis()) / (1000 * 60)
            if (diffMinutes in 1..30) {
                return ProactiveResult.Suggestion("Reminder: '${event.title}' starts in $diffMinutes minutes.", 9)
            }
        }

        return null
    }

    private suspend fun analyzeContextWithLLM(ctx: AssistantContext): ProactiveResult {
        val memories = firebaseMemoryRepository.getRelevantMemories(10)
        val contextJson = ctx.toJsonObject().toString()

        val prompt = """
            Analyze the following user context and memories to decide if a proactive suggestion is needed.
            Be concise. Do not be annoying.
            Suggestions should be actionable.

            Context: $contextJson
            Memories: ${memories.joinToString("; ")}

            Respond strictly in JSON:
            {
              "shouldNotify": boolean,
              "message": "suggestion text",
              "priority": 1-10
            }
        """.trimIndent()

        return try {
            val response = llmManager.getSmartResponse(prompt)
            val json = JSONObject(response)
            if (json.getBoolean("shouldNotify")) {
                updateLastNotificationTime()
                ProactiveResult.Suggestion(json.getString("message"), json.getInt("priority"))
            } else {
                ProactiveResult.None
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM Proactive analysis failed", e)
            ProactiveResult.None
        }
    }

    private fun shouldTriggerLLM(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTime = prefs.getLong(KEY_LAST_NOTIFICATION, 0L)
        return (System.currentTimeMillis() - lastTime) > MIN_INTERVAL_MS
    }

    private fun updateLastNotificationTime() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_NOTIFICATION, System.currentTimeMillis())
            .apply()
    }
}

sealed class ProactiveResult {
    object None : ProactiveResult()
    data class Suggestion(val message: String, val priority: Int) : ProactiveResult()
}
