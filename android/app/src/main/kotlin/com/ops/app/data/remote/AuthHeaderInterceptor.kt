package com.ops.app.data.remote

import com.ops.app.data.datastore.AuthPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches `Authorization: Bearer <access>` from DataStore to every request
 * except the three auth endpoints that don't need it (they're how a token is
 * obtained in the first place — see API_CONTRACT.md, "Auth"). A 401 past
 * this point is handled by [TokenAuthenticator], which runs the refresh flow
 * and retries once.
 */
class AuthHeaderInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (NO_AUTH_PATHS.any { path.endsWith(it) }) {
            return chain.proceed(request)
        }

        // Interceptors run on OkHttp's own dispatcher thread, never the main
        // thread, so a short blocking DataStore read here is safe and is the
        // simplest way to plug a suspend-based preferences store into
        // OkHttp's synchronous Interceptor contract.
        val token = runBlocking { authPreferences.currentAccessToken() }
        val authedRequest = if (!token.isNullOrBlank()) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            request
        }
        return chain.proceed(authedRequest)
    }

    private companion object {
        val NO_AUTH_PATHS = listOf(
            "/api/auth/register/",
            "/api/auth/login/",
            "/api/auth/refresh/",
            "/api/health/",
        )
    }
}
