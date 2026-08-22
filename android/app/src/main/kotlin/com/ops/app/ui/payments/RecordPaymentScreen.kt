package com.ops.app.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.ops.app.ui.components.FormSectionLabel
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.PAYMENT_METHOD_CHOICES
import com.ops.app.ui.components.StatCard
import com.ops.app.ui.components.formatZar
import java.math.BigDecimal

@Composable
fun RecordPaymentScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RecordPaymentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecordPaymentContent(
        uiState = uiState,
        onBack = onBack,
        onUpdate = viewModel::update,
        onSave = { viewModel.save(onSaved) },
    )
}

/** Stateless render of [RecordPaymentScreen] — split out for the
 * screenshot pack (see android/README.md); not called from navigation
 * directly. The simplest form in the app on purpose: when an invoice is
 * known, "Invoice total / Already paid / Remaining" is always on screen
 * above the amount field, and Remaining recomputes on every keystroke
 * (see [RecordPaymentUiState.remainingAfterThisPayment]) — the user should
 * never have to do the subtraction themselves. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentContent(
    uiState: RecordPaymentUiState,
    onBack: () -> Unit,
    onUpdate: ((RecordPaymentUiState) -> RecordPaymentUiState) -> Unit,
    onSave: () -> Unit,
) {
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
            FormSectionLabel("From")
            Text(uiState.customerName, style = MaterialTheme.typography.titleMedium)

            if (uiState.invoiceNumber != null) {
                Text("Against invoice ${uiState.invoiceNumber}", style = MaterialTheme.typography.bodyMedium)
            } else if (uiState.invoiceId == null) {
                Text("On account (not tied to a specific invoice)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (uiState.invoiceTotal != null && uiState.alreadyPaid != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Invoice total", formatZar(uiState.invoiceTotal), Modifier.weight(1f))
                    StatCard("Already paid", formatZar(uiState.alreadyPaid), Modifier.weight(1f))
                    val remaining = uiState.remainingAfterThisPayment ?: uiState.outstandingOnInvoice ?: BigDecimal.ZERO
                    StatCard("Remaining", formatZar(remaining), Modifier.weight(1f), emphasise = remaining.signum() > 0)
                }
            }

            FormSectionLabel("Payment details")
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { onUpdate { s -> s.copy(amount = it) } },
                label = { Text("Amount (R)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            LabeledDropdown("Method", PAYMENT_METHOD_CHOICES, uiState.method, { onUpdate { s -> s.copy(method = it) } })
            OutlinedTextField(
                value = uiState.reference,
                onValueChange = { onUpdate { s -> s.copy(reference = it) } },
                label = { Text("Reference (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            DateField("Date paid", uiState.paidDate, { if (it != null) onUpdate { s -> s.copy(paidDate = it) } }, clearable = false)
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { onUpdate { s -> s.copy(notes = it) } },
                label = { Text("Notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSave,
                enabled = uiState.canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            ) { Text("Record payment") }
        }
    }
}
