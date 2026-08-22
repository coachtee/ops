package com.ops.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ops.coredomain.InvoiceStatus
import com.ops.coredomain.JobStatus
import com.ops.coredomain.LeadStatus
import com.ops.coredomain.QuoteStatus

/** OPS Design System v3 — the compact colored pill used for a record's
 * lifecycle status (Lead/Quote/Job/Invoice) everywhere that status appears:
 * list rows, detail headers. Same pill shape as [SyncStateBadge] (a
 * different, orthogonal kind of status — "is this on the server yet",
 * not "where is this record in its lifecycle") so the two badge families
 * read as one visual language rather than two.
 *
 * Only four tones exist, matching the design system's color rule: neutral
 * (nothing to report), attention (amber — needs the owner to act or wait),
 * success (green — reached its good end state), critical (red — lost,
 * cancelled, overdue). A record's specific wire status is translated to a
 * tone by the `xStatusTone` functions below, one per domain, so the actual
 * backend enum values (see API_CONTRACT.md) are never renamed to match this
 * UI — only re-colored. */
enum class StatusTone { NEUTRAL, ATTENTION, SUCCESS, CRITICAL }

@Composable
fun StatusBadge(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val color = when (tone) {
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        StatusTone.ATTENTION -> MaterialTheme.colorScheme.secondary
        // v3: success is tertiary (a dedicated green), not primary — primary
        // is the product's blue interaction color and no longer doubles as
        // "this succeeded" the way v2's green primary did.
        StatusTone.SUCCESS -> MaterialTheme.colorScheme.tertiary
        StatusTone.CRITICAL -> MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Lead lifecycle: NEW/CONTACTED are plain progress (neutral), QUOTED is
 * "waiting on the customer" (attention), CONVERTED is the good end state
 * (success), LOST is the dead end (critical). */
fun leadStatusTone(wire: String): StatusTone = when (wire) {
    LeadStatus.NEW.wire, LeadStatus.CONTACTED.wire -> StatusTone.NEUTRAL
    LeadStatus.QUOTED.wire -> StatusTone.ATTENTION
    LeadStatus.CONVERTED.wire -> StatusTone.SUCCESS
    LeadStatus.LOST.wire -> StatusTone.CRITICAL
    else -> StatusTone.NEUTRAL
}

fun jobStatusTone(wire: String): StatusTone = when (wire) {
    JobStatus.NOT_STARTED.wire -> StatusTone.NEUTRAL
    JobStatus.IN_PROGRESS.wire -> StatusTone.ATTENTION
    JobStatus.COMPLETED.wire -> StatusTone.SUCCESS
    JobStatus.CANCELLED.wire -> StatusTone.CRITICAL
    else -> StatusTone.NEUTRAL
}

fun quoteStatusTone(wire: String): StatusTone = when (wire) {
    QuoteStatus.DRAFT.wire -> StatusTone.NEUTRAL
    QuoteStatus.SENT.wire -> StatusTone.ATTENTION
    QuoteStatus.ACCEPTED.wire -> StatusTone.SUCCESS
    QuoteStatus.DECLINED.wire, QuoteStatus.EXPIRED.wire -> StatusTone.CRITICAL
    else -> StatusTone.NEUTRAL
}

fun invoiceStatusTone(wire: String): StatusTone = when (wire) {
    InvoiceStatus.DRAFT.wire, InvoiceStatus.CANCELLED.wire -> StatusTone.NEUTRAL
    InvoiceStatus.SENT.wire, InvoiceStatus.PARTIALLY_PAID.wire -> StatusTone.ATTENTION
    InvoiceStatus.PAID.wire -> StatusTone.SUCCESS
    InvoiceStatus.OVERDUE.wire -> StatusTone.CRITICAL
    else -> StatusTone.NEUTRAL
}
