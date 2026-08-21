package com.ops.app.ui.compliance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.COMPLIANCE_CATEGORY_CHOICES
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.labelFor

@Composable
fun ComplianceListScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onNewItem: () -> Unit,
    viewModel: ComplianceListViewModel = hiltViewModel(),
) {
    val complianceItems by viewModel.items.collectAsStateWithLifecycle()
    ComplianceListContent(complianceItems = complianceItems, onBack = onBack, onOpenItem = onOpenItem, onNewItem = onNewItem)
}

/** Stateless render of [ComplianceListScreen] — split out for the
 * screenshot pack (see android/README.md); not called from navigation
 * directly. */
@Composable
fun ComplianceListContent(
    complianceItems: List<com.ops.app.data.local.entities.ComplianceItemEntity>,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onNewItem: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compliance") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewItem) { Icon(Icons.Filled.Add, contentDescription = "New compliance item") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Text(
                    "OPS helps you track deadlines — it doesn't file anything with SARS or CIPC for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (complianceItems.isEmpty()) {
                item {
                    EmptyState(
                        title = "No deadlines tracked yet",
                        body = "Tap + to add a VAT return, PAYE/UIF/SDL, provisional tax, or CIPC annual return reminder.",
                    )
                }
            } else {
                items(complianceItems, key = { it.id }) { complianceItem ->
                    ListItem(
                        headlineContent = { Text(complianceItem.title) },
                        supportingContent = {
                            val status = if (complianceItem.completedDate != null) {
                                "Done ${formatDate(complianceItem.completedDate)}"
                            } else {
                                "Due ${formatDate(complianceItem.dueDate)}"
                            }
                            Text("${labelFor(COMPLIANCE_CATEGORY_CHOICES, complianceItem.category)} · $status")
                        },
                        trailingContent = { SyncStateBadge(complianceItem.syncState) },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenItem(complianceItem.id) },
                    )
                }
            }
        }
    }
}
