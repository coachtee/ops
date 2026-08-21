package com.ops.app.ui.syncstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.repository.SyncStatusItem
import com.ops.app.data.repository.SyncStatusRepository
import com.ops.app.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    private val syncStatusRepository: SyncStatusRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val items: StateFlow<List<SyncStatusItem>> = syncStatusRepository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun retryAll() {
        viewModelScope.launch { syncManager.syncNow() }
    }

    fun retry(item: SyncStatusItem) {
        viewModelScope.launch { syncStatusRepository.retry(item) }
    }

    /** "Keep mine": bump `updatedAt` to now and re-push. */
    fun keepMine(item: SyncStatusItem) {
        viewModelScope.launch { syncStatusRepository.keepMine(item) }
    }

    /** "Use theirs": overwrite the local row from `conflictServerJson`, clearing the conflict. */
    fun useTheirs(item: SyncStatusItem) {
        viewModelScope.launch { syncStatusRepository.useTheirs(item) }
    }
}
