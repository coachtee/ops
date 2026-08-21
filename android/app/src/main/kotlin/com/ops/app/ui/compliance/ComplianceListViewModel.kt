package com.ops.app.ui.compliance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.ComplianceItemEntity
import com.ops.app.data.repository.ComplianceItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Compliance list — reached from Business Profile/Settings (see
 * BusinessProfileScreen). Every tracked deadline, due-date-first
 * (ComplianceItemDao already orders this way), no filters. */
@HiltViewModel
class ComplianceListViewModel @Inject constructor(
    complianceItemRepository: ComplianceItemRepository,
) : ViewModel() {

    val items: StateFlow<List<ComplianceItemEntity>> = complianceItemRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
