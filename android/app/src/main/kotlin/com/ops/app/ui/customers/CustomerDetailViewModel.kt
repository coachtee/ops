package com.ops.app.ui.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.InvoiceRepository
import com.ops.app.data.repository.JobRepository
import com.ops.app.data.repository.QuoteRepository
import com.ops.coredomain.InvoiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class CustomerDetailUiState(
    val customer: CustomerEntity? = null,
    val quotes: List<QuoteEntity> = emptyList(),
    val jobs: List<JobEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val outstandingTotal: BigDecimal = BigDecimal.ZERO,
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customerRepository: CustomerRepository,
    quoteRepository: QuoteRepository,
    jobRepository: JobRepository,
    invoiceRepository: InvoiceRepository,
) : ViewModel() {

    val customerId: String = checkNotNull(savedStateHandle["customerId"])

    val uiState: StateFlow<CustomerDetailUiState> = combine(
        customerRepository.observeById(customerId),
        quoteRepository.observeByCustomerId(customerId),
        jobRepository.observeByCustomerId(customerId),
        invoiceRepository.observeByCustomerId(customerId),
    ) { customer, quotes, jobs, invoices ->
        val outstanding = invoices
            .filter { it.status != InvoiceStatus.PAID.wire && it.status != InvoiceStatus.CANCELLED.wire }
            .fold(BigDecimal.ZERO) { acc, inv ->
                val total = runCatching { BigDecimal(inv.total) }.getOrDefault(BigDecimal.ZERO)
                val paid = runCatching { BigDecimal(inv.amountPaid) }.getOrDefault(BigDecimal.ZERO)
                acc.add(total.subtract(paid))
            }
        CustomerDetailUiState(customer, quotes, jobs, invoices, outstanding)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerDetailUiState())

    fun updateNotes(notes: String) {
        viewModelScope.launch { customerRepository.updateNotes(customerId, notes) }
    }
}
