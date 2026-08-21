package com.ops.app.ui.leads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatDate

@Composable
fun LeadListScreen(
    onOpenLead: (String) -> Unit,
    onNewLead: () -> Unit,
    viewModel: LeadListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LeadListScreenContent(uiState = uiState, onFilterChange = viewModel::setFilter, onOpenLead = onOpenLead, onNewLead = onNewLead)
}

/** Full-screen stateless render of [LeadListScreen] (Scaffold chrome +
 * [LeadListContent]) — split out for the screenshot pack (see
 * android/README.md); not called from navigation directly. */
@Composable
fun LeadListScreenContent(
    uiState: LeadListUiState,
    onFilterChange: (LeadListFilter) -> Unit,
    onOpenLead: (String) -> Unit,
    onNewLead: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Leads") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewLead) { Icon(Icons.Filled.Add, contentDescription = "New lead") }
        },
    ) { padding ->
        LeadListContent(
            uiState = uiState,
            onFilterChange = onFilterChange,
            onOpenLead = onOpenLead,
            padding = padding,
        )
    }
}

@Composable
private fun LeadListContent(
    uiState: LeadListUiState,
    onFilterChange: (LeadListFilter) -> Unit,
    onOpenLead: (String) -> Unit,
    padding: PaddingValues,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                FilterChip(
                    selected = uiState.filter == LeadListFilter.NEEDS_FOLLOW_UP,
                    onClick = { onFilterChange(LeadListFilter.NEEDS_FOLLOW_UP) },
                    label = { Text("Needs follow-up") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = uiState.filter == LeadListFilter.ALL,
                    onClick = { onFilterChange(LeadListFilter.ALL) },
                    label = { Text("All") },
                )
            }
        }

        if (uiState.leads.isEmpty()) {
            item {
                EmptyState(
                    title = if (uiState.filter == LeadListFilter.NEEDS_FOLLOW_UP) "Nobody to follow up with" else "No leads yet",
                    body = "Tap + to capture a new enquiry — name, phone and where it came from is all you need.",
                )
            }
        } else {
            items(uiState.leads, key = { it.id }) { lead ->
                ListItem(
                    headlineContent = { Text(lead.name) },
                    supportingContent = {
                        Text(
                            listOfNotNull(
                                lead.phone.ifBlank { null },
                                lead.followUpDate?.let { "Follow up ${formatDate(it)}" },
                            ).joinToString(" · "),
                        )
                    },
                    trailingContent = { SyncStateBadge(lead.syncState) },
                    modifier = Modifier.fillMaxWidth().clickable { onOpenLead(lead.id) },
                )
            }
        }
    }
}
