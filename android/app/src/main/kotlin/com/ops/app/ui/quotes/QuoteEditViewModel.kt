package com.ops.app.ui.quotes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.QuoteLineItemInput
import com.ops.app.data.repository.QuoteRepository
import com.ops.app.ui.navigation.OpsDestinations.orNull
import com.ops.coredomain.DocumentTotals
import com.ops.coredomain.Money
import com.ops.coredomain.QuoteStatus
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

/** One line item row as edited on screen — [rowKey] is a stable UI-only key
 * (never sent anywhere); [id] is the underlying [com.ops.app.data.local.entities.QuoteLineItemEntity]
 * id, null for a row not yet saved. */
data class QuoteLineItemRow(
    val rowKey: String = UUID.randomUUID().toString(),
    val id: String? = null,
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "0.00",
)

data class QuoteEditUiState(
    val quoteId: String? = null,
    val customerId: String = "",
    val leadId: String? = null,
    val customerName: String = "",
    val number: String? = null,
    val status: String = QuoteStatus.DRAFT.wire,
    val issueDate: String = LocalDate.now().toString(),
    val validUntil: String? = null,
    val notes: String = "",
    val terms: String = "",
    val isVatApplicable: Boolean = true,
    val discountAmount: String = "0.00",
    val lineItems: List<QuoteLineItemRow> = listOf(QuoteLineItemRow()),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
) {
    val totals: DocumentTotals
        get() {
            val lineTotals = lineItems.map {
                Money.computeLineTotal(it.quantity.toSafeBigDecimal(), it.unitPrice.toSafeBigDecimal())
            }
            return Money.computeDocumentTotals(lineTotals, discountAmount.toSafeBigDecimal(), isVatApplicable)
        }
}

private fun String.toSafeBigDecimal(): BigDecimal = runCatching { BigDecimal(ifBlank { "0" }) }.getOrDefault(BigDecimal.ZERO)

@HiltViewModel
class QuoteEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val routeQuoteId: String? = (savedStateHandle.get<String>("quoteId")).orNull()
    private val routeCustomerId: String? = (savedStateHandle.get<String>("customerId")).orNull()
    private val routeLeadId: String? = (savedStateHandle.get<String>("leadId")).orNull()

    private val _uiState = MutableStateFlow(QuoteEditUiState(quoteId = routeQuoteId, customerId = routeCustomerId.orEmpty(), leadId = routeLeadId))
    val uiState: StateFlow<QuoteEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadInitial() }
    }

    private suspend fun loadInitial() {
        val quoteId = routeQuoteId
        if (quoteId != null) {
            val quote = quoteRepository.getById(quoteId)
            val items = quoteRepository.getLineItems(quoteId)
            if (quote != null) {
                val customer = customerRepository.getById(quote.customerId)
                _uiState.update {
                    it.copy(
                        quoteId = quote.id,
                        customerId = quote.customerId,
                        leadId = quote.leadId,
                        customerName = customer?.name.orEmpty(),
                        number = quote.number,
                        status = quote.status,
                        issueDate = quote.issueDate,
                        validUntil = quote.validUntil,
                        notes = quote.notes,
                        terms = quote.terms,
                        isVatApplicable = quote.isVatApplicable,
                        discountAmount = quote.discountAmount,
                        lineItems = if (items.isEmpty()) listOf(QuoteLineItemRow()) else items.map { li ->
                            QuoteLineItemRow(id = li.id, description = li.description, quantity = li.quantity, unitPrice = li.unitPrice)
                        },
                        isLoading = false,
                    )
                }
                return
            }
        }
        val customer = routeCustomerId?.let { customerRepository.getById(it) }
        _uiState.update { it.copy(customerName = customer?.name.orEmpty(), isLoading = false) }
    }

    fun update(transform: (QuoteEditUiState) -> QuoteEditUiState) = _uiState.update(transform)

    fun updateLineItem(rowKey: String, transform: (QuoteLineItemRow) -> QuoteLineItemRow) {
        _uiState.update { state -> state.copy(lineItems = state.lineItems.map { if (it.rowKey == rowKey) transform(it) else it }) }
    }

    fun addLineItem() = _uiState.update { it.copy(lineItems = it.lineItems + QuoteLineItemRow()) }

    fun removeLineItem(rowKey: String) = _uiState.update { state ->
        val remaining = state.lineItems.filterNot { it.rowKey == rowKey }
        state.copy(lineItems = remaining.ifEmpty { listOf(QuoteLineItemRow()) })
    }

    fun save(asStatus: String? = null, onSaved: (String) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val inputs = state.lineItems
                .filter { it.description.isNotBlank() }
                .map { QuoteLineItemInput(it.id, it.description, it.quantity.toSafeBigDecimal(), it.unitPrice.toSafeBigDecimal()) }
            val id = quoteRepository.saveQuote(
                quoteId = state.quoteId,
                customerId = state.customerId,
                leadId = state.leadId,
                issueDate = state.issueDate,
                validUntil = state.validUntil,
                notes = state.notes,
                terms = state.terms,
                isVatApplicable = state.isVatApplicable,
                discountAmount = state.discountAmount.toSafeBigDecimal(),
                status = asStatus ?: state.status,
                lineItems = inputs,
            )
            _uiState.update { it.copy(isSaving = false, quoteId = id) }
            onSaved(id)
        }
    }
}
