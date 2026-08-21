package com.ops.app.ui.customers

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.INVOICE_STATUS_CHOICES
import com.ops.app.ui.components.QUOTE_STATUS_CHOICES
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.labelFor

@Composable
fun CustomerDetailScreen(
    onBack: () -> Unit,
    onOpenQuote: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    onOpenInvoice: (String) -> Unit,
    onNewQuote: (customerId: String) -> Unit,
    onNewInvoice: (customerId: String) -> Unit,
    onRecordPayment: (customerId: String) -> Unit,
    viewModel: CustomerDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CustomerDetailContent(
        uiState = uiState,
        onBack = onBack,
        onOpenQuote = onOpenQuote,
        onOpenJob = onOpenJob,
        onOpenInvoice = onOpenInvoice,
        onNewQuote = onNewQuote,
        onNewInvoice = onNewInvoice,
        onRecordPayment = onRecordPayment,
        onUpdateNotes = viewModel::updateNotes,
    )
}

/** Stateless render of [CustomerDetailScreen] — split out for the
 * screenshot pack (see android/README.md); not called from navigation
 * directly. */
@Composable
fun CustomerDetailContent(
    uiState: CustomerDetailUiState,
    onBack: () -> Unit,
    onOpenQuote: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    onOpenInvoice: (String) -> Unit,
    onNewQuote: (customerId: String) -> Unit,
    onNewInvoice: (customerId: String) -> Unit,
    onRecordPayment: (customerId: String) -> Unit,
    onUpdateNotes: (String) -> Unit,
) {
    val customer = uiState.customer
    var noteDraft by remember(customer?.id) { mutableStateOf(customer?.notes.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Customer") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        if (customer == null) return@Scaffold

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text(customer.phone, style = MaterialTheme.typography.bodyMedium)
            if (customer.email.isNotBlank()) Text(customer.email, style = MaterialTheme.typography.bodyMedium)

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Outstanding", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatZar(uiState.outstandingTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.outstandingTotal.signum() > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onNewQuote(customer.id) }, modifier = Modifier.weight(1f)) { Text("New quote") }
                OutlinedButton(onClick = { onNewInvoice(customer.id) }, modifier = Modifier.weight(1f)) { Text("New invoice") }
                OutlinedButton(onClick = { onRecordPayment(customer.id) }, modifier = Modifier.weight(1f)) { Text("Payment") }
            }

            SectionHeader("Quotes")
            if (uiState.quotes.isEmpty()) {
                EmptyState("No quotes yet", "")
            } else {
                uiState.quotes.forEach { quote ->
                    ListItem(
                        headlineContent = { Text(quote.number ?: "Draft — not yet synced") },
                        supportingContent = { Text("${labelFor(QUOTE_STATUS_CHOICES, quote.status)} · ${formatZar(quote.total)}") },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenQuote(quote.id) },
                    )
                }
            }

            SectionHeader("Jobs")
            if (uiState.jobs.isEmpty()) {
                EmptyState("No jobs yet", "")
            } else {
                uiState.jobs.forEach { job ->
                    ListItem(
                        headlineContent = { Text(job.number ?: job.title) },
                        supportingContent = { Text(job.status.replace('_', ' ')) },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenJob(job.id) },
                    )
                }
            }

            SectionHeader("Invoices")
            if (uiState.invoices.isEmpty()) {
                EmptyState("No invoices yet", "")
            } else {
                uiState.invoices.forEach { invoice ->
                    ListItem(
                        headlineContent = { Text(invoice.number ?: "Draft — not yet synced") },
                        supportingContent = { Text("${labelFor(INVOICE_STATUS_CHOICES, invoice.status)} · ${formatZar(invoice.total)}") },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenInvoice(invoice.id) },
                    )
                }
            }

            SectionHeader("Notes")
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { noteDraft = it },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onUpdateNotes(noteDraft) },
                enabled = noteDraft != customer.notes,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            ) { Text("Save note") }
        }
    }
}
