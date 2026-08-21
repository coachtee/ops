package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/**
 * Mirrors the `compliance_item` sync model's `fields` payload in
 * API_CONTRACT.md — a plain owner-managed deadline checklist entry. This
 * app never files, submits, or claims to know a business's actual status
 * with SARS or CIPC; [completedDate] is set only when the owner ticks it
 * off themselves. [category] only drives a suggested default title and
 * how the app nudges "add the next one" once a recurring item is marked
 * done — there is no recurrence engine anywhere in this app.
 */
@Entity(tableName = "compliance_items")
data class ComplianceItemEntity(
    @PrimaryKey override val id: String,
    /** [com.ops.coredomain.ComplianceCategory] wire value. */
    val category: String,
    val title: String,
    /** `YYYY-MM-DD`. */
    val dueDate: String,
    /** `YYYY-MM-DD`, or null if not yet done. */
    val completedDate: String?,
    val isRecurring: Boolean,
    val notes: String,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
