package com.ops.app.data.remote

import com.ops.app.data.remote.dto.AuthResponseDto
import com.ops.app.data.remote.dto.BusinessDto
import com.ops.app.data.remote.dto.BusinessPatchDto
import com.ops.app.data.remote.dto.ExpenseFieldsDto
import com.ops.app.data.remote.dto.LoginRequestDto
import com.ops.app.data.remote.dto.RefreshRequestDto
import com.ops.app.data.remote.dto.RefreshResponseDto
import com.ops.app.data.remote.dto.RegisterRequestDto
import com.ops.app.data.remote.dto.SyncPullResponseDto
import com.ops.app.data.remote.dto.SyncPushRequestDto
import com.ops.app.data.remote.dto.SyncPushResponseDto
import com.ops.app.data.remote.dto.VisitFieldsDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Every endpoint in API_CONTRACT.md this app needs. Paths are relative to
 * BASE_URL (see app/build.gradle.kts, `http://10.0.2.2:8000/` in debug) and
 * deliberately match the contract's paths byte-for-byte, including trailing
 * slashes (Django's `APPEND_SLASH` behaviour makes those significant).
 *
 * `register`/`login`/`refresh` are the only endpoints that skip the
 * Authorization header (see AuthInterceptor) since they're how a bearer
 * token is obtained in the first place.
 */
interface OpsApiService {

    @POST("api/auth/register/")
    suspend fun register(@Body body: RegisterRequestDto): AuthResponseDto

    @POST("api/auth/login/")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    @POST("api/auth/refresh/")
    suspend fun refresh(@Body body: RefreshRequestDto): RefreshResponseDto

    @GET("api/business/me/")
    suspend fun getBusiness(): BusinessDto

    /** Text-only profile edit — no logo change. */
    @PATCH("api/business/me/")
    suspend fun updateBusiness(@Body body: BusinessPatchDto): BusinessDto

    /** Profile edit that also replaces the logo — multipart, per API_CONTRACT.md. */
    @Multipart
    @PATCH("api/business/me/")
    suspend fun updateBusinessWithLogo(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part logo: MultipartBody.Part,
    ): BusinessDto

    @POST("api/sync/push/")
    suspend fun syncPush(@Body body: SyncPushRequestDto): SyncPushResponseDto

    /** [since] omitted for the first-ever sync (full snapshot) — see API_CONTRACT.md. */
    @GET("api/sync/pull/")
    suspend fun syncPull(@Query("since") since: String? = null): SyncPullResponseDto

    /**
     * Not part of the sync protocol — see API_CONTRACT.md's "Expense receipt
     * attachments". 404s if [id] isn't an expense the server already has
     * (i.e. its own JSON record hasn't synced yet); see SyncManager.syncReceipts,
     * which only calls this once that's confirmed.
     */
    @Multipart
    @POST("api/expenses/{id}/receipt/")
    suspend fun uploadExpenseReceipt(
        @Path("id") id: String,
        @Part receipt: MultipartBody.Part,
    ): ExpenseFieldsDto

    /** Not part of the sync protocol — see API_CONTRACT.md's "Visit photo
     * attachment". 404s if [id] isn't a visit the server already has. */
    @Multipart
    @POST("api/visits/{id}/photo/")
    suspend fun uploadVisitPhoto(
        @Path("id") id: String,
        @Part photo: MultipartBody.Part,
    ): VisitFieldsDto
}
