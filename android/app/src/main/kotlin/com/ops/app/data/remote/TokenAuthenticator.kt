package com.ops.app.data.remote

import com.ops.app.data.datastore.AuthPreferences
import com.ops.app.data.remote.dto.RefreshRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

/**
 * Handles a 401 by running `POST /api/auth/refresh/` once and retrying the
 * original request with the new access token — "a 401 triggers the
 * refresh-token flow once, then retries" per the task brief.
 *
 * [apiService] is injected as a [Provider] rather than [OpsApiService]
 * directly: the OkHttpClient this Authenticator attaches to is also the
 * client Retrofit/OpsApiService is built from, so a direct dependency would
 * be circular. A `Provider` defers resolving it until [authenticate] actually
 * calls `.get()`, which by then the graph has finished constructing.
 */
class TokenAuthenticator @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val apiService: Provider<OpsApiService>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseChainLength(response) >= 2) {
            // Already retried once with a refreshed token and still got a
            // 401 (or the refresh call itself is what's failing) — give up
            // rather than looping. The caller sees the original 401.
            return null
        }

        val refreshToken = runBlocking { authPreferences.currentRefreshToken() }
        if (refreshToken.isNullOrBlank()) return null

        val newAccessToken = try {
            runBlocking { apiService.get().refresh(RefreshRequestDto(refreshToken)) }.access
        } catch (e: Exception) {
            return null
        }

        runBlocking { authPreferences.saveAccessToken(newAccessToken) }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun responseChainLength(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
