package com.example.jarvis

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Private, device-local history for the experimental chat.
 * No cloud sync, Firebase, or external storage permissions.
 */
internal class ExperimentalChatHistoryStore(context: Context) {

    private val file = AtomicFile(
        File(context.filesDir, "jarvis_experimental_chat_v1.json")
    )

    companion object {
        private const val TAG = "JarvisChatHistory"
        private const val MAX_MESSAGES = 200
        private const val MAX_MESSAGE_LENGTH = 50000
        private const val MAX_FILE_BYTES = 1500000
    }

    fun load(): List<ExperimentalChatMessage> {
        if (!file.baseFile.exists()) return emptyList()

        return try {
            val bytes = file.openRead().use { stream ->
                stream.readBytes()
            }

            if (bytes.size > MAX_FILE_BYTES) {
                Log.w(TAG, "Saved conversation exceeds size limit")
                return emptyList()
            }

            val root = JSONObject(
                String(bytes, StandardCharsets.UTF_8)
            )

            if (root.getInt("schemaVersion") != 1) {
                Log.w(TAG, "Unsupported conversation schema")
                return emptyList()
            }

            val entries = root.getJSONArray("messages")
            val first = (entries.length() - MAX_MESSAGES).coerceAtLeast(0)
            val restored = ArrayList<ExperimentalChatMessage>()

            for (index in first until entries.length()) {
                val entry = entries.getJSONObject(index)
                val text = entry.getString("text")

                if (text.length > MAX_MESSAGE_LENGTH) {
                    continue
                }

                restored.add(
                    ExperimentalChatMessage(
                        text = text,
                        isUser = entry.getBoolean("isUser")
                    )
                )
            }

            restored
        } catch (error: Exception) {
            // Preserve unreadable data rather than deleting it.
            Log.e(TAG, "Could not restore saved conversation", error)
            emptyList()
        }
    }

    fun save(messages: List<ExperimentalChatMessage>) {
        val root = JSONObject()
        val entries = JSONArray()

        messages.takeLast(MAX_MESSAGES).forEach { message ->
            entries.put(
                JSONObject()
                    .put("text", message.text.take(MAX_MESSAGE_LENGTH))
                    .put("isUser", message.isUser)
            )
        }

        root.put("schemaVersion", 1)
        root.put("messages", entries)

        val bytes = root.toString()
            .toByteArray(StandardCharsets.UTF_8)

        if (bytes.size > MAX_FILE_BYTES) {
            Log.w(TAG, "Conversation too large to save")
            return
        }

        val output = file.startWrite()

        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}