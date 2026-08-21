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

private val Context.dataStore by preferencesDataStore(name = "ops_prefs")

/**
 * JWT access/refresh tokens and the sync pull cursor. DataStore (not Room)
 * because this is install-scoped app state, not a syncable business record.
 */
@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        // The cursor for GET /api/sync/pull/?since=<cursor>. Persisted only
        // once a whole pull succeeds (see SyncManager) — set from the
        // response's server_time, taken by the server before its query ran,
        // per API_CONTRACT.md, so a row written mid-request is never missed.
        val SYNC_CURSOR = stringPreferencesKey("sync_cursor")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[Keys.REFRESH_TOKEN] }
    val syncCursor: Flow<String?> = context.dataStore.data.map { it[Keys.SYNC_CURSOR] }
    val isSignedIn: Flow<Boolean> = accessToken.map { !it.isNullOrBlank() }

    suspend fun currentAccessToken(): String? = accessToken.first()
    suspend fun currentRefreshToken(): String? = refreshToken.first()
    suspend fun currentSyncCursor(): String? = syncCursor.first()

    suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = access
            prefs[Keys.REFRESH_TOKEN] = refresh
        }
    }

    /** Called after a successful `/api/auth/refresh/` — refresh token itself doesn't rotate. */
    suspend fun saveAccessToken(access: String) {
        context.dataStore.edit { prefs -> prefs[Keys.ACCESS_TOKEN] = access }
    }

    suspend fun saveSyncCursor(cursor: String) {
        context.dataStore.edit { prefs -> prefs[Keys.SYNC_CURSOR] = cursor }
    }

    /** Logout / sign-out: forgets tokens and the sync cursor (a later sign-in
     * on this device starts sync fresh with a full snapshot pull). */
    suspend fun clear() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }
}
