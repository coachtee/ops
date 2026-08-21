package com.ops.app.ui.leads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.repository.LeadRepository
import com.ops.coredomain.LeadSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewLeadUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val source: String = LeadSource.WHATSAPP.wire,
    val enquiry: String = "",
    val followUpDate: String? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && phone.isNotBlank()
}

@HiltViewModel
class NewLeadViewModel @Inject constructor(
    private val leadRepository: LeadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewLeadUiState())
    val uiState: StateFlow<NewLeadUiState> = _uiState.asStateFlow()

    fun update(transform: (NewLeadUiState) -> NewLeadUiState) = _uiState.update(transform)

    fun save(onSaved: (String) -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val id = leadRepository.create(
                name = state.name,
                phone = state.phone,
                email = state.email,
                source = state.source,
                enquiry = state.enquiry,
                followUpDate = state.followUpDate,
            )
            onSaved(id)
        }
    }
}
