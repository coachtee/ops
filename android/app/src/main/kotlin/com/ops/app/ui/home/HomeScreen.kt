package com.ops.app.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.ActionableListRow
import com.ops.app.ui.components.COMPLIANCE_CATEGORY_CHOICES
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.StatCard
import com.ops.app.ui.components.SyncStatusChip
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.labelFor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSyncStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLead: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    onOpenCompliance: () -> Unit,
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
        onOpenCompliance = onOpenCompliance,
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
    onOpenCompliance: () -> Unit,
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    fun launchIntent(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar("No app found to handle that.") }
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        ActionableListRow(
                            primary = lead.name,
                            secondary = "Follow up by ${formatDate(lead.followUpDate)}",
                            onClick = { onOpenLead(lead.id) },
                            onCall = if (lead.phone.isNotBlank()) {
                                { launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))) }
                            } else {
                                null
                            },
                            onMessage = if (lead.phone.isNotBlank()) {
                                { launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("smsto:${lead.phone}"))) }
                            } else {
                                null
                            },
                        )
                    }
                }

                item { SectionHeader("Active jobs") }
                if (uiState.activeJobs.isEmpty()) {
                    item { EmptyState("No active jobs", "Jobs appear here once a quote is accepted.") }
                } else {
                    items(uiState.activeJobs, key = { it.id }) { job ->
                        ActionableListRow(
                            primary = job.number ?: job.title,
                            secondary = if (job.number != null) job.title else "Draft — not yet synced",
                            trailingValue = job.status.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            onClick = { onOpenJob(job.id) },
                        )
                    }
                }

                uiState.upcomingComplianceItem?.let { complianceItem ->
                    item { SectionHeader("Compliance") }
                    item {
                        val daysUntil = runCatching {
                            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(complianceItem.dueDate))
                        }.getOrDefault(0L)
                        val whenText = when {
                            daysUntil < 0 -> "Overdue by ${-daysUntil} day${if (daysUntil == -1L) "" else "s"}"
                            daysUntil == 0L -> "Due today"
                            else -> "Due in $daysUntil day${if (daysUntil == 1L) "" else "s"}"
                        }
                        ActionableListRow(
                            primary = complianceItem.title,
                            secondary = labelFor(COMPLIANCE_CATEGORY_CHOICES, complianceItem.category),
                            trailingValue = whenText,
                            trailingEmphasis = true,
                            onClick = onOpenCompliance,
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
private fun QuickAddRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
