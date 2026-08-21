package com.ops.app.ui.leads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.LeadEntity
import com.ops.app.data.repository.LeadRepository
import com.ops.coredomain.LeadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class LeadListFilter { NEEDS_FOLLOW_UP, ALL }

data class LeadListUiState(
    val filter: LeadListFilter = LeadListFilter.NEEDS_FOLLOW_UP,
    val leads: List<LeadEntity> = emptyList(),
)

@HiltViewModel
class LeadListViewModel @Inject constructor(
    private val leadRepository: LeadRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(LeadListFilter.NEEDS_FOLLOW_UP)

    val uiState: StateFlow<LeadListUiState> = combine(filter, leadRepository.observeAll()) { filter, leads ->
        val today = LocalDate.now()
        val filtered = when (filter) {
            LeadListFilter.ALL -> leads
            LeadListFilter.NEEDS_FOLLOW_UP -> leads.filter { lead ->
                lead.status != LeadStatus.CONVERTED.wire &&
                    lead.status != LeadStatus.LOST.wire &&
                    lead.followUpDate != null &&
                    runCatching { !LocalDate.parse(lead.followUpDate).isAfter(today) }.getOrDefault(false)
            }
        }
        LeadListUiState(filter = filter, leads = filtered.sortedByDescending { it.updatedAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeadListUiState())

    fun setFilter(value: LeadListFilter) {
        filter.value = value
    }

    fun setFollowUpDate(leadId: String, date: String) {
        viewModelScope.launch { leadRepository.updateFollowUpDate(leadId, date) }
    }
}
