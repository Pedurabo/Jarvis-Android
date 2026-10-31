package com.example.jarvis.core.ai

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * First-stage Kampala provider-backed geocoding.
 *
 * A returned address is a suggestion, not an authoritative
 * place verification or a calculated route.
 */
internal class KampalaDeviceGeocoder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun suggest(destination: String): String? {
        val name = destination.trim()

        if (name.length !in 2..100 || !Geocoder.isPresent()) {
            return null
        }

        return try {
            withTimeoutOrNull(7000L) {
                val geocoder = Geocoder(context, Locale.ENGLISH)
                val query = "$name, Kampala, Uganda"

                // Greater Kampala / Entebbe-area discovery window.
                val south = -0.3
                val west = 32.1
                val north = 0.9
                val east = 33.1

                val addresses: List<Address> =
                    if (Build.VERSION.SDK_INT >= 33) {
                        suspendCancellableCoroutine { continuation ->
                            geocoder.getFromLocationName(
                                query,
                                3,
                                south,
                                west,
                                north,
                                east,
                                object : Geocoder.GeocodeListener {
                                    override fun onGeocode(
                                        results: MutableList<Address>
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resume(results)
                                        }
                                    }

                                    override fun onError(
                                        errorMessage: String?
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resume(emptyList())
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        withContext(Dispatchers.IO) {
                            geocoder.getFromLocationName(
                                query,
                                3,
                                south,
                                west,
                                north,
                                east
                            ).orEmpty()
                        }
                    }

                // Never pick an arbitrary first result when multiple
                // geographically distinct matches are returned.
                val candidates = addresses
                    .filter {
                        it.hasLatitude() && it.hasLongitude() &&
                            it.latitude in south..north &&
                            it.longitude in west..east
                    }
                    .distinctBy {
                        Pair(
                            (it.latitude * 100000).toLong(),
                            (it.longitude * 100000).toLong()
                        )
                    }

                if (candidates.size != 1) {
                    return@withTimeoutOrNull null
                }

                candidates.single()
                    .getAddressLine(0)
                    ?.replace('\n', ' ')
                    ?.replace('\r', ' ')
                    ?.trim()
                    ?.take(160)
                    ?.takeIf { it.isNotBlank() }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
}