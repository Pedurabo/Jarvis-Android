package com.example.jarvis.core.network

import com.google.gson.JsonParseException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * CDI.6.0-A6.9-D5B
 *
 * A typed HTTP failure from the experimental Overpass
 * transport. The response body is intentionally discarded.
 *
 * Never put raw server HTML, request details or tokens
 * in exception messages.
 */
internal class KampalaOverpassHttpException(
    val statusCode: Int
) : IOException("OpenStreetMap HTTP response category")

/**
 * Safe, bounded failure categories for geographic discovery.
 *
 * Does not perform networking, logging, retries or caching.
 * The existing reliability provider remains responsible
 * for serialized calls and its failure cooldown.
 *
 * The categories are internal diagnostics, not evidence
 * that any particular hospital or business exists.
 */
internal object KampalaOverpassFailurePolicy {

    internal enum class Kind {
        RATE_LIMITED,
        SERVER_UNAVAILABLE,
        HTTP_REJECTED,
        TIMEOUT,
        DNS_FAILURE,
        CONNECTION_FAILURE,
        TLS_FAILURE,
        RESPONSE_TOO_LARGE,
        EMPTY_RESPONSE,
        INVALID_RESPONSE,
        NETWORK_FAILURE,
        UNKNOWN
    }

    fun classify(failure: Exception): Kind =
        when (failure) {
            is KampalaOverpassHttpException -> when {
                failure.statusCode == 429 ->
                    Kind.RATE_LIMITED

                failure.statusCode in 500..599 ->
                    Kind.SERVER_UNAVAILABLE

                else ->
                    Kind.HTTP_REJECTED
            }

            is SocketTimeoutException ->
                Kind.TIMEOUT

            is UnknownHostException ->
                Kind.DNS_FAILURE

            is ConnectException ->
                Kind.CONNECTION_FAILURE

            is SSLException ->
                Kind.TLS_FAILURE

            is JsonParseException,
            is IllegalStateException ->
                Kind.INVALID_RESPONSE

            is IOException -> {
                // Classify only messages created by our own
                // bounded-response transport. All other I/O
                // messages must remain private.
                when (failure.message) {
                    "Overpass response exceeded size limit" ->
                        Kind.RESPONSE_TOO_LARGE

                    "Empty Overpass response" ->
                        Kind.EMPTY_RESPONSE

                    else ->
                        Kind.NETWORK_FAILURE
                }
            }

            else ->
                Kind.UNKNOWN
        }

    /**
     * All returned descriptions are fixed strings.
     * Never forward failure.message or failure.toString().
     */
    fun reason(failure: Exception): String =
        when (classify(failure)) {
            Kind.RATE_LIMITED ->
                "OpenStreetMap request limit reached. " +
                    "Please try again later."

            Kind.SERVER_UNAVAILABLE ->
                "OpenStreetMap server is temporarily unavailable."

            Kind.HTTP_REJECTED ->
                "OpenStreetMap rejected the geographic request."

            Kind.TIMEOUT ->
                "OpenStreetMap request timed out."

            Kind.DNS_FAILURE ->
                "OpenStreetMap server address could not be resolved."

            Kind.CONNECTION_FAILURE ->
                "Connection to OpenStreetMap could not be established."

            Kind.TLS_FAILURE ->
                "Secure connection to OpenStreetMap failed."

            Kind.RESPONSE_TOO_LARGE ->
                "OpenStreetMap response exceeded the size limit. " +
                    "Narrow the search area."

            Kind.EMPTY_RESPONSE ->
                "OpenStreetMap returned an empty HTTP response."

            Kind.INVALID_RESPONSE ->
                "OpenStreetMap returned an invalid response."

            Kind.NETWORK_FAILURE ->
                "OpenStreetMap network request failed."

            Kind.UNKNOWN ->
                "OpenStreetMap geographic discovery failed."
        }
}