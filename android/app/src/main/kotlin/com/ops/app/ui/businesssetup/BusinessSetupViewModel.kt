package com.ops.app.ui.businesssetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.remote.dto.BusinessPatchDto
import com.ops.app.data.remote.dto.BusinessRegistrationDto
import com.ops.app.data.repository.AuthRepository
import com.ops.app.data.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BusinessSetupMode { CREATE, SIGN_IN }

data class BusinessSetupForm(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val businessName: String = "",
    val tradingName: String = "",
    val registrationNumber: String = "",
    val taxNumber: String = "",
    val vatNumber: String = "",
    val isVatRegistered: Boolean = false,
    val industry: String = "other",
    val businessPhone: String = "",
    val businessEmail: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val suburb: String = "",
    val city: String = "",
    val province: String = "",
    val postalCode: String = "",
    val logoBytes: ByteArray? = null,
    val logoFileName: String = "logo.jpg",
    val logoMimeType: String = "image/jpeg",
)

data class BusinessSetupUiState(
    val mode: BusinessSetupMode = BusinessSetupMode.CREATE,
    val step: Int = 0,
    val form: BusinessSetupForm = BusinessSetupForm(),
    val signInEmail: String = "",
    val signInPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val canGoNext: Boolean
        get() = when (step) {
            0 -> form.firstName.isNotBlank() && form.email.isNotBlank() && form.password.length >= 8 && form.businessName.isNotBlank()
            else -> true
        }
}

/**
 * First-run "let's set up your business" flow (create a brand new account),
 * plus a "Sign in" mode for an existing account on this device — needed for
 * the demo login flow (see android/README.md) where the business already
 * exists server-side (seeded by `manage.py seed_demo`). Both modes require
 * connectivity — see DISCOVERY.md's Risks/assumptions — and never queue a
 * failed attempt locally; the screen shows the error and offers Retry.
 */
@HiltViewModel
class BusinessSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val businessRepository: BusinessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessSetupUiState())
    val uiState: StateFlow<BusinessSetupUiState> = _uiState.asStateFlow()

    fun setMode(mode: BusinessSetupMode) = _uiState.update { it.copy(mode = mode, errorMessage = null) }

    fun updateForm(transform: (BusinessSetupForm) -> BusinessSetupForm) =
        _uiState.update { it.copy(form = transform(it.form), errorMessage = null) }

    fun updateSignInEmail(value: String) = _uiState.update { it.copy(signInEmail = value, errorMessage = null) }
    fun updateSignInPassword(value: String) = _uiState.update { it.copy(signInPassword = value, errorMessage = null) }

    fun nextStep() = _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(2)) }
    fun previousStep() = _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun submitCreate(onSuccess: () -> Unit) {
        val form = _uiState.value.form
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val business = BusinessRegistrationDto(
                name = form.businessName,
                tradingName = form.tradingName,
                registrationNumber = form.registrationNumber,
                taxNumber = form.taxNumber,
                vatNumber = form.vatNumber,
                isVatRegistered = form.isVatRegistered,
                phone = form.businessPhone,
                email = form.businessEmail,
                addressLine1 = form.addressLine1,
                addressLine2 = form.addressLine2,
                suburb = form.suburb,
                city = form.city,
                province = form.province,
                postalCode = form.postalCode,
                industry = form.industry,
            )
            authRepository.register(
                email = form.email,
                password = form.password,
                firstName = form.firstName,
                lastName = form.lastName,
                business = business,
            ).onSuccess {
                uploadLogoIfAny(form)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Couldn't create your business. Check your connection and try again.")
                }
            }
        }
    }

    private suspend fun uploadLogoIfAny(form: BusinessSetupForm) {
        val logoBytes = form.logoBytes ?: return
        val current = businessRepository.current() ?: return
        businessRepository.updateProfileWithLogo(
            fields = BusinessPatchDto(
                name = current.name,
                tradingName = current.tradingName,
                registrationNumber = current.registrationNumber,
                taxNumber = current.taxNumber,
                vatNumber = current.vatNumber,
                isVatRegistered = current.isVatRegistered,
                industry = current.industry,
                phone = current.phone,
                email = current.email,
                addressLine1 = current.addressLine1,
                addressLine2 = current.addressLine2,
                suburb = current.suburb,
                city = current.city,
                province = current.province,
                postalCode = current.postalCode,
            ),
            logoBytes = logoBytes,
            fileName = form.logoFileName,
            mimeType = form.logoMimeType,
        )
        // A failed logo upload here is not fatal to account creation — the
        // owner can add a logo later from Settings; register() already
        // succeeded so we don't want to strand them on an error screen for it.
    }

    fun submitSignIn(onSuccess: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.login(state.signInEmail, state.signInPassword)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Couldn't sign in. Check your connection and try again.")
                    }
                }
        }
    }
}
