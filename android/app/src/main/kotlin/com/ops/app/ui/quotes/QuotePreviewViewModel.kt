package com.ops.app.ui.quotes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.BusinessEntity
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.local.entities.QuoteLineItemEntity
import com.ops.app.data.repository.BusinessRepository
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.JobRepository
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
import javax.inject.Inject

data class QuotePreviewUiState(
    val quote: QuoteEntity? = null,
    val lineItems: List<QuoteLineItemEntity> = emptyList(),
    val customer: CustomerEntity? = null,
    val business: BusinessEntity? = null,
)

@HiltViewModel
class QuotePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository,
    private val customerRepository: CustomerRepository,
    private val jobRepository: JobRepository,
    businessRepository: BusinessRepository,
) : ViewModel() {

    private val quoteId: String = checkNotNull(savedStateHandle["quoteId"])

    @OptIn(ExperimentalCoroutinesApi::class)
    private val customerFlow = quoteRepository.observeById(quoteId).flatMapLatest { quote ->
        if (quote == null) flowOf(null) else customerRepository.observeById(quote.customerId)
    }

    val uiState: StateFlow<QuotePreviewUiState> = combine(
        quoteRepository.observeById(quoteId),
        quoteRepository.observeLineItems(quoteId),
        customerFlow,
        businessRepository.observe(),
    ) { quote, lineItems, customer, business ->
        QuotePreviewUiState(quote, lineItems.sortedBy { it.sortOrder }, customer, business)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuotePreviewUiState())

    fun markSent() {
        viewModelScope.launch { quoteRepository.markSent(quoteId) }
    }

    /** Accept -> auto-create the follow-on Job, per DISCOVERY.md's IA
     * ("(accepted) -> Job (auto-created)"). Reuses an existing job for this
     * quote if one is already there (e.g. re-tapping Accept). */
    fun markAccepted(onJobReady: (String) -> Unit) {
        viewModelScope.launch {
            quoteRepository.markAccepted(quoteId)
            val quote = quoteRepository.getById(quoteId) ?: return@launch
            val existingJob = jobRepository.getByQuoteId(quoteId)
            val jobId = existingJob?.id ?: run {
                val customerName = customerRepository.getById(quote.customerId)?.name.orEmpty()
                jobRepository.createFromQuote(quote, customerName)
            }
            onJobReady(jobId)
        }
    }

    fun markDeclined() {
        viewModelScope.launch { quoteRepository.markDeclined(quoteId) }
    }
}
