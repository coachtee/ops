package com.ops.app.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.repository.CustomerRepository
import com.ops.coredomain.CustomerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewCustomerUiState(
    val name: String = "",
    val customerType: String = CustomerType.INDIVIDUAL.wire,
    val phone: String = "",
    val email: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val suburb: String = "",
    val city: String = "",
    val province: String = "",
    val postalCode: String = "",
    val notes: String = "",
) {
    val canSave: Boolean get() = name.isNotBlank() && phone.isNotBlank()
}

@HiltViewModel
class NewCustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewCustomerUiState())
    val uiState: StateFlow<NewCustomerUiState> = _uiState.asStateFlow()

    fun update(transform: (NewCustomerUiState) -> NewCustomerUiState) = _uiState.update(transform)

    fun save(onSaved: (String) -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val id = customerRepository.create(
                name = state.name,
                customerType = state.customerType,
                phone = state.phone,
                email = state.email,
                addressLine1 = state.addressLine1,
                addressLine2 = state.addressLine2,
                suburb = state.suburb,
                city = state.city,
                province = state.province,
                postalCode = state.postalCode,
                notes = state.notes,
            )
            onSaved(id)
        }
    }
}
