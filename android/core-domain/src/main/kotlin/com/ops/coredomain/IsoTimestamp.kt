package com.ops.coredomain

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * The wire format for every timestamp in this API: UTC ISO-8601 with a
 * literal `Z` suffix, e.g. `2026-08-21T10:15:00.123456Z` — NEVER `+00:00`.
 *
 * Why this matters (see API_CONTRACT.md, top of file, and
 * backend/sync/services.py `_iso`): `GET /api/sync/pull/?since=<cursor>`
 * puts this value straight into a URL query string. An un-encoded `+` in a
 * query string is decoded as a space under standard `application/
 * x-www-form-urlencoded` rules, which would silently truncate/corrupt the
 * offset and therefore the sync cursor — a real bug the backend had to fix.
 * `Z` has no such trap (it's not a reserved query character), so the server
 * always emits `Z` and this client must construct its own `updated_at`
 * values (set locally on every create/edit) the exact same way. Never format
 * with `+00:00` here, even though it is a technically-equivalent ISO-8601
 * offset — the whole point is avoiding that byte in a URL.
 */
object IsoTimestamp {

    private val DATE_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)

    /**
     * Format an [Instant] as `yyyy-MM-ddTHH:mm:ss.ffffffZ` (6-digit,
     * i.e. microsecond, fractional seconds — matching the precision of the
     * contract's own example — truncating any sub-microsecond nanosecond
     * precision the JVM clock may carry).
     */
    fun format(instant: Instant): String {
        val truncated = instant.truncatedTo(ChronoUnit.MICROS)
        val base = DATE_TIME_FORMATTER.format(truncated)
        val microsOfSecond = truncated.nano / 1_000
        val fraction = microsOfSecond.toString().padStart(6, '0')
        return "$base.${fraction}Z"
    }

    /**
     * Parse a wire timestamp back into an [Instant]. Accepts the `Z`-suffixed
     * form this app and the server both emit, with or without a fractional
     * second component, and — defensively, since it costs nothing — a
     * `+HH:MM`/`-HH:MM` offset form too, in case a value ever round-trips
     * through something that normalizes it that way.
     */
    fun parse(value: String): Instant =
        try {
            Instant.parse(value)
        } catch (e: DateTimeParseException) {
            // Defensive fallback only — the server and this client always emit
            // 'Z', never an explicit offset. Costs nothing to also accept one.
            OffsetDateTime.parse(value).toInstant()
        }
}
