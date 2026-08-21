package com.ops.app.ui.syncstatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.data.local.SyncState
import com.ops.app.data.repository.SyncStatusItem
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.syncStateColor
import com.ops.app.ui.components.syncStateLabel

@Composable
fun SyncStatusScreen(
    onBack: () -> Unit,
    viewModel: SyncStatusViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync status") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = viewModel::retryAll, enabled = !isSyncing) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Sync now")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState("Everything's synced", "Every record on this phone matches the server.")
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            items(items, key = { "${it.kindLabel}-${it.id}" }) { item ->
                SyncStatusCard(
                    item = item,
                    onRetry = { viewModel.retry(item) },
                    onKeepMine = { viewModel.keepMine(item) },
                    onUseTheirs = { viewModel.useTheirs(item) },
                )
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    item: SyncStatusItem,
    onRetry: () -> Unit,
    onKeepMine: () -> Unit,
    onUseTheirs: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.kindLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(syncStateLabel(item.syncState), style = MaterialTheme.typography.labelLarge, color = syncStateColor(item.syncState))
            }
            Text(item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))

            if (item.syncState == SyncState.FAILED && !item.syncError.isNullOrBlank()) {
                Text(item.syncError, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
            }

            when (item.syncState) {
                SyncState.FAILED -> {
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("Retry") }
                }
                SyncState.CONFLICT -> {
                    Text(
                        "Someone else's change to this record is newer. Choose which version to keep.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onUseTheirs) { Text("Use theirs") }
                        Button(onClick = onKeepMine) { Text("Keep mine") }
                    }
                }
                else -> Unit // PENDING/SYNCING resolve on their own via the next sync
            }
        }
    }
}
