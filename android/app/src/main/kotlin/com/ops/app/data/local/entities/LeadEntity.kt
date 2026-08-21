package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/** Mirrors the `lead` sync model's `fields` payload in API_CONTRACT.md. */
@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey override val id: String,
    val name: String,
    val phone: String,
    val email: String,
    /** [com.ops.coredomain.LeadSource] wire value. */
    val source: String,
    val enquiry: String,
    val notes: String,
    /** [com.ops.coredomain.LeadStatus] wire value. */
    val status: String,
    /** `YYYY-MM-DD`, nullable. */
    val followUpDate: String?,
    val convertedCustomerId: String?,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
