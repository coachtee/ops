package com.ops.app.data.remote

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns a raw exception from a network call into one of a small, fixed set
 * of categories a small-business owner (or whoever is helping them set up
 * their phone) can actually act on — "failed to connect to /10.0.2.2 (port
 * 8000)" tells a developer the emulator alias leaked into a physical build,
 * but tells an owner nothing. See android/README.md's "Diagnosing a
 * physical-device connection problem" section for what each category means
 * to fix.
 */
enum class ConnectionDiagnosis(val userMessage: String) {
    NETWORK_UNREACHABLE(
        "Can't reach the server. Check that the address in Developer options is correct and " +
            "that this phone and the server are on the same network.",
    ),
    TIMEOUT("The server didn't respond in time. Check your connection and try again."),
    AUTHENTICATION_FAILED("Incorrect email or password."),
    SERVER_ERROR("The server had a problem handling that request. Try again shortly."),
    INVALID_RESPONSE(
        "Got a response the app couldn't understand — check the server address in Developer " +
            "options actually points at the OPS backend, not something else.",
    ),
    SYNC_FAILED("Couldn't sync — your changes are saved on this phone and will retry automatically."),
    UNKNOWN("Something went wrong. Try again.");

    /** [code] is `null` for anything below the HTTP layer (a connection that
     * never got a response at all — unreachable, timeout, no network). */
    companion object {
        fun from(throwable: Throwable, isAuthEndpoint: Boolean = false): ConnectionDiagnosis = when (throwable) {
            is UnknownHostException, is ConnectException -> NETWORK_UNREACHABLE
            is SocketTimeoutException -> TIMEOUT
            is HttpException -> when {
                throwable.code() == 401 || throwable.code() == 403 -> AUTHENTICATION_FAILED
                throwable.code() >= 500 -> SERVER_ERROR
                else -> if (isAuthEndpoint) AUTHENTICATION_FAILED else INVALID_RESPONSE
            }
            is SerializationException -> INVALID_RESPONSE
            is IOException -> NETWORK_UNREACHABLE
            else -> UNKNOWN
        }
    }
}
