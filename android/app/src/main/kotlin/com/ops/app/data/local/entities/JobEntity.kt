package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/** Mirrors the `job` sync model's `fields` payload in API_CONTRACT.md ("Work" in the product UI). */
@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey override val id: String,
    val customerId: String,
    val quoteId: String?,
    val number: String?,
    val title: String,
    val description: String,
    /** [com.ops.coredomain.JobStatus] wire value. */
    val status: String,
    val startDate: String?,
    val dueDate: String?,
    val completedDate: String?,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
