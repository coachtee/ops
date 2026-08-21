package com.ops.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.SyncStatusChip
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSyncStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLead: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    onNewLead: () -> Unit,
    onNewCustomer: () -> Unit,
    onPickCustomerForQuote: () -> Unit,
    onPickCustomerForInvoice: () -> Unit,
    onPickCustomerForPayment: () -> Unit,
    onNewExpense: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncChipState by viewModel.syncChipState.collectAsStateWithLifecycle()
    HomeContent(
        uiState = uiState,
        syncChipState = syncChipState,
        onOpenSyncStatus = onOpenSyncStatus,
        onOpenSettings = onOpenSettings,
        onOpenLead = onOpenLead,
        onOpenJob = onOpenJob,
        onNewLead = onNewLead,
        onNewCustomer = onNewCustomer,
        onPickCustomerForQuote = onPickCustomerForQuote,
        onPickCustomerForInvoice = onPickCustomerForInvoice,
        onPickCustomerForPayment = onPickCustomerForPayment,
        onNewExpense = onNewExpense,
        onRefresh = { viewModel.refresh() },
    )
}

/** Stateless render of [HomeScreen] — split out so a screenshot pack can
 * render the exact production UI with hand-built fake state, without
 * needing Hilt/Room/WorkManager (see android/README.md's screenshot pack
 * notes). Delegates from [HomeScreen] unchanged; not called from
 * navigation directly. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    syncChipState: com.ops.app.data.sync.SyncChipState,
    onOpenSyncStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLead: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    onNewLead: () -> Unit,
    onNewCustomer: () -> Unit,
    onPickCustomerForQuote: () -> Unit,
    onPickCustomerForInvoice: () -> Unit,
    onPickCustomerForPayment: () -> Unit,
    onNewExpense: () -> Unit,
    onRefresh: suspend () -> Unit,
) {
    var showQuickAdd by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.businessName.ifBlank { "OPS" }) },
                actions = {
                    SyncStatusChip(state = syncChipState, onClick = onOpenSyncStatus, modifier = Modifier.padding(end = 4.dp))
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Business profile & settings") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showQuickAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Quick add")
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    onRefresh()
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Money in", formatZar(uiState.moneyInThisMonth), Modifier.weight(1f))
                        StatCard("Money out", formatZar(uiState.moneyOutThisMonth), Modifier.weight(1f))
                        StatCard("Outstanding", formatZar(uiState.outstandingTotal), Modifier.weight(1f), emphasise = uiState.outstandingTotal.signum() > 0)
                    }
                }

                item { SectionHeader("Needs follow-up") }
                if (uiState.leadsNeedingFollowUp.isEmpty()) {
                    item { EmptyState("All caught up", "No leads are waiting on a follow-up right now.") }
                } else {
                    items(uiState.leadsNeedingFollowUp, key = { it.id }) { lead ->
                        ListItem(
                            headlineContent = { Text(lead.name) },
                            supportingContent = { Text("Follow up by ${formatDate(lead.followUpDate)} · ${lead.phone}") },
                            modifier = Modifier.fillMaxWidth().clickable { onOpenLead(lead.id) },
                        )
                    }
                }

                item { SectionHeader("Active jobs") }
                if (uiState.activeJobs.isEmpty()) {
                    item { EmptyState("No active jobs", "Jobs appear here once a quote is accepted.") }
                } else {
                    items(uiState.activeJobs, key = { it.id }) { job ->
                        ListItem(
                            headlineContent = { Text(job.number ?: job.title) },
                            supportingContent = { Text(if (job.number != null) job.title else "Draft — not yet synced") },
                            modifier = Modifier.fillMaxWidth().clickable { onOpenJob(job.id) },
                        )
                    }
                }
            }
        }
    }

    if (showQuickAdd) {
        ModalBottomSheet(onDismissRequest = { showQuickAdd = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text("Quick add", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                QuickAddRow(Icons.Filled.Handshake, "New lead") { showQuickAdd = false; onNewLead() }
                QuickAddRow(Icons.Filled.PersonAdd, "New customer") { showQuickAdd = false; onNewCustomer() }
                QuickAddRow(Icons.Filled.RequestQuote, "New quote") { showQuickAdd = false; onPickCustomerForQuote() }
                QuickAddRow(Icons.Filled.Receipt, "New invoice") { showQuickAdd = false; onPickCustomerForInvoice() }
                QuickAddRow(Icons.Filled.Savings, "Record payment") { showQuickAdd = false; onPickCustomerForPayment() }
                QuickAddRow(Icons.Filled.MoneyOff, "Record expense") { showQuickAdd = false; onNewExpense() }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, emphasise: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (emphasise) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun QuickAddRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
