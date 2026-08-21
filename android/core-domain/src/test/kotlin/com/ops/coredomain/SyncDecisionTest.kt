package com.ops.coredomain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SyncDecisionTest {

    @Test
    fun `no existing record always accepts`() {
        val incoming = Instant.parse("2026-08-21T09:00:00Z")
        assertEquals(SyncDecision.ACCEPT, decideSyncOutcome(existingUpdatedAt = null, incomingUpdatedAt = incoming))
    }

    @Test
    fun `incoming strictly newer than existing accepts`() {
        val existing = Instant.parse("2026-08-21T09:00:00Z")
        val incoming = Instant.parse("2026-08-21T09:00:01Z")
        assertEquals(SyncDecision.ACCEPT, decideSyncOutcome(existing, incoming))
    }

    @Test
    fun `existing newer than incoming conflicts`() {
        val existing = Instant.parse("2026-08-21T09:00:05Z")
        val incoming = Instant.parse("2026-08-21T09:00:00Z")
        assertEquals(SyncDecision.CONFLICT, decideSyncOutcome(existing, incoming))
    }

    @Test
    fun `equal timestamps conflict, matching server's greater-or-equal rule`() {
        // This is also what makes a replayed push after a dropped connection
        // idempotent: same id, same updated_at as what the server already
        // accepted -> lands here as CONFLICT, and the client recognises the
        // returned server_record as identical to what it already has and
        // marks the row SYNCED rather than treating it as a real conflict.
        val same = Instant.parse("2026-08-21T09:00:00Z")
        assertEquals(SyncDecision.CONFLICT, decideSyncOutcome(same, same))
    }

    @Test
    fun `sub-second differences are honoured`() {
        val existing = Instant.parse("2026-08-21T09:00:00.500000Z")
        val incomingOlder = Instant.parse("2026-08-21T09:00:00.499999Z")
        val incomingNewer = Instant.parse("2026-08-21T09:00:00.500001Z")
        assertEquals(SyncDecision.CONFLICT, decideSyncOutcome(existing, incomingOlder))
        assertEquals(SyncDecision.ACCEPT, decideSyncOutcome(existing, incomingNewer))
    }
}
