package com.example.jarvis.core.network

import com.example.jarvis.core.ai.KampalaPlaceResolutionPolicy

/**
 * CDI.6.0-A6.9-C1.
 *
 * Converts already validated directory records into the
 * provider-neutral place resolution contract.
 *
 * A directory source-check date is not independent proof
 * that a physical location has been verified.
 *
 * This adapter does not query GPS, launch Maps or
 * alter conversation/navigation state.
 */
internal object KampalaDirectoryResolutionAdapter {

    private const val PROVIDER = "kampala-places-directory"

    fun toRecord(
        place: KampalaPlace
    ): KampalaPlaceResolutionPolicy.PlaceRecord =
        KampalaPlaceResolutionPolicy.PlaceRecord(
            provider = PROVIDER,
            providerId = place.id,
            name = place.name,
            category = place.category,
            area = place.area,
            address = place.address,
            sourceUrl = place.sourceUrl,
            sourceKind =
                KampalaPlaceResolutionPolicy.SourceKind.DIRECTORY,
            sourceCheckedOn = place.verifiedAt,
            latitude = null,
            longitude = null
        )

    /**
     * Preserve service failure as failure.
     * Never interpret unavailable data as "not found".
     */
    sealed class Outcome {
        data class Resolved(
            val resolution: KampalaPlaceResolutionPolicy.Resolution
        ) : Outcome()

        data class Unavailable(
            val message: String
        ) : Outcome()
    }

    fun resolve(
        requestedName: String,
        result: KampalaPlacesRepository.SearchResult,
        requestedArea: String? = null
    ): Outcome =
        when (result) {
            is KampalaPlacesRepository.SearchResult.Failure ->
                Outcome.Unavailable(result.message)

            is KampalaPlacesRepository.SearchResult.Success -> {
                val records = result.places.map(::toRecord)

                Outcome.Resolved(
                    KampalaPlaceResolutionPolicy.resolveExact(
                        requestedName = requestedName,
                        records = records,
                        requestedArea = requestedArea
                    )
                )
            }
        }
    /**
     * Named-destination integration for AssistantController.
     *
     * Preserve the original validated directory objects so
     * existing notices, dates and source formatting remain intact.
     *
     * A failed service cannot produce a navigation destination.
     * An ambiguous name produces multiple candidates, never an
     * arbitrarily selected first result.
     */
    fun matchingDirectoryPlaces(
        requestedName: String,
        result: KampalaPlacesRepository.SearchResult
    ): List<KampalaPlace> {
        val success =
            result as? KampalaPlacesRepository.SearchResult.Success
                ?: return emptyList()

        val resolved = resolve(
            requestedName = requestedName,
            result = success
        )

        val resolution =
            (resolved as? Outcome.Resolved)?.resolution
                ?: return emptyList()

        val recordIds = when (resolution) {
            is KampalaPlaceResolutionPolicy.Resolution.Exact ->
                listOf(resolution.place.providerId)

            is KampalaPlaceResolutionPolicy.Resolution.Ambiguous ->
                resolution.places.map { it.providerId }

            KampalaPlaceResolutionPolicy.Resolution.NotFound ->
                emptyList()
        }

        val originalById = success.places.associateBy {
            it.id.trim()
        }

        return recordIds.mapNotNull { id ->
            originalById[id.trim()]?.takeIf { original ->
                original.name.trim().equals(
                    requestedName.trim(),
                    ignoreCase = true
                )
            }
        }
    }
}
