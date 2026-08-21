package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/** Mirrors the `quote_line_item` sync model's `fields` payload in API_CONTRACT.md. */
@Entity(tableName = "quote_line_items")
data class QuoteLineItemEntity(
    @PrimaryKey override val id: String,
    val quoteId: String,
    val description: String,
    /** Decimal string, e.g. "2.00". */
    val quantity: String,
    /** Decimal string ZAR, e.g. "450.00". */
    val unitPrice: String,
    /** Decimal string ZAR; computed locally via core-domain's Money for
     * instant UI, overwritten by the server's authoritative value on sync. */
    val lineTotal: String,
    val sortOrder: Int,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
