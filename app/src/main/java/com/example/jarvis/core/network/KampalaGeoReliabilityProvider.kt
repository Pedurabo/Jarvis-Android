package com.example.jarvis.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * CDI.6.0-A6.9-D3B
 *
 * In-memory reliability wrapper for geographic providers.
 *
 * - Cache successful, validated requests for a short TTL.
 * - Apply a cooldown after unavailable or invalid responses.
 * - Serialize requests so concurrent callers do not
 *   issue duplicate or uncontrolled provider requests.
 * - Never turn an outage into an empty successful result.
 *
 * No disk persistence, background polling, device GPS,
 * automatic fallback or external network setup.
 *
 * Only use this wrapper for explicitly authorized queries.
 */
internal class KampalaGeoReliabilityProvider(
    private val delegate: KampalaGeoDiscoveryContract.Provider,
    private val nowMillis: () -> Long = {
        System.nanoTime() / 1_000_000L
    },
    private val cacheTtlMillis: Long = 300_000L,
    private val cooldownMillis: Long = 60_000L,
    private val maxCacheEntries: Int = 32
) : KampalaGeoDiscoveryContract.Provider {

    init {
        require(cacheTtlMillis > 0L)
        require(cooldownMillis > 0L)
        require(maxCacheEntries in 1..128)
    }

    private data class Cached(
        val result: KampalaGeoDiscoveryContract.Result.Success,
        val storedAtMillis: Long
    )

    private val mutex = Mutex()

    private val cache = LinkedHashMap<
        KampalaGeoDiscoveryContract.SearchRequest,
        Cached
    >()

    private var cooldownStartedAtMillis: Long? = null

    private fun elapsedSince(start: Long, now: Long): Long =
        if (now >= start) now - start else Long.MAX_VALUE

    private fun pruneExpired(now: Long) {
        val iterator = cache.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()

            if (
                elapsedSince(
                    entry.value.storedAtMillis,
                    now
                ) >= cacheTtlMillis
            ) {
                iterator.remove()
            }
        }
    }

    private fun unavailable(message: String) =
        KampalaGeoDiscoveryContract.Result.Unavailable(message)

    override suspend fun search(
        request: KampalaGeoDiscoveryContract.SearchRequest
    ): KampalaGeoDiscoveryContract.Result {

        if (!KampalaGeoDiscoveryContract.validRequest(request)) {
            return unavailable(
                "Invalid geographic discovery request."
            )
        }

        return mutex.withLock {
            val now = nowMillis()
            pruneExpired(now)

            // Previously validated successful information
            // may be reused without contacting the provider.
            val cached = cache[request]

            if (cached != null) {
                return@withLock cached.result
            }

            // An outage stays an outage. Never fabricate
            // a successful empty page during cooldown.
            val cooldownStart = cooldownStartedAtMillis

            if (
                cooldownStart != null &&
                elapsedSince(
                    cooldownStart,
                    now
                ) < cooldownMillis
            ) {
                return@withLock unavailable(
                    "Geographic discovery is temporarily " +
                        "unavailable. Please try again later."
                )
            }

            val raw = try {
                delegate.search(request)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                unavailable(
                    "The geographic provider could not " +
                        "complete the request."
                )
            }

            val validated =
                KampalaGeoDiscoveryContract.validate(
                    request,
                    raw
                )

            when (validated) {
                is KampalaGeoDiscoveryContract.Result.Success -> {
                    cooldownStartedAtMillis = null

                    cache[request] = Cached(
                        result = validated,
                        storedAtMillis = nowMillis()
                    )

                    while (cache.size > maxCacheEntries) {
                        val oldest = cache.keys.firstOrNull()
                            ?: break

                        cache.remove(oldest)
                    }

                    validated
                }

                is KampalaGeoDiscoveryContract.Result.Unavailable -> {
                    cooldownStartedAtMillis = nowMillis()
                    validated
                }
            }
        }
    }
}