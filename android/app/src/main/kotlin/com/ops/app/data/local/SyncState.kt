package com.ops.app.data.local

/**
 * Local-only sync state, stored as TEXT on every syncable Room entity. Never
 * sent to / received from the server (see API_CONTRACT.md, "Local-only
 * Android fields"). Drives the per-record sync badge in the UI.
 *
 * Transitions (see [com.ops.app.data.sync.SyncManager]):
 *   PENDING  -- created/edited locally, waiting to be pushed
 *   SYNCING  -- push in flight for this record
 *   SYNCED   -- server accepted it; local fields match server_record
 *   FAILED   -- server rejected the push (validation/auth/5xx); syncError set
 *   CONFLICT -- server's row is newer; local edit is preserved, unpublished,
 *               flagged for the user to resolve via the sync status screen
 */
object SyncState {
    const val PENDING = "PENDING"
    const val SYNCING = "SYNCING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
    const val CONFLICT = "CONFLICT"
}

/** Common columns every syncable Room entity carries. See SyncState above and
 * API_CONTRACT.md's "Local-only Android fields" section. */
interface SyncableRecord {
    val id: String
    val updatedAt: String
    val deletedAt: String?
    val syncState: String
    val syncError: String?

    /** The server's row, as raw JSON, when a push comes back `conflict` — kept
     * so the user can review "their change" before choosing how to resolve it,
     * rather than it being silently discarded. Null outside of CONFLICT state. */
    val conflictServerJson: String?
}
