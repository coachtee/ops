package com.ops.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.devPrefsDataStore by preferencesDataStore(name = "ops_dev_prefs")

/**
 * The debug-build "point this APK at a different server" escape hatch (see
 * [com.ops.app.data.remote.DevServerUrlInterceptor]) — a tester installs
 * whatever debug APK CI produced and types their machine's LAN IP straight
 * into Settings > Developer options, no rebuild required. Kept in its own
 * tiny DataStore file, separate from [AuthPreferences], because it isn't
 * business/account state: it's a per-device debug convenience that a
 * release build never reads (see the interceptor's BuildConfig.DEBUG
 * guard) and that `logout()` has no reason to clear.
 */
@Singleton
class DevServerPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val SERVER_URL_OVERRIDE = stringPreferencesKey("server_url_override")
    }

    val serverUrlOverride: Flow<String?> = context.devPrefsDataStore.data.map { it[Keys.SERVER_URL_OVERRIDE] }

    suspend fun currentServerUrlOverride(): String? = serverUrlOverride.first()

    /** Pass null or blank to clear the override and fall back to [com.ops.app.BuildConfig.BASE_URL]. */
    suspend fun setServerUrlOverride(url: String?) {
        context.devPrefsDataStore.edit { prefs ->
            if (url.isNullOrBlank()) prefs.remove(Keys.SERVER_URL_OVERRIDE) else prefs[Keys.SERVER_URL_OVERRIDE] = url.trim()
        }
    }
}
