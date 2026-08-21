package com.ops.app.ui.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.InvoiceRepository
import com.ops.app.data.repository.PaymentRepository
import com.ops.app.ui.navigation.OpsDestinations.orNull
import com.ops.coredomain.PaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class RecordPaymentUiState(
    val customerId: String = "",
    val invoiceId: String? = null,
    val customerName: String = "",
    val invoiceNumber: String? = null,
    val invoiceTotal: BigDecimal? = null,
    val alreadyPaid: BigDecimal? = null,
    val outstandingOnInvoice: BigDecimal? = null,
    val amount: String = "",
    val method: String = PaymentMethod.EFT.wire,
    val reference: String = "",
    val paidDate: String = LocalDate.now().toString(),
    val notes: String = "",
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = runCatching { BigDecimal(amount) }.getOrNull()?.let { it.signum() > 0 } == true

    private val amountAsBigDecimal: BigDecimal
        get() = runCatching { BigDecimal(amount) }.getOrDefault(BigDecimal.ZERO)

    /** Recomputed on every keystroke — this is a plain getter over
     * [outstandingOnInvoice] and [amount], not a value cached at load, so
     * the "Remaining" figure the user sees always reflects what they just
     * typed, with no extra wiring needed. */
    val remainingAfterThisPayment: BigDecimal?
        get() = outstandingOnInvoice?.subtract(amountAsBigDecimal)
}

@HiltViewModel
class RecordPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val paymentRepository: PaymentRepository,
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository,
) : ViewModel() {

    private val customerId: String = checkNotNull(savedStateHandle["customerId"])
    private val routeInvoiceId: String? = (savedStateHandle.get<String>("invoiceId")).orNull()

    private val _uiState = MutableStateFlow(RecordPaymentUiState(customerId = customerId, invoiceId = routeInvoiceId))
    val uiState: StateFlow<RecordPaymentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val customer = customerRepository.getById(customerId)
            val invoice = routeInvoiceId?.let { invoiceRepository.getById(it) }
            val total = invoice?.let { runCatching { BigDecimal(it.total) }.getOrNull() }
            val alreadyPaid = invoice?.let { runCatching { BigDecimal(it.amountPaid) }.getOrNull() }
            val outstanding = if (total != null && alreadyPaid != null) total.subtract(alreadyPaid) else null
            _uiState.update {
                it.copy(
                    customerName = customer?.name.orEmpty(),
                    invoiceNumber = invoice?.number,
                    invoiceTotal = total,
                    alreadyPaid = alreadyPaid,
                    outstandingOnInvoice = outstanding,
                    amount = outstanding?.toPlainString() ?: it.amount,
                )
            }
        }
    }

    fun update(transform: (RecordPaymentUiState) -> RecordPaymentUiState) = _uiState.update(transform)

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            paymentRepository.record(
                customerId = state.customerId,
                invoiceId = state.invoiceId,
                amount = BigDecimal(state.amount),
                method = state.method,
                reference = state.reference,
                paidDate = state.paidDate,
                notes = state.notes,
            )
            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}
