package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/** Mirrors the `customer` sync model's `fields` payload in API_CONTRACT.md. */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey override val id: String,
    val name: String,
    /** [com.ops.coredomain.CustomerType] wire value. */
    val customerType: String,
    val phone: String,
    val email: String,
    val addressLine1: String,
    val addressLine2: String,
    val suburb: String,
    val city: String,
    val province: String,
    val postalCode: String,
    val notes: String,
    val sourceLeadId: String?,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
