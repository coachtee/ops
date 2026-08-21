package com.ops.coredomain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class IsoTimestampTest {

    @Test
    fun `formats with literal Z suffix, never plus-zero-zero-zero-zero`() {
        val instant = Instant.parse("2026-08-21T10:15:00.123456Z")
        val formatted = IsoTimestamp.format(instant)
        assertEquals("2026-08-21T10:15:00.123456Z", formatted)
        assertTrue(formatted.endsWith("Z"))
        assertFalse("must never contain the offset form the contract forbids", formatted.contains("+00:00"))
        assertFalse("must never contain a raw '+' at all (form-encoding hazard)", formatted.contains("+"))
    }

    @Test
    fun `formats zero microseconds with explicit six zero digits`() {
        val instant = Instant.parse("2026-08-21T10:15:00Z")
        assertEquals("2026-08-21T10:15:00.000000Z", IsoTimestamp.format(instant))
    }

    @Test
    fun `round trips through format and parse`() {
        val original = Instant.parse("2026-08-21T09:00:00.123456Z")
        val roundTripped = IsoTimestamp.parse(IsoTimestamp.format(original))
        assertEquals(original, roundTripped)
    }

    @Test
    fun `round trips an instant with nanosecond precision, truncated to microseconds`() {
        val withNanos = Instant.parse("2026-08-21T09:00:00Z").plusNanos(123_456_789)
        val formatted = IsoTimestamp.format(withNanos)
        val expectedTruncated = withNanos.truncatedTo(ChronoUnit.MICROS)
        assertEquals(expectedTruncated, IsoTimestamp.parse(formatted))
    }

    @Test
    fun `parses the server's own example value from the contract`() {
        val parsed = IsoTimestamp.parse("2026-08-21T10:15:00.123456Z")
        assertEquals(Instant.parse("2026-08-21T10:15:00.123456Z"), parsed)
    }

    @Test
    fun `parses a value with no fractional seconds`() {
        val parsed = IsoTimestamp.parse("2026-08-21T10:15:00Z")
        assertEquals(Instant.parse("2026-08-21T10:15:00Z"), parsed)
    }

    @Test
    fun `defensively accepts an explicit offset form on parse`() {
        // The app itself never emits this, but parsing is lenient in case a
        // value is ever normalized to this equivalent form upstream.
        val parsed = IsoTimestamp.parse("2026-08-21T10:15:00.123456+00:00")
        assertEquals(Instant.parse("2026-08-21T10:15:00.123456Z"), parsed)
    }
}
