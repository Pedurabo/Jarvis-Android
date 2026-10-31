package com.example.jarvis.ui

import java.net.URI

/**
 * CDI.6.0-A6.8-B2
 *
 * Presentation adapter for an existing Jarvis-generated
 * source-backed Google Maps handoff.
 *
 * No route is calculated, launched or verified here.
 */
internal object JarvisMapsActionPresentation {

    data class Action(
        val visibleText: String,
        val url: String,
        val label: String = "Review route in Google Maps"
    )

    private const val marker =
        "Review route in Google Maps (tap to open): "

    fun parseOrNull(message: String): Action? {
        val markerAt = message.lastIndexOf(marker)
        if (markerAt < 0) return null

        val prefix = message.substring(0, markerAt).trimEnd()
        val url = message.substring(
            markerAt + marker.length
        ).trim()

        if (prefix.isBlank()) return null

        // The handoff URL must be the final content.
        // Never consume unrelated text following a link.
        if (url.any { it.isWhitespace() }) return null

        val parsed = try {
            URI(url)
        } catch (_: Exception) {
            return null
        }

        if (parsed.scheme != "https") return null
        if (parsed.host != "www.google.com") return null
        if (parsed.rawPath != "/maps/dir/") return null

        val query = parsed.rawQuery ?: return null

        if (!query.startsWith("api=1&")) return null
        if (!query.contains("&origin=")) return null
        if (!query.contains("&destination=")) return null

        val params = query.split("&").associate { part ->
            val equalAt = part.indexOf('=')
            if (equalAt < 1) {
                part to ""
            } else {
                part.substring(0, equalAt) to
                    part.substring(equalAt + 1)
            }
        }

        if (params["api"] != "1") return null
        if (params["origin"].isNullOrBlank()) return null
        if (params["destination"].isNullOrBlank()) return null

        // Reject nested or ambiguous handoff markers.
        if (prefix.contains(marker)) return null

        return Action(
            visibleText = prefix,
            url = url
        )
    }
}