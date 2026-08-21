package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/**
 * Mirrors the `quote` sync model's `fields` payload in API_CONTRACT.md. Money
 * fields (`discountAmount`, `subtotal`, `vatAmount`, `total`) are stored as
 * canonical decimal-string TEXT, never REAL/float — see the class doc on
 * [com.ops.app.data.local.OpsDatabase]. `number` is null until the server
 * assigns one on first successful sync (see DISCOVERY.md section 6): the UI
 * shows "Draft — not yet synced" for a null number.
 */
@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey override val id: String,
    val customerId: String,
    val leadId: String?,
    val number: String?,
    /** [com.ops.coredomain.QuoteStatus] wire value. */
    val status: String,
    /** `YYYY-MM-DD`. */
    val issueDate: String,
    val validUntil: String?,
    val notes: String,
    val terms: String,
    val isVatApplicable: Boolean,
    val discountAmount: String,
    val subtotal: String,
    val vatAmount: String,
    val total: String,
    val sentAt: String?,
    val acceptedAt: String?,
    val declinedAt: String?,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
