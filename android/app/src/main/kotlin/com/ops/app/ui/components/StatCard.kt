package com.ops.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * OPS Design System v2 — the flat-bordered stat tile used across Home,
 * Customer detail, and Payslip: one label, one large figure. `emphasise`
 * shifts both to the amber "needs attention" tone (an overdue amount,
 * outstanding balance) — the only two colors a stat card is allowed to
 * render in, per the design system's color rule (green/brand is never
 * used here; amber means attention, everything else stays neutral ink).
 */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, emphasise: Boolean = false) {
    val tone = if (emphasise) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (emphasise) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (emphasise) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.displaySmall, color = tone, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
