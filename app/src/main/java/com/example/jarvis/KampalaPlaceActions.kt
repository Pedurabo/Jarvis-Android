package com.example.jarvis

import android.net.Uri

internal object KampalaPlaceActions {

    data class Action(
        val sourceUrl: String,
        val mapsUrl: String
    )

    fun parse(response: String): List<Action> {
        if (!response.startsWith("Kampala Places Directory")) {
            return emptyList()
        }

        val results = mutableListOf<Action>()
        var address: String? = null

        response.lineSequence().forEach { raw ->
            val line = raw.trim()

            when {
                line.startsWith("Address: ") -> {
                    address = line.removePrefix("Address: ").trim()
                }

                line.startsWith("Source link: ") -> {
                    val source = line.removePrefix("Source link: ").trim()
                    val location = address

                    val uri = try {
                        Uri.parse(source)
                    } catch (_: Exception) {
                        null
                    }

                    if (
                        results.size < 10 &&
                        uri?.scheme == "https" &&
                        !uri.host.isNullOrBlank() &&
                        !location.isNullOrBlank() &&
                        location.length <= 240
                    ) {
                        val mapsUrl = Uri.parse(
                            "https://www.google.com/maps/search/"
                        ).buildUpon()
                            .appendQueryParameter("api", "1")
                            .appendQueryParameter("query", location)
                            .build()
                            .toString()

                        results.add(
                            Action(
                                sourceUrl = source,
                                mapsUrl = mapsUrl
                            )
                        )
                    }

                    address = null
                }
            }
        }

        return results
    }
}