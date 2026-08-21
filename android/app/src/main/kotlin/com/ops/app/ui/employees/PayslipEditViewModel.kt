package com.ops.app.ui.employees

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.repository.EmployeeRepository
import com.ops.app.data.repository.PayslipRepository
import com.ops.app.ui.navigation.OpsDestinations.orNull
import com.ops.coredomain.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class PayslipEditUiState(
    val payslipId: String? = null,
    val employeeId: String = "",
    val employeeName: String = "",
    val periodStart: String = LocalDate.now().minusDays(6).toString(),
    val periodEnd: String = LocalDate.now().toString(),
    val grossPay: String = "",
    val deductions: String = "0.00",
    val deductionsNote: String = "",
    val paidDate: String? = null,
    val notes: String = "",
    val syncState: String? = null,
    val syncError: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val grossPayError: String? = null,
    val deductionsError: String? = null,
    val periodError: String? = null,
) {
    /** Instant offline-correct net pay for the live "Net pay: RXX.XX" line —
     * see Money.computeNetPay's doc comment: always derived, never entered
     * by hand, no PAYE/UIF tax-table computation anywhere in this app. */
    val netPay: BigDecimal
        get() = Money.computeNetPay(
            grossPay.toSafeBigDecimalOrNull() ?: BigDecimal.ZERO,
            deductions.toSafeBigDecimalOrNull() ?: BigDecimal.ZERO,
        )
}

private fun String.toSafeBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

/**
 * One screen for create, edit, and view (delete + "mark as paid" + share
 * all live here too) — same "always editable, no separate edit mode"
 * spirit as ExpenseEditScreen. Reached only from an employee's own screen
 * (EmployeeEditScreen), so [employeeId] is always known up front, unlike
 * Expense's optional job/supplier links.
 */
@HiltViewModel
class PayslipEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val payslipRepository: PayslipRepository,
    private val employeeRepository: EmployeeRepository,
) : ViewModel() {

    private val routeEmployeeId: String = checkNotNull(savedStateHandle["employeeId"])
    private val routePayslipId: String? = (savedStateHandle.get<String>("payslipId")).orNull()

    private val _uiState = MutableStateFlow(
        PayslipEditUiState(payslipId = routePayslipId, employeeId = routeEmployeeId),
    )
    val uiState: StateFlow<PayslipEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadInitial() }
    }

    private suspend fun loadInitial() {
        val employee = employeeRepository.getById(routeEmployeeId)
        val existing = routePayslipId?.let { payslipRepository.getById(it) }
        if (existing != null) {
            _uiState.update {
                it.copy(
                    payslipId = existing.id,
                    employeeName = employee?.name.orEmpty(),
                    periodStart = existing.periodStart,
                    periodEnd = existing.periodEnd,
                    grossPay = existing.grossPay,
                    deductions = existing.deductions,
                    deductionsNote = existing.deductionsNote,
                    paidDate = existing.paidDate,
                    notes = existing.notes,
                    syncState = existing.syncState,
                    syncError = existing.syncError,
                    isLoading = false,
                )
            }
        } else {
            _uiState.update { it.copy(employeeName = employee?.name.orEmpty(), isLoading = false) }
        }
    }

    fun update(transform: (PayslipEditUiState) -> PayslipEditUiState) = _uiState.update(transform)

    /** Mirrors PayslipSerializer's validate_gross_pay/validate_deductions/validate
     * server-side — instant feedback, not a substitute for the server's own check. */
    private fun validate(state: PayslipEditUiState): PayslipEditUiState {
        val grossPayValue = state.grossPay.toSafeBigDecimalOrNull()
        val grossPayError = when {
            grossPayValue == null -> "Enter the gross pay"
            grossPayValue <= BigDecimal.ZERO -> "Gross pay must be greater than zero"
            else -> null
        }
        val deductionsValue = state.deductions.toSafeBigDecimalOrNull()
        val deductionsError = when {
            deductionsValue == null -> "Enter a deductions amount, or 0"
            deductionsValue < BigDecimal.ZERO -> "Deductions can't be negative"
            grossPayValue != null && deductionsValue > grossPayValue -> "Deductions can't be more than gross pay"
            else -> null
        }
        val periodError = runCatching { LocalDate.parse(state.periodStart) to LocalDate.parse(state.periodEnd) }
            .fold(
                onSuccess = { (start, end) -> if (end.isBefore(start)) "Period end can't be before period start" else null },
                onFailure = { "Enter valid period dates" },
            )
        return state.copy(grossPayError = grossPayError, deductionsError = deductionsError, periodError = periodError)
    }

    fun save(onSaved: () -> Unit) {
        val validated = validate(_uiState.value)
        _uiState.value = validated
        if (validated.grossPayError != null || validated.deductionsError != null || validated.periodError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val id = payslipRepository.save(
                id = validated.payslipId,
                employeeId = validated.employeeId,
                periodStart = validated.periodStart,
                periodEnd = validated.periodEnd,
                grossPay = validated.grossPay.toSafeBigDecimalOrNull() ?: BigDecimal.ZERO,
                deductions = validated.deductions.toSafeBigDecimalOrNull() ?: BigDecimal.ZERO,
                deductionsNote = validated.deductionsNote,
                paidDate = validated.paidDate,
                notes = validated.notes,
            )
            _uiState.update { it.copy(isSaving = false, payslipId = id) }
            onSaved()
        }
    }

    fun markPaidToday() {
        update { it.copy(paidDate = LocalDate.now().toString()) }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.payslipId ?: return
        viewModelScope.launch {
            payslipRepository.delete(id)
            onDeleted()
        }
    }
}
