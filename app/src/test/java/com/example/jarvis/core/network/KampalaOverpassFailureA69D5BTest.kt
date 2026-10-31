package com.example.jarvis.core.network

import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class KampalaOverpassFailureA69D5BTest {

    private val request =
        KampalaGeoDiscoveryContract.SearchRequest(
            category = "hospital",
            bounds = KampalaGeoDiscoveryContract.Bounds(
                south = 0.30,
                west = 32.55,
                north = 0.35,
                east = 32.60
            ),
            pageSize = 10
        )

    private fun failingProvider(
        failure: Exception
    ): KampalaOverpassDiscoveryProvider =
        KampalaOverpassDiscoveryProvider(
            object : KampalaOverpassDiscoveryProvider.Transport {
                override suspend fun fetch(
                    overpassQuery: String
                ): String {
                    throw failure
                }
            }
        )

    private fun unavailableReason(
        result: KampalaGeoDiscoveryContract.Result
    ): String {
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )

        return (
            result as KampalaGeoDiscoveryContract.Result.Unavailable
        ).reason
    }

    @Test
    fun http429IsRateLimitNotMissingResults() = runBlocking {
        val failure = KampalaOverpassHttpException(429)
        val reason =
            unavailableReason(failingProvider(failure).search(request))

        assertEquals(
            KampalaOverpassFailurePolicy.Kind.RATE_LIMITED,
            KampalaOverpassFailurePolicy.classify(failure)
        )
        assertTrue(reason.contains("request limit"))
    }

    @Test
    fun http502IsServerFailure() = runBlocking {
        val failure = KampalaOverpassHttpException(502)
        val reason =
            unavailableReason(failingProvider(failure).search(request))

        assertEquals(
            KampalaOverpassFailurePolicy.Kind.SERVER_UNAVAILABLE,
            KampalaOverpassFailurePolicy.classify(failure)
        )
        assertTrue(reason.contains("server is temporarily unavailable"))
    }

    @Test
    fun http400IsRejectedRequest() {
        assertEquals(
            KampalaOverpassFailurePolicy.Kind.HTTP_REJECTED,
            KampalaOverpassFailurePolicy.classify(
                KampalaOverpassHttpException(400)
            )
        )
    }

    @Test
    fun timeoutIsNotTreatedAsEmptySearch() = runBlocking {
        val reason = unavailableReason(
            failingProvider(
                SocketTimeoutException("private transport detail")
            ).search(request)
        )

        assertTrue(reason.contains("timed out"))
        assertFalse(reason.contains("private transport detail"))
    }

    @Test
    fun dnsAndConnectionFailuresRemainDistinct() {
        assertEquals(
            KampalaOverpassFailurePolicy.Kind.DNS_FAILURE,
            KampalaOverpassFailurePolicy.classify(
                UnknownHostException("host detail")
            )
        )

        assertEquals(
            KampalaOverpassFailurePolicy.Kind.CONNECTION_FAILURE,
            KampalaOverpassFailurePolicy.classify(
                ConnectException("connection detail")
            )
        )
    }

    @Test
    fun tlsFailureIsExplicitlyClassified() {
        assertEquals(
            KampalaOverpassFailurePolicy.Kind.TLS_FAILURE,
            KampalaOverpassFailurePolicy.classify(
                SSLException("certificate detail")
            )
        )
    }

    @Test
    fun oversizedResponseIsRecognized() {
        assertEquals(
            KampalaOverpassFailurePolicy.Kind.RESPONSE_TOO_LARGE,
            KampalaOverpassFailurePolicy.classify(
                IOException("Overpass response exceeded size limit")
            )
        )
    }

    @Test
    fun invalidJsonHasSafeDiagnostic() = runBlocking {
        val provider = KampalaOverpassDiscoveryProvider(
            object : KampalaOverpassDiscoveryProvider.Transport {
                override suspend fun fetch(
                    overpassQuery: String
                ): String = "{not valid json"
            }
        )

        val reason = unavailableReason(provider.search(request))
        assertTrue(reason.contains("invalid response"))
    }

    @Test
    fun originalExceptionMessageIsNotLeaked() {
        val secret = "PRIVATE-REQUEST-BODY-DO-NOT-LEAK"

        val errors = listOf(
            IOException(secret),
            SocketTimeoutException(secret),
            UnknownHostException(secret),
            ConnectException(secret),
            SSLException(secret),
            IllegalStateException(secret)
        )

        errors.forEach { failure ->
            val reason = KampalaOverpassFailurePolicy.reason(failure)
            assertFalse(reason.contains(secret))
            assertTrue(reason.length in 10..180)
        }
    }

    @Test
    fun providerCancellationIsNotSwallowed() = runBlocking {
        val provider = failingProvider(
            CancellationException("Cancelled by caller")
        )

        try {
            provider.search(request)
            fail("Expected cancellation to propagate")
        } catch (_: CancellationException) {
            // Expected: caller remains in control.
        }
    }

    @Test
    fun unsupportedRequestNeverCallsTransport() = runBlocking {
        var called = false

        val provider = KampalaOverpassDiscoveryProvider(
            object : KampalaOverpassDiscoveryProvider.Transport {
                override suspend fun fetch(
                    overpassQuery: String
                ): String {
                    called = true
                    return """{"elements":[]}"""
                }
            }
        )

        val unsupported = request.copy(
            category = "private_residence"
        )

        val result = provider.search(unsupported)

        assertFalse(called)
        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Unavailable
        )
    }

    @Test
    fun validEmptyMapResponseRemainsEmptySuccess() = runBlocking {
        val provider = KampalaOverpassDiscoveryProvider(
            object : KampalaOverpassDiscoveryProvider.Transport {
                override suspend fun fetch(
                    overpassQuery: String
                ): String = """{"elements":[]}"""
            }
        )

        val result = provider.search(request)

        assertTrue(
            result is KampalaGeoDiscoveryContract.Result.Success
        )
        assertTrue(
            (
                result as KampalaGeoDiscoveryContract.Result.Success
            ).page.places.isEmpty()
        )
    }
}