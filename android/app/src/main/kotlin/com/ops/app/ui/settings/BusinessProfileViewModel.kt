package com.ops.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.BusinessEntity
import com.ops.app.data.remote.dto.BusinessPatchDto
import com.ops.app.data.repository.AuthRepository
import com.ops.app.data.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val business: StateFlow<BusinessEntity?> = businessRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun save(fields: BusinessPatchDto, logoBytes: ByteArray?, logoMimeType: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            val result = if (logoBytes != null) {
                businessRepository.updateProfileWithLogo(fields, logoBytes, "logo.jpg", logoMimeType ?: "image/jpeg")
            } else {
                businessRepository.updateProfile(fields)
            }
            _isSaving.value = false
            result.onSuccess { onDone() }
                .onFailure { _errorMessage.value = it.message ?: "Couldn't save. Check your connection and try again." }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
