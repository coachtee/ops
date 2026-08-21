package com.ops.app.ui.employees

import android.content.Intent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import kotlinx.coroutines.launch

@Composable
fun PayslipEditScreen(
    onBack: () -> Unit,
    viewModel: PayslipEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.payslipId == null) "New payslip" else "Payslip") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (uiState.payslipId != null) {
                        IconButton(onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Payslip — ${formatDate(uiState.periodStart)} to ${formatDate(uiState.periodEnd)}")
                                putExtra(Intent.EXTRA_TEXT, buildShareText(uiState))
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share payslip"))
                        }) { Icon(Icons.Filled.Share, contentDescription = "Share payslip") }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete payslip")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) return@Scaffold

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.employeeName.isNotBlank()) {
                Text(uiState.employeeName, style = MaterialTheme.typography.titleMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DateField(
                    label = "Period start",
                    value = uiState.periodStart,
                    onValueChange = { picked -> picked?.let { viewModel.update { s -> s.copy(periodStart = it, periodError = null) } } },
                    clearable = false,
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    label = "Period end",
                    value = uiState.periodEnd,
                    onValueChange = { picked -> picked?.let { viewModel.update { s -> s.copy(periodEnd = it, periodError = null) } } },
                    clearable = false,
                    modifier = Modifier.weight(1f),
                )
            }
            uiState.periodError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            OutlinedTextField(
                value = uiState.grossPay,
                onValueChange = { viewModel.update { s -> s.copy(grossPay = it, grossPayError = null) } },
                label = { Text("Gross pay (R)") },
                isError = uiState.grossPayError != null,
                supportingText = uiState.grossPayError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.deductions,
                onValueChange = { viewModel.update { s -> s.copy(deductions = it, deductionsError = null) } },
                label = { Text("Deductions (R)") },
                isError = uiState.deductionsError != null,
                supportingText = uiState.deductionsError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.deductionsNote,
                onValueChange = { viewModel.update { s -> s.copy(deductionsNote = it) } },
                label = { Text("What the deductions are for (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Net pay: ${formatZar(uiState.netPay)} — this app doesn't calculate PAYE/UIF for you; enter what your bookkeeper tells you.",
                style = MaterialTheme.typography.bodyMedium,
            )

            DateField(
                label = "Paid date (optional)",
                value = uiState.paidDate,
                onValueChange = { picked -> viewModel.update { s -> s.copy(paidDate = picked) } },
            )
            if (uiState.paidDate == null) {
                OutlinedButton(onClick = viewModel::markPaidToday, modifier = Modifier.fillMaxWidth()) {
                    Text("Mark as paid today")
                }
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.update { s -> s.copy(notes = it) } },
                label = { Text("Notes (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    viewModel.save {
                        scope.launch { snackbarHostState.showSnackbar("Saved") }
                    }
                },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(if (uiState.isSaving) "Saving…" else "Save") }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this payslip?") },
            text = { Text("This removes it from your records. Anything already synced is removed there too.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; viewModel.delete(onBack) }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

private fun buildShareText(state: PayslipEditUiState): String = buildString {
    appendLine(state.employeeName)
    appendLine("Pay period: ${formatDate(state.periodStart)} to ${formatDate(state.periodEnd)}")
    appendLine("Gross pay: ${formatZar(state.grossPay.toBigDecimalOrZero())}")
    appendLine("Deductions: ${formatZar(state.deductions.toBigDecimalOrZero())}${if (state.deductionsNote.isNotBlank()) " (${state.deductionsNote})" else ""}")
    appendLine("Net pay: ${formatZar(state.netPay)}")
    append(if (state.paidDate != null) "Paid: ${formatDate(state.paidDate)}" else "Not yet paid")
}

private fun String.toBigDecimalOrZero() = runCatching { java.math.BigDecimal(this) }.getOrDefault(java.math.BigDecimal.ZERO)
