package com.ops.app.data.local

/**
 * The receipt-upload phase's own state machine, independent of
 * [SyncableRecord.syncState] (which only covers an expense's JSON `fields`)
 * — see SyncManager.syncReceipts and API_CONTRACT.md's "Expense receipt
 * attachments" addendum.
 */
object ReceiptSyncState {
    /** No photo captured/picked for this expense. */
    const val NONE = "NONE"

    /** A local photo is waiting to be uploaded (captured offline, or the
     * previous upload attempt failed and is queued for retry). */
    const val PENDING = "PENDING"
    const val UPLOADING = "UPLOADING"
    const val UPLOADED = "UPLOADED"
    const val FAILED = "FAILED"
}
