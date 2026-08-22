package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.SyncableRecord

/**
 * Mirrors the `visit` sync model's `fields` payload in API_CONTRACT.md — one
 * scheduled attendance against a [JobEntity]. Customer/address are reached
 * via the parent job (`job.customerId`), not duplicated here.
 *
 * The photo does NOT travel through the JSON sync protocol (see
 * API_CONTRACT.md's "Visit photo attachment" addendum) — same
 * [photoUrl]/[localPhotoPath]/[photoSyncState]/[photoSyncError] shape as
 * [ExpenseEntity]'s receipt fields, one slot, not a gallery.
 */
@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey override val id: String,
    val jobId: String,
    val employeeId: String?,
    /** `YYYY-MM-DD`. */
    val scheduledDate: String,
    /** `HH:MM:SS`, nullable. */
    val startTime: String?,
    val endTime: String?,
    /** [com.ops.coredomain.VisitStatus] wire value. */
    val status: String,
    val notes: String,
    /** ISO datetime, nullable. */
    val startedAt: String?,
    val completedAt: String?,
    val photoUrl: String?,
    val localPhotoPath: String?,
    val photoSyncState: String = ReceiptSyncState.NONE,
    val photoSyncError: String? = null,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
