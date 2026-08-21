package com.ops.app.ui.invoices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.BusinessEntity
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.InvoiceLineItemEntity
import com.ops.app.data.local.entities.PaymentEntity
import com.ops.app.data.repository.BusinessRepository
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.InvoiceRepository
import com.ops.app.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoicePreviewUiState(
    val invoice: InvoiceEntity? = null,
    val lineItems: List<InvoiceLineItemEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val customer: CustomerEntity? = null,
    val business: BusinessEntity? = null,
)

@HiltViewModel
class InvoicePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val invoiceRepository: InvoiceRepository,
    customerRepository: CustomerRepository,
    paymentRepository: PaymentRepository,
    businessRepository: BusinessRepository,
) : ViewModel() {

    val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])

    @OptIn(ExperimentalCoroutinesApi::class)
    private val customerFlow = invoiceRepository.observeById(invoiceId).flatMapLatest { invoice ->
        if (invoice == null) flowOf(null) else customerRepository.observeById(invoice.customerId)
    }

    val uiState: StateFlow<InvoicePreviewUiState> = combine(
        invoiceRepository.observeById(invoiceId),
        invoiceRepository.observeLineItems(invoiceId),
        paymentRepository.observeByInvoiceId(invoiceId),
        customerFlow,
        businessRepository.observe(),
    ) { invoice, lineItems, payments, customer, business ->
        InvoicePreviewUiState(invoice, lineItems.sortedBy { it.sortOrder }, payments, customer, business)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoicePreviewUiState())

    fun markSent() {
        viewModelScope.launch { invoiceRepository.markSent(invoiceId) }
    }
}
