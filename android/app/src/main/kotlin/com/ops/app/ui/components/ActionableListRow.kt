package com.ops.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * OPS Design System v2 — the "who needs my attention" row: a name, a
 * secondary line, an optional trailing figure, and up to two quick
 * actions. Used for Home's follow-up/outstanding lists today; the same
 * shape is meant to replace the plain [androidx.compose.material3.ListItem]
 * rows across Money and Customer detail in Phase 2. Call/WhatsApp stay as
 * plain callbacks — the actual `Intent` construction stays in the calling
 * screen (same pattern LeadDetailScreen already uses), so this component
 * has no Context dependency of its own.
 */
@Composable
fun ActionableListRow(
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier,
    trailingValue: String? = null,
    trailingEmphasis: Boolean = false,
    onClick: (() -> Unit)? = null,
    onCall: (() -> Unit)? = null,
    onMessage: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth().let { m -> if (onClick != null) m.clickable(onClick = onClick) else m },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(primary, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (trailingValue != null) {
                    Text(
                        trailingValue,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (trailingEmphasis) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (onCall != null || onMessage != null) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onCall != null) {
                        OutlinedButton(onClick = onCall, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" Call", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (onMessage != null) {
                        OutlinedButton(onClick = onMessage, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" WhatsApp", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
