package com.ops.app.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.EmployeeEntity
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.VisitEntity
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.EmployeeRepository
import com.ops.app.data.repository.JobRepository
import com.ops.app.data.repository.VisitRepository
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

data class VisitDetailUiState(
    val visit: VisitEntity? = null,
    val job: JobEntity? = null,
    val customer: CustomerEntity? = null,
    val employee: EmployeeEntity? = null,
)

@HiltViewModel
class VisitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val visitRepository: VisitRepository,
    jobRepository: JobRepository,
    customerRepository: CustomerRepository,
    employeeRepository: EmployeeRepository,
) : ViewModel() {

    val visitId: String = checkNotNull(savedStateHandle["visitId"])

    @OptIn(ExperimentalCoroutinesApi::class)
    private val jobFlow = visitRepository.observeById(visitId).flatMapLatest { visit ->
        if (visit == null) flowOf(null) else jobRepository.observeById(visit.jobId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val customerFlow = jobFlow.flatMapLatest { job ->
        if (job == null) flowOf(null) else customerRepository.observeById(job.customerId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val employeeFlow = visitRepository.observeById(visitId).flatMapLatest { visit ->
        val employeeId = visit?.employeeId
        if (employeeId == null) {
            flowOf(null)
        } else {
            employeeRepository.observeAll().flatMapLatest { list -> flowOf(list.firstOrNull { it.id == employeeId }) }
        }
    }

    val uiState: StateFlow<VisitDetailUiState> = combine(
        visitRepository.observeById(visitId),
        jobFlow,
        customerFlow,
        employeeFlow,
    ) { visit, job, customer, employee -> VisitDetailUiState(visit, job, customer, employee) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VisitDetailUiState())

    fun start() {
        viewModelScope.launch { visitRepository.start(visitId) }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch { visitRepository.updateStatus(visitId, status) }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch { visitRepository.updateNotes(visitId, notes) }
    }

    fun complete() {
        viewModelScope.launch { visitRepository.complete(visitId) }
    }

    fun attachPhoto(localPath: String) {
        viewModelScope.launch { visitRepository.attachPhoto(visitId, localPath) }
    }

    fun retryPhoto() {
        viewModelScope.launch { visitRepository.retryPhoto(visitId) }
    }
}
