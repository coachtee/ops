package com.ops.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String = "",
    val email: String = "",
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val phone: String = "",
)

/** The writable subset of Business sent inside `POST /api/auth/register/`'s
 * `business` object — see API_CONTRACT.md's register example body. */
@Serializable
data class BusinessRegistrationDto(
    val name: String,
    @SerialName("trading_name") val tradingName: String = "",
    @SerialName("registration_number") val registrationNumber: String = "",
    @SerialName("tax_number") val taxNumber: String = "",
    @SerialName("vat_number") val vatNumber: String = "",
    @SerialName("is_vat_registered") val isVatRegistered: Boolean = false,
    val phone: String = "",
    val email: String = "",
    @SerialName("address_line1") val addressLine1: String = "",
    @SerialName("address_line2") val addressLine2: String = "",
    val suburb: String = "",
    val city: String = "",
    val province: String = "",
    @SerialName("postal_code") val postalCode: String = "",
    val industry: String = "other",
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val business: BusinessRegistrationDto,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequestDto(
    val refresh: String,
)

@Serializable
data class RefreshResponseDto(
    val access: String,
)

/** Response shape shared by register and login: `{access, refresh, user, business}`. */
@Serializable
data class AuthResponseDto(
    val access: String,
    val refresh: String,
    val user: UserDto,
    val business: BusinessDto,
)
