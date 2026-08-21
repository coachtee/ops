package com.ops.app.ui.employees

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.PayslipEntity
import com.ops.app.data.repository.EmployeeRepository
import com.ops.app.data.repository.PayslipRepository
import com.ops.app.ui.navigation.OpsDestinations.orNull
import com.ops.coredomain.PayRateType
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

data class EmployeeEditUiState(
    val employeeId: String? = null,
    val name: String = "",
    val role: String = "",
    val phone: String = "",
    val email: String = "",
    val payRateType: String = PayRateType.MONTHLY.wire,
    val payRate: String = "",
    val startDate: String? = null,
    val notes: String = "",
    val syncState: String? = null,
    val syncError: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val nameError: String? = null,
)

/** One screen for create, edit, and view (delete too) — same "always
 * editable, no separate edit mode" spirit as SupplierEditScreen. Once an
 * employee is saved, their payslip history is shown below the form (see
 * [linkedPayslips]), with a "+ New payslip" action. */
@HiltViewModel
class EmployeeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val employeeRepository: EmployeeRepository,
    private val payslipRepository: PayslipRepository,
) : ViewModel() {

    private val routeEmployeeId: String? = (savedStateHandle.get<String>("employeeId")).orNull()

    private val _uiState = MutableStateFlow(EmployeeEditUiState(employeeId = routeEmployeeId))
    val uiState: StateFlow<EmployeeEditUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkedPayslips: StateFlow<List<PayslipEntity>> = _uiState
        .map { it.employeeId }
        .distinctUntilChanged()
        .flatMapLatest { employeeId ->
            employeeId?.let { payslipRepository.observeByEmployeeId(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { loadInitial() }
    }

    private suspend fun loadInitial() {
        val id = routeEmployeeId
        val existing = id?.let { employeeRepository.getById(it) }
        if (existing != null) {
            _uiState.update {
                it.copy(
                    employeeId = existing.id,
                    name = existing.name,
                    role = existing.role,
                    phone = existing.phone,
                    email = existing.email,
                    payRateType = existing.payRateType,
                    payRate = existing.payRate,
                    startDate = existing.startDate,
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

    fun update(transform: (EmployeeEditUiState) -> EmployeeEditUiState) = _uiState.update(transform)

    /** Mirrors EmployeeSerializer.validate_name server-side. */
    private fun validate(state: EmployeeEditUiState): EmployeeEditUiState =
        state.copy(nameError = if (state.name.isBlank()) "Employee name is required" else null)

    fun save(onSaved: () -> Unit) {
        val validated = validate(_uiState.value)
        _uiState.value = validated
        if (validated.nameError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val id = employeeRepository.save(
                id = validated.employeeId,
                name = validated.name,
                role = validated.role,
                phone = validated.phone,
                email = validated.email,
                payRateType = validated.payRateType,
                payRate = validated.payRate.ifBlank { "0.00" },
                startDate = validated.startDate,
                notes = validated.notes,
            )
            _uiState.update { it.copy(isSaving = false, employeeId = id) }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.employeeId ?: return
        viewModelScope.launch {
            employeeRepository.delete(id)
            onDeleted()
        }
    }
}
