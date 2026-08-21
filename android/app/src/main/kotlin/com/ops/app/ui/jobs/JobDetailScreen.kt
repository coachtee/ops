package com.ops.app.ui.jobs

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.ActionableListRow
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.EXPENSE_CATEGORY_CHOICES
import com.ops.app.ui.components.INVOICE_STATUS_CHOICES
import com.ops.app.ui.components.JOB_STATUS_CHOICES
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.PAYMENT_METHOD_CHOICES
import com.ops.app.ui.components.QUOTE_STATUS_CHOICES
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.StatCard
import com.ops.app.ui.components.StatusBadge
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.invoiceStatusTone
import com.ops.app.ui.components.jobStatusTone
import com.ops.app.ui.components.labelFor
import com.ops.app.ui.components.quoteStatusTone

@Composable
fun JobDetailScreen(
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onOpenQuote: (String) -> Unit,
    onOpenInvoice: (String) -> Unit,
    onCreateInvoice: (customerId: String, jobId: String, quoteId: String?) -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    JobDetailContent(
        uiState = uiState,
        onBack = onBack,
        onOpenCustomer = onOpenCustomer,
        onOpenQuote = onOpenQuote,
        onOpenInvoice = onOpenInvoice,
        onCreateInvoice = onCreateInvoice,
        onUpdateStatus = viewModel::updateStatus,
        onUpdateDates = viewModel::updateDates,
    )
}

/** Stateless render of [JobDetailScreen] — split out for the screenshot
 * pack (see android/README.md); not called from navigation directly.
 *
 * A job sits in the middle of the commercial flow, so this is the one
 * screen that has to show its whole neighbourhood at once: the quote it
 * came from, the invoice(s) it's billed through, what's been paid, and
 * what it cost to do (expenses) — the "Job value / Paid / Outstanding"
 * figures the user asked for up top, everything else below. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailContent(
    uiState: JobDetailUiState,
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onOpenQuote: (String) -> Unit,
    onOpenInvoice: (String) -> Unit,
    onCreateInvoice: (customerId: String, jobId: String, quoteId: String?) -> Unit,
    onUpdateStatus: (String) -> Unit,
    onUpdateDates: (String?, String?) -> Unit,
) {
    val job = uiState.job

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(job?.number ?: job?.title ?: "Job") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        if (job == null) return@Scaffold

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(job.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Customer: ${uiState.customer?.name.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp).clickable(enabled = uiState.customer != null) {
                            uiState.customer?.let { onOpenCustomer(it.id) }
                        },
                    )
                }
                StatusBadge(labelFor(JOB_STATUS_CHOICES, job.status), jobStatusTone(job.status))
            }
            SyncStateBadge(job.syncState)
            if (job.description.isNotBlank()) Text(job.description)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Job value", formatZar(uiState.jobValue), Modifier.weight(1f))
                StatCard("Paid", formatZar(uiState.amountPaid), Modifier.weight(1f))
                StatCard("Outstanding", formatZar(uiState.outstanding), Modifier.weight(1f), emphasise = uiState.outstanding.signum() > 0)
            }

            LabeledDropdown("Status", JOB_STATUS_CHOICES, job.status, onUpdateStatus)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Start date", job.startDate, { onUpdateDates(it, job.dueDate) }, modifier = Modifier.weight(1f))
                DateField("Due date", job.dueDate, { onUpdateDates(job.startDate, it) }, modifier = Modifier.weight(1f))
            }

            SectionHeader("Quote")
            val quote = uiState.quote
            if (quote == null) {
                EmptyState("No quote linked", "This job wasn't created from an accepted quote.")
            } else {
                ActionableListRow(
                    primary = quote.number ?: "Draft — not yet synced",
                    secondary = "Issued ${formatDate(quote.issueDate)}",
                    statusBadge = { StatusBadge(labelFor(QUOTE_STATUS_CHOICES, quote.status), quoteStatusTone(quote.status)) },
                    trailingValue = formatZar(quote.total),
                    onClick = { onOpenQuote(quote.id) },
                )
            }

            SectionHeader("Invoices")
            if (uiState.invoices.isEmpty()) {
                EmptyState("No invoices yet", "Tap \"Create invoice\" below once the job is ready to bill.")
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
            Button(
                onClick = { onCreateInvoice(job.customerId, job.id, job.quoteId) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create invoice") }

            SectionHeader("Payments")
            if (uiState.payments.isEmpty()) {
                EmptyState("No payments yet", "Payments recorded against this job's invoices appear here.")
            } else {
                uiState.payments.sortedByDescending { it.paidDate }.forEach { payment ->
                    ActionableListRow(
                        primary = formatZar(payment.amount),
                        secondary = "${labelFor(PAYMENT_METHOD_CHOICES, payment.method)} · ${formatDate(payment.paidDate)}",
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            SectionHeader("Expenses")
            if (uiState.expenses.isEmpty()) {
                EmptyState("No expenses logged", "Materials, fuel and other costs logged against this job appear here.")
            } else {
                uiState.expenses.sortedByDescending { it.date }.forEach { expense ->
                    ActionableListRow(
                        primary = expense.description.ifBlank { labelFor(EXPENSE_CATEGORY_CHOICES, expense.category) },
                        secondary = "${labelFor(EXPENSE_CATEGORY_CHOICES, expense.category)} · ${formatDate(expense.date)}",
                        trailingValue = formatZar(expense.amount),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
