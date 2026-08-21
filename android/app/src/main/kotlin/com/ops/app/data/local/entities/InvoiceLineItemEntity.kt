package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/** Mirrors the `invoice_line_item` sync model's `fields` payload in API_CONTRACT.md. */
@Entity(tableName = "invoice_line_items")
data class InvoiceLineItemEntity(
    @PrimaryKey override val id: String,
    val invoiceId: String,
    val description: String,
    val quantity: String,
    val unitPrice: String,
    val lineTotal: String,
    val sortOrder: Int,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
