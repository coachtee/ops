package com.ops.app.ui.customers

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.ActionableListRow
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.INVOICE_STATUS_CHOICES
import com.ops.app.ui.components.JOB_STATUS_CHOICES
import com.ops.app.ui.components.QUOTE_STATUS_CHOICES
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.StatCard
import com.ops.app.ui.components.StatusBadge
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.invoiceStatusTone
import com.ops.app.ui.components.jobStatusTone
import com.ops.app.ui.components.labelFor
import com.ops.app.ui.components.quoteStatusTone
import kotlinx.coroutines.launch

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
 * directly.
 *
 * The customer is the hub of the commercial flow: this screen is meant to
 * answer "where do things stand with this customer" in one glance — the
 * outstanding balance up top, then every Quote/Job/Invoice below it with
 * its own status, all one tap away. */
@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Customer") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (customer == null) return@Scaffold

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(customer.phone, style = MaterialTheme.typography.bodyMedium)
            if (customer.email.isNotBlank()) Text(customer.email, style = MaterialTheme.typography.bodyMedium)

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))) },
                    enabled = customer.phone.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Call, contentDescription = null); Text(" Call", modifier = Modifier.padding(start = 4.dp)) }
                OutlinedButton(
                    onClick = { launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("smsto:${customer.phone}"))) },
                    enabled = customer.phone.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Chat, contentDescription = null); Text(" WhatsApp", modifier = Modifier.padding(start = 4.dp)) }
            }

            StatCard(
                "Outstanding",
                formatZar(uiState.outstandingTotal),
                Modifier.fillMaxWidth().padding(top = 16.dp),
                emphasise = uiState.outstandingTotal.signum() > 0,
            )

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onNewQuote(customer.id) }, modifier = Modifier.weight(1f)) { Text("New quote") }
                Button(onClick = { onNewInvoice(customer.id) }, modifier = Modifier.weight(1f)) { Text("New invoice") }
                Button(onClick = { onRecordPayment(customer.id) }, modifier = Modifier.weight(1f)) { Text("Payment") }
            }

            SectionHeader("Quotes")
            if (uiState.quotes.isEmpty()) {
                EmptyState("No quotes yet", "Tap \"New quote\" above to put together a price for this customer.")
            } else {
                uiState.quotes.forEach { quote ->
                    ActionableListRow(
                        primary = quote.number ?: "Draft — not yet synced",
                        secondary = "Issued ${formatDate(quote.issueDate)}",
                        statusBadge = { StatusBadge(labelFor(QUOTE_STATUS_CHOICES, quote.status), quoteStatusTone(quote.status)) },
                        trailingValue = formatZar(quote.total),
                        onClick = { onOpenQuote(quote.id) },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            SectionHeader("Jobs")
            if (uiState.jobs.isEmpty()) {
                EmptyState("No jobs yet", "Jobs appear here once a quote is accepted.")
            } else {
                uiState.jobs.forEach { job ->
                    ActionableListRow(
                        primary = job.number ?: job.title,
                        secondary = if (job.number != null) job.title else "Draft — not yet synced",
                        statusBadge = { StatusBadge(labelFor(JOB_STATUS_CHOICES, job.status), jobStatusTone(job.status)) },
                        onClick = { onOpenJob(job.id) },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            SectionHeader("Invoices")
            if (uiState.invoices.isEmpty()) {
                EmptyState("No invoices yet", "Tap \"New invoice\" above, or create one from a job.")
            } else {
                uiState.invoices.forEach { invoice ->
                    ActionableListRow(
                        primary = invoice.number ?: "Draft — not yet synced",
                        secondary = "Due ${formatDate(invoice.dueDate)}",
                        statusBadge = { StatusBadge(labelFor(INVOICE_STATUS_CHOICES, invoice.status), invoiceStatusTone(invoice.status)) },
                        trailingValue = formatZar(invoice.total),
                        onClick = { onOpenInvoice(invoice.id) },
                        modifier = Modifier.padding(vertical = 4.dp),
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
