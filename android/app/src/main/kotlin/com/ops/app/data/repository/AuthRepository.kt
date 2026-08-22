package com.ops.app.data.repository

import com.ops.app.data.datastore.AuthPreferences
import com.ops.app.data.local.OpsDatabase
import com.ops.app.data.local.dao.BusinessDao
import com.ops.app.data.remote.ConnectionDiagnosis
import com.ops.app.data.remote.OpsApiService
import com.ops.app.data.remote.dto.BusinessRegistrationDto
import com.ops.app.data.remote.dto.LoginRequestDto
import com.ops.app.data.remote.dto.RegisterRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one-time first-run "let's set up your business" flow, and signing back
 * in on this device. Per DISCOVERY.md's Risks/assumptions: establishing
 * identity (register/login) is the one thing in this app that requires
 * connectivity — everything else works offline once an account exists. On
 * failure this never queues anything locally; nothing is written to Room or
 * DataStore until the server actually confirms.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: OpsApiService,
    private val authPreferences: AuthPreferences,
    private val businessDao: BusinessDao,
    private val database: OpsDatabase,
) {
    val isSignedIn: Flow<Boolean> = authPreferences.isSignedIn

    /** Creates the User + Business + owner Membership in one step (`POST /api/auth/register/`). */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        business: BusinessRegistrationDto,
    ): Result<Unit> = runCatching {
        val response = apiService.register(
            RegisterRequestDto(
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName,
                business = business,
            ),
        )
        authPreferences.saveTokens(response.access, response.refresh)
        authPreferences.saveSignedInEmail(email)
        businessDao.upsert(response.business.toLocalEntity())
    }.recoverCatching { throw it.toFriendlyAuthError() }

    /** Signs into an existing account (e.g. the demo login, or a second
     * device) — `POST /api/auth/login/`. Pulls its business/data down through
     * the normal sync path afterwards, not through this call. */
    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = apiService.login(LoginRequestDto(email = email, password = password))
        authPreferences.saveTokens(response.access, response.refresh)
        authPreferences.saveSignedInEmail(email)
        businessDao.upsert(response.business.toLocalEntity())
    }.recoverCatching { throw it.toFriendlyAuthError() }

    /** Forgets local tokens/cursor and wipes the local database — this
     * device goes back to needing Business Setup / sign-in. The server side
     * of the account is untouched. */
    suspend fun logout() {
        authPreferences.clear()
        // clearAllTables() asserts it isn't called on the main thread.
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }

    /**
     * Replaces a raw exception message (e.g. "failed to connect to /10.0.2.2
     * (port 8000)" — accurate to a developer, meaningless to a small-business
     * owner or whoever is helping them get their phone connected) with one
     * of a fixed set of categories the user can actually act on. A 400 with
     * a real validation-error body (e.g. register rejecting a duplicate
     * email) is the one case kept verbatim, since DRF's field-error body is
     * already specific and more useful than any generic category message.
     * See ConnectionDiagnosis and android/README.md's diagnosis section.
     */
    private fun Throwable.toFriendlyAuthError(): Throwable {
        if (this is HttpException && code() == 400) {
            val body = try {
                response()?.errorBody()?.string()
            } catch (e: Exception) {
                null
            }
            if (!body.isNullOrBlank()) return Exception(body, this)
        }
        return Exception(ConnectionDiagnosis.from(this, isAuthEndpoint = true).userMessage, this)
    }
}
