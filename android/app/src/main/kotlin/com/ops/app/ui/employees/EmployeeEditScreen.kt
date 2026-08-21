package com.ops.app.ui.employees

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.PAY_RATE_TYPE_CHOICES
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import kotlinx.coroutines.launch

/**
 * One screen for create, edit, and view (delete lives here too) — same
 * "always editable" spirit as SupplierEditScreen/ExpenseEditScreen. Once
 * saved, shows this employee's payslip history below the form, with a
 * "+ New payslip" action (see [EmployeeEditViewModel.linkedPayslips]).
 */
@Composable
fun EmployeeEditScreen(
    onBack: () -> Unit,
    onOpenPayslip: (employeeId: String, payslipId: String) -> Unit,
    onNewPayslip: (employeeId: String) -> Unit,
    viewModel: EmployeeEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val linkedPayslips by viewModel.linkedPayslips.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                title = { Text(if (uiState.employeeId == null) "New employee" else "Employee") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (uiState.employeeId != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete employee")
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
            uiState.syncState?.let { SyncStateBadge(it) }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.update { s -> s.copy(name = it, nameError = null) } },
                label = { Text("Employee name") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.role,
                onValueChange = { viewModel.update { s -> s.copy(role = it) } },
                label = { Text("Role (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.employeeId != null && (uiState.phone.isNotBlank() || uiState.email.isNotBlank())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${uiState.phone}"))) },
                        enabled = uiState.phone.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Filled.Call, contentDescription = null); Text(" Call", modifier = Modifier.padding(start = 4.dp)) }
                    OutlinedButton(
                        onClick = { launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("smsto:${uiState.phone}"))) },
                        enabled = uiState.phone.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Filled.Chat, contentDescription = null); Text(" WhatsApp", modifier = Modifier.padding(start = 4.dp)) }
                    OutlinedButton(
                        onClick = { launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:${uiState.email}"))) },
                        enabled = uiState.email.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Filled.Email, contentDescription = null); Text(" Email", modifier = Modifier.padding(start = 4.dp)) }
                }
            }

            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { viewModel.update { s -> s.copy(phone = it) } },
                label = { Text("Phone (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.update { s -> s.copy(email = it) } },
                label = { Text("Email (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledDropdown(
                label = "Pay rate type",
                options = PAY_RATE_TYPE_CHOICES,
                selected = uiState.payRateType,
                onSelected = { viewModel.update { s -> s.copy(payRateType = it) } },
            )

            OutlinedTextField(
                value = uiState.payRate,
                onValueChange = { viewModel.update { s -> s.copy(payRate = it) } },
                label = { Text("Agreed pay rate (R)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "A reminder of what you agreed — payslips still need their own gross pay entered each time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DateField(
                label = "Start date (optional)",
                value = uiState.startDate,
                onValueChange = { picked -> viewModel.update { s -> s.copy(startDate = picked) } },
            )

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

            val employeeId = uiState.employeeId
            if (employeeId != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    SectionHeader("Payslips", modifier = Modifier.weight(1f))
                    IconButton(onClick = { onNewPayslip(employeeId) }) { Icon(Icons.Filled.Add, contentDescription = "New payslip") }
                }
                if (linkedPayslips.isEmpty()) {
                    EmptyState("No payslips yet", "Tap + to record this employee's first pay period.")
                } else {
                    linkedPayslips.forEach { payslip ->
                        ListItem(
                            headlineContent = { Text("${formatDate(payslip.periodStart)} – ${formatDate(payslip.periodEnd)}") },
                            supportingContent = { Text(if (payslip.paidDate != null) "Paid ${formatDate(payslip.paidDate)}" else "Not yet paid") },
                            trailingContent = { Text(formatZar(payslip.netPay)) },
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPayslip(employeeId, payslip.id) },
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this employee?") },
            text = { Text("This removes them from your records. Their payslip history stays intact.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; viewModel.delete(onBack) }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}
