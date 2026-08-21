package com.ops.app.ui.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.ExpenseEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.PaymentEntity
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.ExpenseRepository
import com.ops.app.data.repository.InvoiceRepository
import com.ops.app.data.repository.JobRepository
import com.ops.app.data.repository.PaymentRepository
import com.ops.app.data.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** [jobValue]/[amountPaid]/[outstanding] make the job's money story visible
 * in one place (the user's explicit "Job value / Paid / Outstanding" ask):
 * value comes from the job's invoice(s) once any exist (a job can have more
 * than one), falling back to its source quote before the first invoice is
 * raised; paid/outstanding are derived from each invoice's own running
 * `amountPaid` — the same figure the Invoice screen already keeps current
 * after every payment — rather than re-summing [payments] here, so the two
 * screens can never disagree. */
data class JobDetailUiState(
    val job: JobEntity? = null,
    val customer: CustomerEntity? = null,
    val quote: QuoteEntity? = null,
    val invoices: List<InvoiceEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
) {
    val jobValue: BigDecimal
        get() = if (invoices.isNotEmpty()) {
            invoices.fold(BigDecimal.ZERO) { acc, inv -> acc.add(inv.total.toSafeBigDecimal()) }
        } else {
            quote?.total?.toSafeBigDecimal() ?: BigDecimal.ZERO
        }

    val amountPaid: BigDecimal
        get() = invoices.fold(BigDecimal.ZERO) { acc, inv -> acc.add(inv.amountPaid.toSafeBigDecimal()) }

    val outstanding: BigDecimal
        get() = jobValue.subtract(amountPaid).max(BigDecimal.ZERO)
}

private fun String.toSafeBigDecimal(): BigDecimal = runCatching { BigDecimal(this) }.getOrDefault(BigDecimal.ZERO)

private data class JobDetailCore(
    val job: JobEntity?,
    val customer: CustomerEntity?,
    val quote: QuoteEntity?,
    val invoices: List<InvoiceEntity>,
    val payments: List<PaymentEntity>,
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: JobRepository,
    customerRepository: CustomerRepository,
    quoteRepository: QuoteRepository,
    invoiceRepository: InvoiceRepository,
    paymentRepository: PaymentRepository,
    expenseRepository: ExpenseRepository,
) : ViewModel() {

    val jobId: String = checkNotNull(savedStateHandle["jobId"])

    @OptIn(ExperimentalCoroutinesApi::class)
    private val customerFlow = jobRepository.observeById(jobId).flatMapLatest { job ->
        if (job == null) flowOf(null) else customerRepository.observeById(job.customerId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val quoteFlow = jobRepository.observeById(jobId).flatMapLatest { job ->
        val quoteId = job?.quoteId
        if (quoteId == null) flowOf(null) else quoteRepository.observeById(quoteId)
    }

    private val invoicesFlow = invoiceRepository.observeByJobId(jobId)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val paymentsFlow = invoicesFlow.flatMapLatest { invoices ->
        if (invoices.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(invoices.map { paymentRepository.observeByInvoiceId(it.id) }) { arrays -> arrays.toList().flatten() }
        }
    }

    private val coreFlow = combine(
        jobRepository.observeById(jobId),
        customerFlow,
        quoteFlow,
        invoicesFlow,
        paymentsFlow,
    ) { job, customer, quote, invoices, payments -> JobDetailCore(job, customer, quote, invoices, payments) }

    val uiState: StateFlow<JobDetailUiState> = combine(
        coreFlow,
        expenseRepository.observeByJobId(jobId),
    ) { core, expenses ->
        JobDetailUiState(core.job, core.customer, core.quote, core.invoices, core.payments, expenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobDetailUiState())

    fun updateStatus(status: String) {
        viewModelScope.launch { jobRepository.updateStatus(jobId, status) }
    }

    fun updateDates(startDate: String?, dueDate: String?) {
        viewModelScope.launch {
            val existing = jobRepository.getById(jobId) ?: return@launch
            jobRepository.save(existing.copy(startDate = startDate, dueDate = dueDate))
        }
    }
}
