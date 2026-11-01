package com.example.jarvis.core.network

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Straight-line distance only, not driving or walking distance.
 * Sorts the provider-returned page, not the full provider dataset.
 */
internal object KampalaNearbyDistanceSorter {

    fun distanceMeters(
        originLatitude: Double,
        originLongitude: Double,
        targetLatitude: Double,
        targetLongitude: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val deltaLatitude = Math.toRadians(targetLatitude - originLatitude)
        val deltaLongitude = Math.toRadians(targetLongitude - originLongitude)
        val latitude1 = Math.toRadians(originLatitude)
        val latitude2 = Math.toRadians(targetLatitude)

        val a = sin(deltaLatitude / 2).pow(2.0) +
            cos(latitude1) * cos(latitude2) *
            sin(deltaLongitude / 2).pow(2.0)

        return 2.0 * earthRadiusMeters *
            asin(sqrt(min(1.0, a.coerceAtLeast(0.0))))
    }

    fun sort(
        page: KampalaGeoDiscoveryContract.Page,
        latitude: Double,
        longitude: Double
    ): KampalaGeoDiscoveryContract.Page =
        page.copy(
            places = page.places.sortedBy { place ->
                distanceMeters(
                    latitude,
                    longitude,
                    place.coordinates.latitude,
                    place.coordinates.longitude
                )
            }
        )
}