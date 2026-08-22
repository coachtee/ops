package com.ops.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.VisitEntity
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.JobRepository
import com.ops.app.data.repository.VisitRepository
import com.ops.coredomain.JobStatus
import com.ops.coredomain.VisitStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/** One visit row, pre-joined with the job title / customer it's against —
 * the schedule screen has no reason to make every row re-derive this. */
data class ScheduleVisitRow(
    val visit: VisitEntity,
    val jobTitle: String,
    val customerName: String,
)

data class ScheduleUiState(
    val overdue: List<ScheduleVisitRow> = emptyList(),
    val today: List<ScheduleVisitRow> = emptyList(),
    val upcoming: List<ScheduleVisitRow> = emptyList(),
    /** Jobs that are actual open work (not started/in progress) with zero
     * visits scheduled against them yet — the "you haven't scheduled this
     * yet" nudge. */
    val unscheduledJobs: List<JobEntity> = emptyList(),
)

private val OPEN_VISIT_STATUSES = setOf(VisitStatus.SCHEDULED.wire, VisitStatus.EN_ROUTE.wire, VisitStatus.IN_PROGRESS.wire, VisitStatus.NEEDS_FOLLOW_UP.wire)
private val OPEN_JOB_STATUSES = setOf(JobStatus.NOT_STARTED.wire, JobStatus.IN_PROGRESS.wire)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    visitRepository: VisitRepository,
    jobRepository: JobRepository,
    customerRepository: CustomerRepository,
) : ViewModel() {

    val uiState: StateFlow<ScheduleUiState> = combine(
        visitRepository.observeAll(),
        jobRepository.observeAll(),
        customerRepository.observeAll(),
    ) { visits, jobs, customers ->
        val jobsById = jobs.associateBy { it.id }
        val customersById = customers.associateBy { it.id }
        val today = LocalDate.now().toString()

        val openVisits = visits.filter { it.status in OPEN_VISIT_STATUSES }
        val rows = openVisits.mapNotNull { visit ->
            val job = jobsById[visit.jobId] ?: return@mapNotNull null
            val customer = customersById[job.customerId]
            ScheduleVisitRow(visit, job.number ?: job.title, customer?.name.orEmpty())
        }

        val scheduledJobIds = visits.map { it.jobId }.toSet()
        val unscheduledJobs = jobs.filter { it.status in OPEN_JOB_STATUSES && it.id !in scheduledJobIds }

        ScheduleUiState(
            overdue = rows.filter { it.visit.scheduledDate < today }.sortedBy { it.visit.scheduledDate },
            today = rows.filter { it.visit.scheduledDate == today }.sortedBy { it.visit.startTime ?: "" },
            upcoming = rows.filter { it.visit.scheduledDate > today }.sortedBy { it.visit.scheduledDate },
            unscheduledJobs = unscheduledJobs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())
}
