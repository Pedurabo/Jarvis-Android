package com.example.jarvis.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KampalaGeoTransportFailuresP0R1Test {

    private val baseUrl =
        "https://jarvis-uganda-geo-service.onrender.com"

    private val request =
        KampalaGeoDiscoveryContract.SearchRequest(
            category = "hospital",
            bounds = KampalaGeoDiscoveryContract.Bounds(
                south = 0.300,
                west = 32.550,
                north = 0.350,
                east = 32.600
            ),
            pageSize = 10
        )

    private suspend fun searchThrowing(
        failure: Exception
    ): KampalaGeoDiscoveryContract.Result {

        val transport =
            object : KampalaJarvisGeoApiProvider.Transport {
                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult {
                    throw failure
                }
            }

        return KampalaJarvisGeoApiProvider(
            baseUrl,
            transport
        ).search(request)
    }

    private suspend fun searchHttp(
        status: Int
    ): KampalaGeoDiscoveryContract.Result {

        val transport =
            object : KampalaJarvisGeoApiProvider.Transport {
                override suspend fun get(
                    url: String
                ) = KampalaJarvisGeoApiProvider.HttpResult(
                    status,
                    "{}"
                )
            }

        return KampalaJarvisGeoApiProvider(
            baseUrl,
            transport
        ).search(request)
    }

    private fun reason(
        result: KampalaGeoDiscoveryContract.Result
    ): String {
        assertTrue(
            "Failure must never produce successful listings.",
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )

        return (
            result as KampalaGeoDiscoveryContract.Result.Unavailable
        ).reason
    }

    @Test
    fun socketTimeoutIsRecognized() = runBlocking {
        val result = searchThrowing(
            java.net.SocketTimeoutException("internal details")
        )

        assertTrue(reason(result).contains("timed out"))
        assertFalse(reason(result).contains("internal details"))
    }

    @Test
    fun dnsFailureIsRecognized() = runBlocking {
        val result = searchThrowing(
            java.net.UnknownHostException("internal DNS details")
        )

        assertTrue(reason(result).contains("hostname"))
        assertFalse(reason(result).contains("internal DNS details"))
    }

    @Test
    fun connectionFailureIsRecognized() = runBlocking {
        val result = searchThrowing(
            java.net.ConnectException("internal connection details")
        )

        assertTrue(reason(result).contains("connection"))
    }

    @Test
    fun secureTransportFailureIsRecognized() = runBlocking {
        val result = searchThrowing(
            javax.net.ssl.SSLException("internal TLS details")
        )

        assertTrue(reason(result).contains("secure connection"))
    }

    @Test
    fun genericIoFailureIsRecognized() = runBlocking {
        val result = searchThrowing(
            java.io.IOException("internal transport details")
        )

        assertTrue(reason(result).contains("interrupted"))
    }

    @Test
    fun serverErrorNeverBecomesSuccess() = runBlocking {
        val result = searchHttp(503)

        assertTrue(reason(result).contains("server error"))
    }

    @Test
    fun rateLimitIsRecognized() = runBlocking {
        val result = searchHttp(429)

        assertTrue(reason(result).contains("rate-limiting"))
    }

    @Test
    fun gatewayTimeoutIsRecognized() = runBlocking {
        val result = searchHttp(504)

        assertTrue(reason(result).contains("timed out"))
    }

    @Test
    fun cancellationIsPropagated() = runBlocking {
        val transport =
            object : KampalaJarvisGeoApiProvider.Transport {
                override suspend fun get(
                    url: String
                ): KampalaJarvisGeoApiProvider.HttpResult {
                    throw CancellationException("cancelled")
                }
            }

        try {
            KampalaJarvisGeoApiProvider(
                baseUrl,
                transport
            ).search(request)

            fail("Cancellation should propagate.")
        } catch (_: CancellationException) {
            // Expected: cancellation must not become Unavailable.
        }
    }
}