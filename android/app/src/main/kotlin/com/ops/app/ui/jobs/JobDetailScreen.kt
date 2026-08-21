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
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.INVOICE_STATUS_CHOICES
import com.ops.app.ui.components.JOB_STATUS_CHOICES
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.labelFor

@Composable
fun JobDetailScreen(
    onBack: () -> Unit,
    onOpenInvoice: (String) -> Unit,
    onCreateInvoice: (customerId: String, jobId: String, quoteId: String?) -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            SyncStateBadge(job.syncState)
            Text(job.title, style = MaterialTheme.typography.titleMedium)
            Text("Customer: ${uiState.customer?.name.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
            if (job.description.isNotBlank()) Text(job.description)

            LabeledDropdown("Status", JOB_STATUS_CHOICES, job.status, viewModel::updateStatus)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Start date", job.startDate, { viewModel.updateDates(it, job.dueDate) }, modifier = Modifier.weight(1f))
                DateField("Due date", job.dueDate, { viewModel.updateDates(job.startDate, it) }, modifier = Modifier.weight(1f))
            }

            SectionHeader("Invoices")
            uiState.invoices.forEach { invoice ->
                ListItem(
                    headlineContent = { Text(invoice.number ?: "Draft — not yet synced") },
                    supportingContent = { Text("${labelFor(INVOICE_STATUS_CHOICES, invoice.status)} · ${formatZar(invoice.total)}") },
                    modifier = Modifier.fillMaxWidth().clickable { onOpenInvoice(invoice.id) },
                )
            }

            Button(
                onClick = { onCreateInvoice(job.customerId, job.id, job.quoteId) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Create invoice") }
        }
    }
}
