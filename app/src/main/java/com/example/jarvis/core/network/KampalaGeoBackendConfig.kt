package com.example.jarvis.core.network

/**
 * D5K-C
 *
 * Geographic backend deployment configuration.
 *
 * A blank HTTPS origin disables backend discovery safely.
 * Configure only a verified, phone-reachable HTTPS origin.
 *
 * Do not put credentials, tokens, or private URLs here.
 */
internal object KampalaGeoBackendConfig {
    const val HTTPS_ORIGIN: String = "https://jarvis-uganda-geo-service.onrender.com"
}