package com.example.jarvis.core.ai

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/**
 * Strictly validates model-generated structured JSON.
 *
 * Does not authorize or execute tools.
 */
internal object StrictActionJsonValidator {

    private const val MAX_DEPTH = 32

    fun isValid(text: String): Boolean {
        return try {
            JsonReader(StringReader(text)).use { reader ->
                reader.isLenient = false

                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    return false
                }

                readObject(reader, 0)

                reader.peek() == JsonToken.END_DOCUMENT
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun readObject(reader: JsonReader, depth: Int) {
        check(depth < MAX_DEPTH) {
            "JSON nesting limit exceeded"
        }

        reader.beginObject()

        val names = mutableSetOf<String>()

        while (reader.hasNext()) {
            val name = reader.nextName()

            check(names.add(name)) {
                "Duplicate JSON key"
            }

            readValue(reader, depth + 1)
        }

        reader.endObject()
    }

    private fun readArray(reader: JsonReader, depth: Int) {
        check(depth < MAX_DEPTH) {
            "JSON nesting limit exceeded"
        }

        reader.beginArray()

        while (reader.hasNext()) {
            readValue(reader, depth + 1)
        }

        reader.endArray()
    }

    private fun readValue(reader: JsonReader, depth: Int) {
        when (reader.peek()) {
            JsonToken.BEGIN_OBJECT ->
                readObject(reader, depth)

            JsonToken.BEGIN_ARRAY ->
                readArray(reader, depth)

            JsonToken.STRING ->
                reader.nextString()

            JsonToken.NUMBER ->
                reader.nextString()

            JsonToken.BOOLEAN ->
                reader.nextBoolean()

            JsonToken.NULL ->
                reader.nextNull()

            else ->
                error("Unexpected JSON token")
        }
    }
}