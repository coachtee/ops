package com.ops.app.data.remote

import com.ops.app.BuildConfig
import com.ops.app.data.datastore.DevServerPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Debug-only fix for "the emulator alias 10.0.2.2 doesn't mean anything to
 * a physical phone": when a Developer-options server URL override is set
 * (see [DevServerPreferences]), every request's scheme/host/port is
 * rewritten to it before it goes out — the path and everything else about
 * the request (auth header, body) is untouched, so this is a pure
 * "which server" swap, not a second API client.
 *
 * [BuildConfig.DEBUG] is compiled in per build variant, not a runtime flag,
 * so a release build can never be redirected this way regardless of what a
 * stray preference on the device holds — the production endpoint stays
 * whatever [BuildConfig.BASE_URL] was built with.
 */
class DevServerUrlInterceptor @Inject constructor(
    private val devServerPreferences: DevServerPreferences,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!BuildConfig.DEBUG) return chain.proceed(request)

        // Interceptors run on OkHttp's own dispatcher thread, never the main
        // thread — see AuthHeaderInterceptor for the same justification.
        val override = runBlocking { devServerPreferences.currentServerUrlOverride() }
        val overrideUrl = override?.toHttpUrlOrNull() ?: return chain.proceed(request)

        val redirected = request.url.newBuilder()
            .scheme(overrideUrl.scheme)
            .host(overrideUrl.host)
            .port(overrideUrl.port)
            .build()
        return chain.proceed(request.newBuilder().url(redirected).build())
    }
}
