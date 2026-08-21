package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.SyncableRecord

/**
 * Mirrors the `expense` sync model's `fields` payload in API_CONTRACT.md.
 * `amount` is the VAT-INCLUSIVE total the owner paid (see
 * [com.ops.coredomain.Money.extractVatFromInclusive]); `vatAmount` is
 * derived from it, computed locally for instant UI and overwritten by the
 * server's value once synced — same pattern as `line_total` elsewhere.
 *
 * The receipt photo does NOT travel through the JSON sync protocol (see
 * API_CONTRACT.md's "Expense receipt attachments" addendum) — [receiptUrl]
 * is the server's URL once a photo has been uploaded and synced down,
 * [localReceiptPath] is where a captured/picked photo lives on this device
 * before (or regardless of) upload, and [receiptSyncState]/[receiptSyncError]
 * track that second, separate sync phase (see SyncManager.syncReceipts).
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey override val id: String,
    val jobId: String?,
    val supplierId: String?,
    /** [com.ops.coredomain.ExpenseCategory] wire value. */
    val category: String,
    val description: String,
    /** Decimal string, VAT-inclusive total paid — never a float. */
    val amount: String,
    val isVatApplicable: Boolean,
    /** Decimal string, derived from [amount] — never hand-entered. */
    val vatAmount: String,
    /** `YYYY-MM-DD`. */
    val date: String,
    val receiptUrl: String?,
    val localReceiptPath: String?,
    val receiptSyncState: String = ReceiptSyncState.NONE,
    val receiptSyncError: String? = null,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
