package com.ops.app.data.sync

/** Drives the sync status chip in the Home top bar — exactly the four states
 * the brief calls for: "Synced / Syncing / N pending / Sync failed". Tapping
 * it (any state) opens the sync status screen. CONFLICT rows are folded into
 * [Failed] here (they need the same "go look at this" urgency as an error)
 * but keep their own distinct resolution UI once you're on that screen. */
sealed interface SyncChipState {
    data object Synced : SyncChipState
    data object Syncing : SyncChipState
    data class Pending(val count: Int) : SyncChipState
    data class Failed(val count: Int) : SyncChipState
}
