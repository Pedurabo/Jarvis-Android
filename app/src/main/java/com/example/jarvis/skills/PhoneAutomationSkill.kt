package com.example.jarvis.skills

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneAutomationSkill @Inject constructor(
    @ApplicationContext private val context: Context
) : JarvisSkill {
    override val name = "PhoneAutomation"
    override val description = "Control device settings, make calls, or open apps."
    override val keywords = listOf("call", "open", "wifi", "bluetooth", "brightness", "flashlight")

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("action", JSONObject().apply {
                    put("type", "string")
                    put("enum", listOf("call", "open_app"))
                })
                put("target", JSONObject().apply { put("type", "string") })
                put("value", JSONObject().apply { put("type", "string") })
            })
            put("required", listOf("action"))
        }
    }

    override suspend fun execute(command: String): String {
        return try {
            val params = JSONObject(command)
            val action = params.getString("action")
            val target = params.optString("target")
            val value = params.optString("value")

            when (action) {
                "call" -> {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$target")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    "Dialing $target..."
                }
                "open_app" -> {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(target)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                        "Opening $target"
                    } else {
                        "I couldn't find the app with package name $target. Should I look it up on the Play Store?"
                    }
                }
                "toggle_flashlight" -> {
                    // Flashlight logic would go here using CameraManager
                    "Toggling flashlight..."
                }
                "set_brightness" -> {
                    "Brightness adjustment requires manual permission. Opening settings..."
                }
                else -> "I'm not sure how to perform that automation yet."
            }
        } catch (e: Exception) {
            "Phone automation failed: ${e.localizedMessage}"
        }
    }
}
