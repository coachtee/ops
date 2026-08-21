package com.ops.app.ui.leads

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.LeadEntity
import com.ops.app.data.repository.LeadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeadDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val leadRepository: LeadRepository,
) : ViewModel() {

    private val leadId: String = checkNotNull(savedStateHandle["leadId"])

    val lead: StateFlow<LeadEntity?> = leadRepository.observeById(leadId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateNotes(notes: String) {
        viewModelScope.launch { leadRepository.updateNotes(leadId, notes) }
    }

    fun updateFollowUpDate(date: String?) {
        viewModelScope.launch { leadRepository.updateFollowUpDate(leadId, date) }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch { leadRepository.updateStatus(leadId, status) }
    }

    /** Convert to customer — used both by the standalone "Convert to
     * customer" action and internally by "Create quote" (a quote always
     * needs a customer_id). */
    fun convertToCustomer(onDone: (customerId: String) -> Unit) {
        viewModelScope.launch {
            leadRepository.convertToCustomer(leadId)?.let(onDone)
        }
    }

    fun createQuote(onDone: (customerId: String, leadId: String) -> Unit) {
        viewModelScope.launch {
            val customerId = lead.value?.convertedCustomerId ?: leadRepository.convertToCustomer(leadId)
            if (customerId != null) onDone(customerId, leadId)
        }
    }
}
