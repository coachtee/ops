package com.ops.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ops.app.data.local.SyncState

/** Small per-record badge — "the owner always knows if a record is only on
 * their phone" (DISCOVERY.md section 6). Used on list rows and detail screens. */
@Composable
fun SyncStateBadge(syncState: String, modifier: Modifier = Modifier) {
    val (label, color) = when (syncState) {
        SyncState.PENDING -> "Saved on this phone" to MaterialTheme.colorScheme.secondary
        SyncState.SYNCING -> "Syncing…" to MaterialTheme.colorScheme.secondary
        SyncState.SYNCED -> return // clean state — no badge needed, keeps lists quiet
        SyncState.FAILED -> "Sync failed" to MaterialTheme.colorScheme.error
        SyncState.CONFLICT -> "Needs review" to MaterialTheme.colorScheme.error
        else -> syncState to MaterialTheme.colorScheme.secondary
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Same states, but never blank — used where "Synced" itself is worth showing (sync status screen). */
fun syncStateLabel(syncState: String): String = when (syncState) {
    SyncState.PENDING -> "Saved on this phone"
    SyncState.SYNCING -> "Syncing…"
    SyncState.SYNCED -> "Synced"
    SyncState.FAILED -> "Sync failed"
    SyncState.CONFLICT -> "Needs review"
    else -> syncState
}

@Composable
fun syncStateColor(syncState: String): Color = when (syncState) {
    SyncState.FAILED, SyncState.CONFLICT -> MaterialTheme.colorScheme.error
    SyncState.SYNCED -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.secondary
}
