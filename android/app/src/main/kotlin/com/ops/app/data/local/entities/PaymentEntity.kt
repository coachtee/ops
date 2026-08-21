package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/** Mirrors the `payment` sync model's `fields` payload in API_CONTRACT.md.
 * `invoiceId == null` means a payment on account (against the customer
 * directly, not tied to one invoice). */
@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey override val id: String,
    val customerId: String,
    val invoiceId: String?,
    /** Decimal string ZAR. */
    val amount: String,
    /** [com.ops.coredomain.PaymentMethod] wire value. */
    val method: String,
    val reference: String,
    /** `YYYY-MM-DD`. */
    val paidDate: String,
    val notes: String,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
