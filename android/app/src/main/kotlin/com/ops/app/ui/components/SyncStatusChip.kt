package com.ops.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ops.app.data.sync.SyncChipState

/**
 * The Home top bar's sync status chip — exactly the four states the brief
 * calls for ("Synced / Syncing / N pending / Sync failed"); tapping it opens
 * the sync status screen (see DISCOVERY.md section 6 and the screen list's
 * "Sync status sheet").
 */
@Composable
fun SyncStatusChip(state: SyncChipState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (label, icon, containerColor, contentColor) = when (state) {
        is SyncChipState.Synced -> ChipVisuals(
            "Synced",
            Icons.Filled.CheckCircle,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        is SyncChipState.Syncing -> ChipVisuals(
            "Syncing…",
            Icons.Filled.Sync,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        is SyncChipState.Pending -> ChipVisuals(
            "${state.count} pending",
            Icons.Filled.Sync,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        is SyncChipState.Failed -> ChipVisuals(
            "Sync failed",
            Icons.Filled.Error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }

    SuggestionChip(
        onClick = onClick,
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 0.dp)) },
        shape = RoundedCornerShape(50),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            iconContentColor = contentColor,
        ),
        modifier = modifier,
    )
}

private data class ChipVisuals(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
)
