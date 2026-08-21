package com.ops.app.ui.suppliers

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.EXPENSE_CATEGORY_CHOICES
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.labelFor
import kotlinx.coroutines.launch

/**
 * One screen for create, edit, and view (delete lives here too) — same
 * "always editable" spirit as ExpenseEditScreen/JobDetailScreen. Once
 * saved, shows this supplier's expense history read-only below the form
 * (see SupplierEditViewModel.linkedExpenses) — that's the "what have I
 * bought from them" answer, not a separate ledger screen.
 */
@Composable
fun SupplierEditScreen(
    onBack: () -> Unit,
    onOpenExpense: (String) -> Unit,
    viewModel: SupplierEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val linkedExpenses by viewModel.linkedExpenses.collectAsStateWithLifecycle()
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
                title = { Text(if (uiState.supplierId == null) "New supplier" else "Supplier") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (uiState.supplierId != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete supplier")
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
                label = { Text("Supplier name") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.contactPerson,
                onValueChange = { viewModel.update { s -> s.copy(contactPerson = it) } },
                label = { Text("Contact person (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.supplierId != null && (uiState.phone.isNotBlank() || uiState.email.isNotBlank())) {
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

            if (uiState.supplierId != null) {
                SectionHeader("Bought from them")
                if (linkedExpenses.isEmpty()) {
                    EmptyState("Nothing recorded yet", "Expenses linked to this supplier will show up here.")
                } else {
                    linkedExpenses.sortedByDescending { it.date }.forEach { expense ->
                        ListItem(
                            headlineContent = { Text(expense.description.ifBlank { labelFor(EXPENSE_CATEGORY_CHOICES, expense.category) }) },
                            supportingContent = { Text("${formatDate(expense.date)} · ${labelFor(EXPENSE_CATEGORY_CHOICES, expense.category)}") },
                            trailingContent = { Text(formatZar(expense.amount)) },
                            modifier = Modifier.fillMaxWidth().clickable { onOpenExpense(expense.id) },
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this supplier?") },
            text = { Text("This removes it from your records. Expenses already linked to it keep their history but won't show a supplier name.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; viewModel.delete(onBack) }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}
