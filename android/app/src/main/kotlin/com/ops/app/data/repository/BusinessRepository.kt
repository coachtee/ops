package com.ops.app.data.repository

import com.ops.app.data.local.dao.BusinessDao
import com.ops.app.data.local.entities.BusinessEntity
import com.ops.app.data.remote.OpsApiService
import com.ops.app.data.remote.dto.BusinessPatchDto
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `GET`/`PATCH /api/business/me/` — Business is NOT one of the eight sync
 * engine models (see API_CONTRACT.md), so unlike every other repository in
 * this app, writes here go straight to the network rather than through the
 * PENDING-then-sync outbox. That's a deliberate difference, not an
 * inconsistency: business setup already requires connectivity (see
 * AuthRepository), and profile edits after that are rare/low-frequency
 * enough that "requires a connection" is an acceptable, honestly-labelled
 * trade-off for this one screen — the Settings screen surfaces a failure
 * with a retry action rather than silently queuing it.
 */
@Singleton
class BusinessRepository @Inject constructor(
    private val apiService: OpsApiService,
    private val businessDao: BusinessDao,
) {
    fun observe(): Flow<BusinessEntity?> = businessDao.observe()

    suspend fun current(): BusinessEntity? = businessDao.get()

    suspend fun refreshFromServer(): Result<Unit> = runCatching {
        businessDao.upsert(apiService.getBusiness().toLocalEntity())
    }

    suspend fun updateProfile(fields: BusinessPatchDto): Result<Unit> = runCatching {
        businessDao.upsert(apiService.updateBusiness(fields).toLocalEntity())
    }

    suspend fun updateProfileWithLogo(
        fields: BusinessPatchDto,
        logoBytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): Result<Unit> = runCatching {
        val plainText = "text/plain".toMediaType()
        val textParts = mapOf(
            "name" to fields.name,
            "trading_name" to fields.tradingName,
            "registration_number" to fields.registrationNumber,
            "tax_number" to fields.taxNumber,
            "vat_number" to fields.vatNumber,
            "is_vat_registered" to fields.isVatRegistered.toString(),
            "industry" to fields.industry,
            "phone" to fields.phone,
            "email" to fields.email,
            "address_line1" to fields.addressLine1,
            "address_line2" to fields.addressLine2,
            "suburb" to fields.suburb,
            "city" to fields.city,
            "province" to fields.province,
            "postal_code" to fields.postalCode,
        ).mapValues { (_, value) -> value.toRequestBody(plainText) }

        val logoPart = MultipartBody.Part.createFormData(
            "logo",
            fileName,
            logoBytes.toRequestBody(mimeType.toMediaType()),
        )

        businessDao.upsert(apiService.updateBusinessWithLogo(textParts, logoPart).toLocalEntity())
    }
}
