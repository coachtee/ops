package com.ops.app.ui.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.InvoiceRepository
import com.ops.app.data.repository.JobRepository
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

data class JobDetailUiState(
    val job: JobEntity? = null,
    val customer: CustomerEntity? = null,
    val invoices: List<InvoiceEntity> = emptyList(),
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: JobRepository,
    customerRepository: CustomerRepository,
    invoiceRepository: InvoiceRepository,
) : ViewModel() {

    val jobId: String = checkNotNull(savedStateHandle["jobId"])

    @OptIn(ExperimentalCoroutinesApi::class)
    private val customerFlow = jobRepository.observeById(jobId).flatMapLatest { job ->
        if (job == null) flowOf(null) else customerRepository.observeById(job.customerId)
    }

    val uiState: StateFlow<JobDetailUiState> = combine(
        jobRepository.observeById(jobId),
        customerFlow,
        invoiceRepository.observeByJobId(jobId),
    ) { job, customer, invoices ->
        JobDetailUiState(job, customer, invoices)
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
