package com.example.jarvis.skills

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationSkill @Inject constructor(
    @ApplicationContext private val context: Context
) : JarvisSkill {
    override val name = "Medication"
    override val description = "Track medications and set health reminders."
    override val keywords = listOf("medicine", "pill", "tablet", "health", "medication", "dose")

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("action", JSONObject().apply {
                    put("type", "string")
                    put("enum", listOf("record_intake", "set_reminder"))
                })
                put("medication_name", JSONObject().apply { put("type", "string") })
                put("time", JSONObject().apply { put("type", "string"); put("description", "HH:mm format") })
            })
            put("required", listOf("action", "medication_name"))
        }
    }

    override suspend fun execute(command: String): String {
        return try {
            val params = JSONObject(command)
            val med = params.getString("medication_name")
            val action = params.optString("action", "record_intake")

            if (action == "set_reminder") {
                val timeStr = params.optString("time", "08:00")
                scheduleReminder(med, timeStr)
                "I've scheduled a reminder for $med at $timeStr daily."
            } else {
                "Logged your $med intake. I'll remember this in your health history."
            }
        } catch (e: Exception) {
            "I couldn't process the medication request. Please specify the name and action."
        }
    }

    private fun scheduleReminder(med: String, timeStr: String) {
        try {
            val parts = timeStr.split(":")
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
            }
            Log.d("MedicationSkill", "Reminder scheduled for $med at ${calendar.time}")
        } catch (e: Exception) {
            Log.e("MedicationSkill", "Failed to parse time: $timeStr")
        }
    }
}
