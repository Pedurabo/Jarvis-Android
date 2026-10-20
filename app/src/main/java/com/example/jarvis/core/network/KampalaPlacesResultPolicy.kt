package com.example.jarvis.core.network

/**
 * M1F-I3C.120M.1
 *
 * Honest, context-aware empty directory results.
 * Nonempty results and service failures retain existing formatting.
 */
object KampalaPlacesResultPolicy {

    fun format(
        request: KampalaPlacesQueryRoute.SearchRequest,
        result: KampalaPlacesRepository.SearchResult
    ): String {
        if (result !is KampalaPlacesRepository.SearchResult.Success) {
            return KampalaPlacesResponseFormatter.format(result)
        }

        if (result.places.isNotEmpty()) {
            return KampalaPlacesResponseFormatter.format(result)
        }

        // Named-place lookups need destination-specific notices.
        val namedDestination = request.query
            ?.trim()
            ?.takeIf {
                request.area == null &&
                    request.category == null &&
                    it.split(Regex("""\s+""")).size >= 2
            }

        if (namedDestination != null) {
            return "I couldn't find a verified directory listing for " +
                "$namedDestination. I can't confirm its address or " +
                "directions from my current Kampala catalog. " +
                "The destination may still exist; my directory is " +
                "incomplete. No location has been assumed."
        }
        val subject = request.category
            ?.replace('_', ' ')
            ?: request.query
            ?: "places"

        val location = request.area ?: "Kampala"

        return buildString {
            append("No matching entries were found in the current ")
            append("Jarvis Kampala directory for ")
            append(subject)
            append(" in ")
            append(location)
            append(". ")
            append("This does not mean those businesses or services ")
            append("do not exist. ")
            append("Our verified catalog is still being expanded. ")
            append("You can try another supported neighbourhood ")
            append("or a broader directory category. ")
            append("No businesses, opening hours, prices or ")
            append("availability have been assumed.")
        }
    }
}
