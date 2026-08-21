package com.ops.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.LeadEntity
import com.ops.app.data.repository.BusinessRepository
import com.ops.app.data.repository.InvoiceRepository
import com.ops.app.data.repository.JobRepository
import com.ops.app.data.repository.LeadRepository
import com.ops.app.data.repository.PaymentRepository
import com.ops.app.data.sync.SyncChipState
import com.ops.app.data.sync.SyncManager
import com.ops.coredomain.InvoiceStatus
import com.ops.coredomain.JobStatus
import com.ops.coredomain.LeadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val businessName: String = "",
    /** "Today's money in" per the brief — computed as this month's payments,
     * per DISCOVERY.md section 5's home dashboard description. */
    val moneyInThisMonth: BigDecimal = BigDecimal.ZERO,
    val outstandingTotal: BigDecimal = BigDecimal.ZERO,
    val leadsNeedingFollowUp: List<LeadEntity> = emptyList(),
    val activeJobs: List<JobEntity> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    businessRepository: BusinessRepository,
    leadRepository: LeadRepository,
    jobRepository: JobRepository,
    invoiceRepository: InvoiceRepository,
    paymentRepository: PaymentRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        businessRepository.observe(),
        leadRepository.observeAll(),
        jobRepository.observeAll(),
        invoiceRepository.observeAll(),
        paymentRepository.observeAll(),
    ) { business, leads, jobs, invoices, payments ->
        val today = LocalDate.now()
        val thisMonth = YearMonth.now()

        val moneyIn = payments
            .filter { runCatching { YearMonth.from(LocalDate.parse(it.paidDate)) == thisMonth }.getOrDefault(false) }
            .fold(BigDecimal.ZERO) { acc, p -> acc.add(runCatching { BigDecimal(p.amount) }.getOrDefault(BigDecimal.ZERO)) }

        val outstandingStatuses = setOf(InvoiceStatus.SENT.wire, InvoiceStatus.PARTIALLY_PAID.wire, InvoiceStatus.OVERDUE.wire)
        val outstanding = invoices
            .filter { it.status in outstandingStatuses }
            .fold(BigDecimal.ZERO) { acc, inv ->
                val total = runCatching { BigDecimal(inv.total) }.getOrDefault(BigDecimal.ZERO)
                val paid = runCatching { BigDecimal(inv.amountPaid) }.getOrDefault(BigDecimal.ZERO)
                acc.add(total.subtract(paid))
            }

        val followUpDue = leads.filter { lead ->
            lead.status != LeadStatus.CONVERTED.wire &&
                lead.status != LeadStatus.LOST.wire &&
                lead.followUpDate != null &&
                runCatching { !LocalDate.parse(lead.followUpDate).isAfter(today) }.getOrDefault(false)
        }.sortedBy { it.followUpDate }

        val active = jobs.filter { it.status == JobStatus.NOT_STARTED.wire || it.status == JobStatus.IN_PROGRESS.wire }

        HomeUiState(
            businessName = business?.name.orEmpty(),
            moneyInThisMonth = moneyIn,
            outstandingTotal = outstanding,
            leadsNeedingFollowUp = followUpDue,
            activeJobs = active,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val syncChipState: StateFlow<SyncChipState> = syncManager.observeChipState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncChipState.Synced)

    /** A genuine suspend call (not an internal fire-and-forget launch) so
     * pull-to-refresh's spinner stays visible for the caller's own
     * `scope.launch { viewModel.refresh() }` until sync actually finishes,
     * rather than disappearing the instant this function returns. */
    suspend fun refresh() {
        syncManager.syncNow()
    }
}
