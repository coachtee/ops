package com.ops.app.ui.invoices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.InvoiceLineItemInput
import com.ops.app.data.repository.InvoiceRepository
import com.ops.app.data.repository.QuoteRepository
import com.ops.app.ui.navigation.OpsDestinations.orNull
import com.ops.coredomain.DocumentTotals
import com.ops.coredomain.InvoiceStatus
import com.ops.coredomain.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class InvoiceLineItemRow(
    val rowKey: String = UUID.randomUUID().toString(),
    val id: String? = null,
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "0.00",
)

data class InvoiceEditUiState(
    val invoiceId: String? = null,
    val customerId: String = "",
    val jobId: String? = null,
    val quoteId: String? = null,
    val customerName: String = "",
    val number: String? = null,
    val status: String = InvoiceStatus.DRAFT.wire,
    val issueDate: String = LocalDate.now().toString(),
    val dueDate: String? = null,
    val notes: String = "",
    val terms: String = "",
    val isVatApplicable: Boolean = true,
    val discountAmount: String = "0.00",
    val lineItems: List<InvoiceLineItemRow> = listOf(InvoiceLineItemRow()),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
) {
    val totals: DocumentTotals
        get() {
            val lineTotals = lineItems.map { Money.computeLineTotal(it.quantity.toSafeBigDecimal(), it.unitPrice.toSafeBigDecimal()) }
            return Money.computeDocumentTotals(lineTotals, discountAmount.toSafeBigDecimal(), isVatApplicable)
        }
}

private fun String.toSafeBigDecimal(): BigDecimal = runCatching { BigDecimal(ifBlank { "0" }) }.getOrDefault(BigDecimal.ZERO)

@HiltViewModel
class InvoiceEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val invoiceRepository: InvoiceRepository,
    private val quoteRepository: QuoteRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val routeInvoiceId: String? = (savedStateHandle.get<String>("invoiceId")).orNull()
    private val routeCustomerId: String? = (savedStateHandle.get<String>("customerId")).orNull()
    private val routeJobId: String? = (savedStateHandle.get<String>("jobId")).orNull()
    private val routeQuoteId: String? = (savedStateHandle.get<String>("quoteId")).orNull()

    private val _uiState = MutableStateFlow(
        InvoiceEditUiState(invoiceId = routeInvoiceId, customerId = routeCustomerId.orEmpty(), jobId = routeJobId, quoteId = routeQuoteId),
    )
    val uiState: StateFlow<InvoiceEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadInitial() }
    }

    private suspend fun loadInitial() {
        val invoiceId = routeInvoiceId
        if (invoiceId != null) {
            val invoice = invoiceRepository.getById(invoiceId)
            val items = invoiceRepository.getLineItems(invoiceId)
            if (invoice != null) {
                val customer = customerRepository.getById(invoice.customerId)
                _uiState.update {
                    it.copy(
                        invoiceId = invoice.id,
                        customerId = invoice.customerId,
                        jobId = invoice.jobId,
                        quoteId = invoice.quoteId,
                        customerName = customer?.name.orEmpty(),
                        number = invoice.number,
                        status = invoice.status,
                        issueDate = invoice.issueDate,
                        dueDate = invoice.dueDate,
                        notes = invoice.notes,
                        terms = invoice.terms,
                        isVatApplicable = invoice.isVatApplicable,
                        discountAmount = invoice.discountAmount,
                        lineItems = if (items.isEmpty()) listOf(InvoiceLineItemRow()) else items.map { li ->
                            InvoiceLineItemRow(id = li.id, description = li.description, quantity = li.quantity, unitPrice = li.unitPrice)
                        },
                        isLoading = false,
                    )
                }
                return
            }
        }

        // New invoice: prefill customer name, and line items from the source
        // quote if one is known (from a job that came from a quote, or a
        // quote's own "Create invoice" — see IA: "New/edit invoice (from
        // job, with line items pre-filled from job/quote)").
        val customer = routeCustomerId?.let { customerRepository.getById(it) }
        val prefillItems = routeQuoteId?.let { quoteId ->
            val quoteItems = quoteRepository.getLineItems(quoteId)
            invoiceRepository.toLineItemInputsFromQuote(quoteItems).map { input ->
                InvoiceLineItemRow(description = input.description, quantity = input.quantity.toPlainString(), unitPrice = input.unitPrice.toPlainString())
            }
        }
        _uiState.update {
            it.copy(
                customerName = customer?.name.orEmpty(),
                lineItems = prefillItems?.takeIf { rows -> rows.isNotEmpty() } ?: listOf(InvoiceLineItemRow()),
                isLoading = false,
            )
        }
    }

    fun update(transform: (InvoiceEditUiState) -> InvoiceEditUiState) = _uiState.update(transform)

    fun updateLineItem(rowKey: String, transform: (InvoiceLineItemRow) -> InvoiceLineItemRow) {
        _uiState.update { state -> state.copy(lineItems = state.lineItems.map { if (it.rowKey == rowKey) transform(it) else it }) }
    }

    fun addLineItem() = _uiState.update { it.copy(lineItems = it.lineItems + InvoiceLineItemRow()) }

    fun removeLineItem(rowKey: String) = _uiState.update { state ->
        val remaining = state.lineItems.filterNot { it.rowKey == rowKey }
        state.copy(lineItems = remaining.ifEmpty { listOf(InvoiceLineItemRow()) })
    }

    fun save(onSaved: (String) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val inputs = state.lineItems
                .filter { it.description.isNotBlank() }
                .map { InvoiceLineItemInput(it.id, it.description, it.quantity.toSafeBigDecimal(), it.unitPrice.toSafeBigDecimal()) }
            val id = invoiceRepository.saveInvoice(
                invoiceId = state.invoiceId,
                customerId = state.customerId,
                jobId = state.jobId,
                quoteId = state.quoteId,
                issueDate = state.issueDate,
                dueDate = state.dueDate,
                notes = state.notes,
                terms = state.terms,
                isVatApplicable = state.isVatApplicable,
                discountAmount = state.discountAmount.toSafeBigDecimal(),
                status = state.status,
                lineItems = inputs,
            )
            _uiState.update { it.copy(isSaving = false, invoiceId = id) }
            onSaved(id)
        }
    }
}
