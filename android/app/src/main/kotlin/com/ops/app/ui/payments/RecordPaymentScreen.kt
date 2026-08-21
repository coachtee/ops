package com.ops.app.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.PAYMENT_METHOD_CHOICES
import com.ops.app.ui.components.formatZar

@Composable
fun RecordPaymentScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RecordPaymentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record payment") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("From: ${uiState.customerName}", style = MaterialTheme.typography.titleMedium)
            if (uiState.invoiceNumber != null) {
                Text("Against invoice ${uiState.invoiceNumber}", style = MaterialTheme.typography.bodyMedium)
            } else if (uiState.invoiceId == null) {
                Text("On account (not tied to a specific invoice)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            uiState.outstandingOnInvoice?.let {
                Text("Outstanding: ${formatZar(it)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.update { s -> s.copy(amount = it) } },
                label = { Text("Amount (R)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            LabeledDropdown("Method", PAYMENT_METHOD_CHOICES, uiState.method, { viewModel.update { s -> s.copy(method = it) } })
            OutlinedTextField(
                value = uiState.reference,
                onValueChange = { viewModel.update { s -> s.copy(reference = it) } },
                label = { Text("Reference (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            DateField("Date paid", uiState.paidDate, { if (it != null) viewModel.update { s -> s.copy(paidDate = it) } }, clearable = false)
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.update { s -> s.copy(notes = it) } },
                label = { Text("Notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = uiState.canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Record payment") }
        }
    }
}
