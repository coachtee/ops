package com.ops.app.ui.suppliers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.ExpenseEntity
import com.ops.app.data.repository.ExpenseRepository
import com.ops.app.data.repository.SupplierRepository
import com.ops.app.ui.navigation.OpsDestinations.orNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierEditUiState(
    val supplierId: String? = null,
    val name: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    val syncState: String? = null,
    val syncError: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val nameError: String? = null,
)

/** One screen for create, edit, and view (delete too) — same "always
 * editable, no separate edit mode" spirit as ExpenseEditScreen. Once a
 * supplier is saved, its expense history (what's actually been bought from
 * them) is shown read-only below the form — that's the whole point of
 * linking Expense.supplier_id instead of building a separate ledger. */
@HiltViewModel
class SupplierEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val supplierRepository: SupplierRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val routeSupplierId: String? = (savedStateHandle.get<String>("supplierId")).orNull()

    private val _uiState = MutableStateFlow(SupplierEditUiState(supplierId = routeSupplierId))
    val uiState: StateFlow<SupplierEditUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkedExpenses: StateFlow<List<ExpenseEntity>> = _uiState
        .map { it.supplierId }
        .distinctUntilChanged()
        .flatMapLatest { supplierId ->
            supplierId?.let { expenseRepository.observeBySupplierId(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { loadInitial() }
    }

    private suspend fun loadInitial() {
        val id = routeSupplierId
        val existing = id?.let { supplierRepository.getById(it) }
        if (existing != null) {
            _uiState.update {
                it.copy(
                    supplierId = existing.id,
                    name = existing.name,
                    contactPerson = existing.contactPerson,
                    phone = existing.phone,
                    email = existing.email,
                    notes = existing.notes,
                    syncState = existing.syncState,
                    syncError = existing.syncError,
                    isLoading = false,
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun update(transform: (SupplierEditUiState) -> SupplierEditUiState) = _uiState.update(transform)

    /** Mirrors SupplierSerializer.validate_name server-side. */
    private fun validate(state: SupplierEditUiState): SupplierEditUiState =
        state.copy(nameError = if (state.name.isBlank()) "Supplier name is required" else null)

    fun save(onSaved: () -> Unit) {
        val validated = validate(_uiState.value)
        _uiState.value = validated
        if (validated.nameError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val id = supplierRepository.save(
                id = validated.supplierId,
                name = validated.name,
                contactPerson = validated.contactPerson,
                phone = validated.phone,
                email = validated.email,
                notes = validated.notes,
            )
            _uiState.update { it.copy(isSaving = false, supplierId = id) }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.supplierId ?: return
        viewModelScope.launch {
            supplierRepository.delete(id)
            onDeleted()
        }
    }
}
