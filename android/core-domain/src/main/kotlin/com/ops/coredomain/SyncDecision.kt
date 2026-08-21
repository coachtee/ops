package com.ops.coredomain

import java.time.Instant

/**
 * Outcome of comparing a record's local `updated_at` against an incoming
 * `updated_at` under the app's last-write-wins sync rule. Pure function,
 * mirrors the comparison the server makes in sync/services.py:
 *
 * ```python
 * existing = model_cls.objects.filter(business=business, id=record_id).first()
 * if existing and existing.updated_at >= incoming_updated_at:
 *     # conflict — server keeps its row, returns it
 * else:
 *     # accept — no existing row, or incoming is strictly newer
 * ```
 *
 * On the Android client this same rule decides whether a row just pulled
 * from `GET /api/sync/pull/` is allowed to overwrite what's in Room: a
 * pulled server row is the "incoming" side, the local row's `updatedAt` is
 * the "existing" side. If the local row is still unsynced (PENDING/SYNCING/
 * FAILED/CONFLICT) it must never be silently clobbered by a pull — see
 * SyncManager, which only ever calls this comparison against SYNCED local
 * rows in the first place, exactly so CONFLICT here can only mean "two
 * already-synced writes raced", never "a pull stomped on unsent local work".
 */
enum class SyncDecision {
    ACCEPT,
    CONFLICT,
}

/**
 * Decide whether [incomingUpdatedAt] may overwrite a record whose current
 * timestamp is [existingUpdatedAt] (null = no existing record at all).
 *
 * - No existing record → [SyncDecision.ACCEPT] (nothing to conflict with).
 * - `incomingUpdatedAt` strictly newer than `existingUpdatedAt` → [SyncDecision.ACCEPT].
 * - `existingUpdatedAt >= incomingUpdatedAt` (equal or newer) → [SyncDecision.CONFLICT].
 *   Equal timestamps count as a conflict, not an accept, matching the
 *   server's `>=` — this is also what makes replaying an identical push
 *   idempotent (same id, same updated_at, second attempt lands here).
 */
fun decideSyncOutcome(existingUpdatedAt: Instant?, incomingUpdatedAt: Instant): SyncDecision {
    if (existingUpdatedAt == null) {
        return SyncDecision.ACCEPT
    }
    return if (existingUpdatedAt >= incomingUpdatedAt) {
        SyncDecision.CONFLICT
    } else {
        SyncDecision.ACCEPT
    }
}
