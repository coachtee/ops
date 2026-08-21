package com.ops.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors accounts/serializers.py BusinessSerializer exactly. Returned by
 * register/login, `GET /api/business/me/`, and `PATCH /api/business/me/`. */
@Serializable
data class BusinessDto(
    val id: String,
    val name: String,
    @SerialName("trading_name") val tradingName: String = "",
    @SerialName("registration_number") val registrationNumber: String = "",
    @SerialName("tax_number") val taxNumber: String = "",
    @SerialName("vat_number") val vatNumber: String = "",
    @SerialName("is_vat_registered") val isVatRegistered: Boolean = false,
    val industry: String = "other",
    val phone: String = "",
    val email: String = "",
    @SerialName("address_line1") val addressLine1: String = "",
    @SerialName("address_line2") val addressLine2: String = "",
    val suburb: String = "",
    val city: String = "",
    val province: String = "",
    @SerialName("postal_code") val postalCode: String = "",
    val logo: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

/** JSON body for a `PATCH /api/business/me/` that does NOT touch the logo
 * (the multipart variant is used only when a new logo file is attached — see
 * OpsApiService.updateBusinessWithLogo). All fields optional/omittable since
 * a PATCH only sends what changed, but this screen always sends the whole
 * edited form for simplicity, matching how BusinessSerializer treats a full
 * profile edit. */
@Serializable
data class BusinessPatchDto(
    val name: String,
    @SerialName("trading_name") val tradingName: String,
    @SerialName("registration_number") val registrationNumber: String,
    @SerialName("tax_number") val taxNumber: String,
    @SerialName("vat_number") val vatNumber: String,
    @SerialName("is_vat_registered") val isVatRegistered: Boolean,
    val industry: String,
    val phone: String,
    val email: String,
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("address_line2") val addressLine2: String,
    val suburb: String,
    val city: String,
    val province: String,
    @SerialName("postal_code") val postalCode: String,
)
