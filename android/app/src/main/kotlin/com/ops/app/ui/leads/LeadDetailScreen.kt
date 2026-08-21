package com.ops.app.ui.leads

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.LEAD_SOURCE_CHOICES
import com.ops.app.ui.components.LEAD_STATUS_CHOICES
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.QuickFollowUpButton
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.StatusBadge
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.labelFor
import com.ops.app.ui.components.leadStatusTone
import kotlinx.coroutines.launch

@Composable
fun LeadDetailScreen(
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onCreateQuote: (customerId: String, leadId: String) -> Unit,
    viewModel: LeadDetailViewModel = hiltViewModel(),
) {
    val lead by viewModel.lead.collectAsStateWithLifecycle()
    LeadDetailContent(
        lead = lead,
        onBack = onBack,
        onUpdateStatus = viewModel::updateStatus,
        onUpdateFollowUpDate = viewModel::updateFollowUpDate,
        onUpdateNotes = viewModel::updateNotes,
        onConvertToCustomer = { viewModel.convertToCustomer { onOpenCustomer(it) } },
        onOpenCustomer = onOpenCustomer,
        onCreateQuote = { viewModel.createQuote(onCreateQuote) },
    )
}

/** Stateless render of [LeadDetailScreen] — split out for the screenshot
 * pack (see android/README.md); not called from navigation directly.
 *
 * Mirrors the list row's shape: header identifies the lead and its status
 * at a glance, the same [Call]/[WhatsApp]/[Follow up] actions sit right
 * under it, and "Convert to customer" — the one action that moves a lead
 * into the rest of the commercial flow — is the one filled, unmissable
 * button on the screen. There is no separate activity-history section:
 * OPS doesn't keep a lead activity log today (see API_CONTRACT.md), so
 * this only shows what's actually stored — status, follow-up date, notes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadDetailContent(
    lead: com.ops.app.data.local.entities.LeadEntity?,
    onBack: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onUpdateFollowUpDate: (String?) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onConvertToCustomer: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onCreateQuote: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var noteDraft by remember(lead?.id) { mutableStateOf(lead?.notes.orEmpty()) }

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
                title = { Text(lead?.name ?: "Lead") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val currentLead = lead
        if (currentLead == null) {
            Column(Modifier.fillMaxSize().padding(padding)) {}
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(currentLead.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        currentLead.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                StatusBadge(labelFor(LEAD_STATUS_CHOICES, currentLead.status), leadStatusTone(currentLead.status))
            }
            SyncStateBadge(currentLead.syncState)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentLead.phone}"))) },
                    enabled = currentLead.phone.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Call, contentDescription = null); Text(" Call", modifier = Modifier.padding(start = 4.dp)) }
                OutlinedButton(
                    onClick = { launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("smsto:${currentLead.phone}"))) },
                    enabled = currentLead.phone.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Chat, contentDescription = null); Text(" WhatsApp", modifier = Modifier.padding(start = 4.dp)) }
                QuickFollowUpButton(
                    onDateSelected = onUpdateFollowUpDate,
                    modifier = Modifier.weight(1f),
                )
            }

            if (currentLead.enquiry.isNotBlank()) {
                Text("Enquiry: ${currentLead.enquiry}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Source: ${labelFor(LEAD_SOURCE_CHOICES, currentLead.source)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (currentLead.convertedCustomerId != null) {
                OutlinedButton(onClick = { onOpenCustomer(currentLead.convertedCustomerId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("View customer")
                }
            } else {
                Button(onClick = onConvertToCustomer, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Handshake, contentDescription = null)
                    Text(" Convert to customer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(onClick = onCreateQuote, modifier = Modifier.fillMaxWidth()) { Text("Create quote") }
            }

            SectionHeader("Details")
            LabeledDropdown(
                label = "Status",
                options = LEAD_STATUS_CHOICES,
                selected = currentLead.status,
                onSelected = onUpdateStatus,
            )
            DateField(
                label = "Follow-up date",
                value = currentLead.followUpDate,
                onValueChange = onUpdateFollowUpDate,
            )

            SectionHeader("Notes")
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { noteDraft = it },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onUpdateNotes(noteDraft) },
                enabled = noteDraft != currentLead.notes,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save note") }
        }
    }
}
