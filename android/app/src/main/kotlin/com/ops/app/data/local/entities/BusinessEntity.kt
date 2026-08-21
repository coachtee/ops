package com.ops.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ops.app.data.local.SyncableRecord

/**
 * Single-row cache of the authenticated user's [Business]. Fields mirror
 * accounts/serializers.py BusinessSerializer exactly. Unlike the eight sync
 * models, Business is read/written directly via `GET`/`PATCH
 * /api/business/me/` (see API_CONTRACT.md) rather than through
 * `/api/sync/push|pull/` — it is not one of the sync engine's registered
 * model keys — so [syncState] here reflects "has this PATCH round-tripped
 * yet", set by [com.ops.app.data.repository.BusinessRepository] directly,
 * not by [com.ops.app.data.sync.SyncManager]'s outbox loop. [deletedAt] and
 * [conflictServerJson] are carried for schema consistency with the other
 * entities but are never populated in practice — a business is never soft
 * deleted or put through the conflict flow in this slice.
 */
@Entity(tableName = "business")
data class BusinessEntity(
    @PrimaryKey override val id: String,
    val name: String,
    val tradingName: String,
    val registrationNumber: String,
    val taxNumber: String,
    val vatNumber: String,
    val isVatRegistered: Boolean,
    val industry: String,
    val phone: String,
    val email: String,
    val addressLine1: String,
    val addressLine2: String,
    val suburb: String,
    val city: String,
    val province: String,
    val postalCode: String,
    /** Absolute (or server-relative) URL of the uploaded logo image, as
     * returned by the server; null if no logo has been uploaded yet. */
    val logoUrl: String?,
    override val updatedAt: String,
    override val deletedAt: String? = null,
    override val syncState: String,
    override val syncError: String? = null,
    override val conflictServerJson: String? = null,
) : SyncableRecord
