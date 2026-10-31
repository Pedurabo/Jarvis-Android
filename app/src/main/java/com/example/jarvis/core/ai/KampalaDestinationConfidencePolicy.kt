package com.example.jarvis.core.ai

/**
 * CDI.5.58-D: Classification of directory destination confidence.
 *
 * This policy uses only directory search results.
 *
 * Exact does not mean that Jarvis independently confirmed
 * the place's current location, opening status, or existence.
 * It means one usable exact directory record was returned.
 *
 * No fuzzy place matching, route calculation, device location
 * retrieval, or Google Maps invocation occurs here.
 */
internal object KampalaDestinationConfidencePolicy {

    sealed class Resolution {

        data class ExactListing(
            val index: Int
        ) : Resolution()

        data class Ambiguous(
            val indices: List<Int>
        ) : Resolution()

        data class TooManyMatches(
            val count: Int
        ) : Resolution()

        object Unverified : Resolution()

        object DirectoryUnavailable : Resolution()
    }

    fun resolve(
        requestedName: String,
        entries: List<Pair<String, String>>,
        directoryAvailable: Boolean
    ): Resolution {

        if (!directoryAvailable) {
            return Resolution.DirectoryUnavailable
        }

        val indices = KampalaDestinationMatchPolicy.exactIndices(
            requestedName = requestedName,
            entries = entries
        )

        return when {
            indices.isEmpty() ->
                Resolution.Unverified

            indices.size == 1 ->
                Resolution.ExactListing(indices.single())

            indices.size <= 10 ->
                Resolution.Ambiguous(indices)

            else ->
                Resolution.TooManyMatches(indices.size)
        }
    }
}