package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/** Mirrors the `invoice` sync model's `fields` payload in API_CONTRACT.md. */
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey override val id: String,
    val customerId: String,
    val jobId: String?,
    val quoteId: String?,
    val number: String?,
    /** [com.ops.coredomain.InvoiceStatus] wire value. */
    val status: String,
    val issueDate: String,
    val dueDate: String?,
    val notes: String,
    val terms: String,
    val isVatApplicable: Boolean,
    val discountAmount: String,
    val subtotal: String,
    val vatAmount: String,
    val total: String,
    /** Decimal string; server-computed/read-only, kept up to date locally by
     * recomputing after every payment for instant "outstanding" display. */
    val amountPaid: String,
    val sentAt: String?,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
