package com.example.jarvis.core.network

/**
 * D5K-D12-C
 *
 * Runs safe geographic subdivision below the reliability wrapper.
 * The reliability wrapper sees only the final aggregate outcome.
 */
internal class KampalaGeoSubdivisionProvider(
    private val delegate: KampalaGeoDiscoveryContract.Provider
) : KampalaGeoDiscoveryContract.Provider {

    override suspend fun search(
        request: KampalaGeoDiscoveryContract.SearchRequest
    ): KampalaGeoDiscoveryContract.Result =
        KampalaGeoSubdivision.search(delegate, request)
}