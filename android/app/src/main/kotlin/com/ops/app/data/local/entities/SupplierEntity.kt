package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/** Mirrors the `supplier` sync model's `fields` payload in API_CONTRACT.md —
 * a simple contact record, not a vendor-management module. */
@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey override val id: String,
    val name: String,
    val contactPerson: String,
    val phone: String,
    val email: String,
    val notes: String,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
