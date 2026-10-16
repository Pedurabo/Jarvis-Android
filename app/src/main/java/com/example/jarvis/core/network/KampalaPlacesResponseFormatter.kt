package com.example.jarvis.core.network

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object KampalaPlacesResponseFormatter {

    private val displayDateFormat =
        DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH)

    fun format(result: KampalaPlacesRepository.SearchResult): String {
        return when (result) {
            is KampalaPlacesRepository.SearchResult.Failure ->
                result.message

            is KampalaPlacesRepository.SearchResult.Success -> {
                if (result.places.isEmpty()) {
                    "No matching places were found in the current Kampala directory. " +
                        "The directory is still being expanded."
                } else {
                    buildString {
                        appendLine("Kampala Places Directory")
                        appendLine()

                        result.places.forEachIndexed { index, place ->
                            if (result.places.size > 1) {
                                appendLine("${index + 1}. ${place.name}")
                            } else {
                                appendLine(place.name)
                            }

                            appendLine(
                                "${place.area} - ${place.category.replaceFirstChar {
                                    it.titlecase(Locale.ENGLISH)
                                }}"
                            )
                            appendLine("Address: ${place.address}")
                            appendLine("Source: ${sourceLabel(place.sourceUrl)}")
                            appendLine("Source link: ${place.sourceUrl}")
                            appendLine(
                                "Source checked: ${displayDate(place.verifiedAt)}"
                            )

                            if (index != result.places.lastIndex) {
                                appendLine()
                            }
                        }

                        appendLine()
                        append(
                            "Directory information only. Opening hours and " +
                                "current availability are not confirmed."
                        )
                    }
                }
            }
        }
    }

    private fun sourceLabel(url: String): String {
        return when {
            url.startsWith("https://visitkampala.kcca.go.ug/") ->
                "Kampala tourism directory"

            url.startsWith("https://www.theacaciamall.com/") ->
                "Acacia Mall official website"

            else -> "Published directory source"
        }
    }

    private fun displayDate(value: String): String {
        return try {
            LocalDate.parse(value).format(displayDateFormat)
        } catch (_: Exception) {
            value
        }
    }
}