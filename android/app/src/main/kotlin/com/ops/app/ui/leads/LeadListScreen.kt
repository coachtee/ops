package com.ops.app.ui.leads

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.ActionableListRow
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.LEAD_SOURCE_CHOICES
import com.ops.app.ui.components.LEAD_STATUS_CHOICES
import com.ops.app.ui.components.QuickFollowUpButton
import com.ops.app.ui.components.StatusBadge
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.labelFor
import com.ops.app.ui.components.leadStatusTone
import kotlinx.coroutines.launch

@Composable
fun LeadListScreen(
    onOpenLead: (String) -> Unit,
    onNewLead: () -> Unit,
    viewModel: LeadListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LeadListScreenContent(
        uiState = uiState,
        onFilterChange = viewModel::setFilter,
        onOpenLead = onOpenLead,
        onNewLead = onNewLead,
        onFollowUpDateChange = viewModel::setFollowUpDate,
    )
}

/** Full-screen stateless render of [LeadListScreen] (Scaffold chrome +
 * [LeadListContent]) — split out for the screenshot pack (see
 * android/README.md); not called from navigation directly. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadListScreenContent(
    uiState: LeadListUiState,
    onFilterChange: (LeadListFilter) -> Unit,
    onOpenLead: (String) -> Unit,
    onNewLead: () -> Unit,
    onFollowUpDateChange: (String, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun launchIntent(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar("No app found to handle that.") }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Leads") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewLead) { Icon(Icons.Filled.Add, contentDescription = "New lead") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LeadListContent(
            uiState = uiState,
            onFilterChange = onFilterChange,
            onOpenLead = onOpenLead,
            onFollowUpDateChange = onFollowUpDateChange,
            onCallLead = { phone -> launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) },
            onMessageLead = { phone -> launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("smsto:$phone"))) },
            padding = padding,
        )
    }
}

@Composable
private fun LeadListContent(
    uiState: LeadListUiState,
    onFilterChange: (LeadListFilter) -> Unit,
    onOpenLead: (String) -> Unit,
    onFollowUpDateChange: (String, String) -> Unit,
    onCallLead: (String) -> Unit,
    onMessageLead: (String) -> Unit,
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
                ActionableListRow(
                    primary = lead.name,
                    secondary = listOfNotNull(
                        lead.phone.ifBlank { null },
                        labelFor(LEAD_SOURCE_CHOICES, lead.source),
                        lead.followUpDate?.let { "Follow up ${formatDate(it)}" },
                    ).joinToString(" · "),
                    statusBadge = { StatusBadge(labelFor(LEAD_STATUS_CHOICES, lead.status), leadStatusTone(lead.status)) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = { onOpenLead(lead.id) },
                    onCall = if (lead.phone.isNotBlank()) { { onCallLead(lead.phone) } } else null,
                    onMessage = if (lead.phone.isNotBlank()) { { onMessageLead(lead.phone) } } else null,
                    extraAction = {
                        QuickFollowUpButton(onDateSelected = { date -> onFollowUpDateChange(lead.id, date) })
                    },
                )
            }
        }
    }
}
