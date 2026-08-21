package com.ops.app.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.SupplierEntity
import com.ops.app.data.repository.ExpenseRepository
import com.ops.app.data.repository.JobRepository
import com.ops.app.data.repository.SupplierRepository
import com.ops.app.ui.navigation.OpsDestinations.orNull
import com.ops.coredomain.ExpenseCategory
import com.ops.coredomain.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class ExpenseEditUiState(
    val expenseId: String? = null,
    val jobId: String? = null,
    val supplierId: String? = null,
    val category: String = ExpenseCategory.OTHER.wire,
    val description: String = "",
    val amount: String = "",
    val isVatApplicable: Boolean = false,
    val date: String = LocalDate.now().toString(),
    val receiptUrl: String? = null,
    val localReceiptPath: String? = null,
    val receiptSyncState: String = ReceiptSyncState.NONE,
    val receiptSyncError: String? = null,
    val syncState: String? = null,
    val syncError: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val dateError: String? = null,
    val amountError: String? = null,
) {
    /** Instant offline-correct VAT split for the live "VAT: RXX.XX of this
     * total" line — see Money.extractVatFromInclusive's doc comment on why
     * this is extraction, not addition. */
    val vatAmount: BigDecimal
        get() = Money.extractVatFromInclusive(amount.toSafeBigDecimalOrNull() ?: BigDecimal.ZERO, isVatApplicable)
}

private fun String.toSafeBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

/**
 * One screen for create, edit, and view (delete + receipt capture live
 * here too) — same "always editable, no separate edit mode" spirit as
 * JobDetailScreen. Form fields are local mutable state (not a live Room
 * [kotlinx.coroutines.flow.Flow]) to avoid fighting Compose recomposition
 * while the owner is mid-type — the same reason InvoiceEditViewModel does
 * this. One deliberate consequence: background sync/receipt-upload
 * progress (SYNCING -> SYNCED, UPLOADING -> UPLOADED) isn't reflected live
 * on this screen while it stays open — the top-bar [com.ops.app.data.sync.SyncChipState]
 * still updates live regardless; this screen catches up on next open.
 */
@HiltViewModel
class ExpenseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    jobRepository: JobRepository,
    supplierRepository: SupplierRepository,
) : ViewModel() {

    private val routeExpenseId: String? = (savedStateHandle.get<String>("expenseId")).orNull()

    private val _uiState = MutableStateFlow(ExpenseEditUiState(expenseId = routeExpenseId))
    val uiState: StateFlow<ExpenseEditUiState> = _uiState.asStateFlow()

    val jobs: StateFlow<List<JobEntity>> = jobRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = supplierRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { loadInitial() }
    }

    private suspend fun loadInitial() {
        val id = routeExpenseId
        val existing = id?.let { expenseRepository.getById(it) }
        if (existing != null) {
            _uiState.update {
                it.copy(
                    expenseId = existing.id,
                    jobId = existing.jobId,
                    supplierId = existing.supplierId,
                    category = existing.category,
                    description = existing.description,
                    amount = existing.amount,
                    isVatApplicable = existing.isVatApplicable,
                    date = existing.date,
                    receiptUrl = existing.receiptUrl,
                    localReceiptPath = existing.localReceiptPath,
                    receiptSyncState = existing.receiptSyncState,
                    receiptSyncError = existing.receiptSyncError,
                    syncState = existing.syncState,
                    syncError = existing.syncError,
                    isLoading = false,
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun update(transform: (ExpenseEditUiState) -> ExpenseEditUiState) = _uiState.update(transform)

    /** Mirrors ExpenseSerializer.validate_amount/validate_date server-side —
     * instant feedback, not a substitute for the server's own check. */
    private fun validate(state: ExpenseEditUiState): ExpenseEditUiState {
        val amountValue = state.amount.toSafeBigDecimalOrNull()
        val amountError = when {
            amountValue == null -> "Enter an amount"
            amountValue <= BigDecimal.ZERO -> "Amount must be greater than zero"
            else -> null
        }
        val dateError = runCatching { LocalDate.parse(state.date) }
            .fold(
                onSuccess = { parsed -> if (parsed.isAfter(LocalDate.now().plusDays(1))) "Expense date can't be in the future" else null },
                onFailure = { "Enter a valid date" },
            )
        return state.copy(amountError = amountError, dateError = dateError)
    }

    fun save(onSaved: () -> Unit) {
        val validated = validate(_uiState.value)
        _uiState.value = validated
        if (validated.amountError != null || validated.dateError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val id = expenseRepository.save(
                id = validated.expenseId,
                jobId = validated.jobId,
                supplierId = validated.supplierId,
                category = validated.category,
                description = validated.description,
                amount = validated.amount.toSafeBigDecimalOrNull() ?: BigDecimal.ZERO,
                isVatApplicable = validated.isVatApplicable,
                date = validated.date,
            )
            _uiState.update { it.copy(isSaving = false, expenseId = id) }
            onSaved()
        }
    }

    /** [localFilePath] is already a permanent, app-private copy of a
     * captured/picked photo — see ExpenseEditScreen, which does the actual
     * capture/copy before calling this. */
    fun attachReceipt(localFilePath: String) {
        _uiState.update { it.copy(localReceiptPath = localFilePath, receiptSyncState = ReceiptSyncState.PENDING, receiptSyncError = null) }
        val id = _uiState.value.expenseId ?: return
        viewModelScope.launch { expenseRepository.attachReceipt(id, localFilePath) }
    }

    fun retryReceiptUpload() {
        val id = _uiState.value.expenseId ?: return
        viewModelScope.launch { expenseRepository.retryReceipt(id) }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.expenseId ?: return
        viewModelScope.launch {
            expenseRepository.delete(id)
            onDeleted()
        }
    }
}
