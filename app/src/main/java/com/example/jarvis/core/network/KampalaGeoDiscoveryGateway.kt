package com.example.jarvis.core.network

/**
 * CDI.6.0-A6.9-D3C
 *
 * Explicit entry point for geographic discovery.
 *
 * The default dependency graph is:
 *
 * Gateway
 *   -> Reliability wrapper
 *       -> OpenStreetMap/Overpass provider
 *
 * Construction does not perform a network request.
 *
 * The gateway is not automatically connected to
 * AssistantController, GPS or background work.
 */
internal class KampalaGeoDiscoveryGateway private constructor(
    private val provider: KampalaGeoDiscoveryContract.Provider
) {

    suspend fun search(
        request: KampalaGeoDiscoveryContract.SearchRequest
    ): KampalaGeoDiscoveryContract.Result {
        if (!KampalaGeoDiscoveryContract.validRequest(request)) {
            return KampalaGeoDiscoveryContract.Result.Unavailable(
                "Invalid geographic discovery request."
            )
        }

        return provider.search(request)
    }

    companion object {

        /**
         * Explicit production composition.
         *
         * The network is contacted only when search()
         * is subsequently invoked with a supported request.
         *
         * Create one gateway per intended lifetime to
         * preserve its cache and cooldown state.
         */
                /**
         * Explicit HTTPS backend composition.
         * No request is made during construction.
         */
        fun createBackend(
            baseUrl: String
        ): KampalaGeoDiscoveryGateway {

            val transport =
                KampalaGeoSubdivisionProvider(KampalaJarvisGeoApiProvider(baseUrl))

            return KampalaGeoDiscoveryGateway(
                KampalaGeoReliabilityProvider(transport)
            )
        }
fun createOverpass(): KampalaGeoDiscoveryGateway {
            val transport =
                KampalaOverpassDiscoveryProvider()

            val reliable =
                KampalaGeoReliabilityProvider(transport)

            return KampalaGeoDiscoveryGateway(reliable)
        }

        /**
         * Test composition.
         *
         * Uses exactly the same reliability wrapper as
         * the production composition, but substitutes a
         * controlled provider that makes no network calls.
         */
        internal fun forTesting(
            delegate: KampalaGeoDiscoveryContract.Provider,
            nowMillis: () -> Long
        ): KampalaGeoDiscoveryGateway =
            KampalaGeoDiscoveryGateway(
                KampalaGeoReliabilityProvider(
                    delegate = delegate,
                    nowMillis = nowMillis
                )
            )
    }
}