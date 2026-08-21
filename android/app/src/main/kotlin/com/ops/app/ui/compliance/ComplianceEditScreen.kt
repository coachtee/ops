package com.ops.app.ui.compliance

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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Switch
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.COMPLIANCE_CATEGORY_CHOICES
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.labelFor
import kotlinx.coroutines.launch
import java.time.LocalDate


@Composable
fun ComplianceEditScreen(
    onBack: () -> Unit,
    viewModel: ComplianceEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.itemId == null) "New compliance item" else "Compliance item") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (uiState.itemId != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete compliance item")
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

            Text(
                "OPS helps you track deadlines — it doesn't file anything with SARS or CIPC for you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LabeledDropdown(
                label = "Category",
                options = COMPLIANCE_CATEGORY_CHOICES,
                selected = uiState.category,
                onSelected = { viewModel.update { s -> s.copy(category = it) } },
            )

            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.update { s -> s.copy(title = it, titleError = null) } },
                label = { Text("Title") },
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(
                label = "Due date",
                value = uiState.dueDate,
                onValueChange = { picked -> picked?.let { viewModel.update { s -> s.copy(dueDate = it) } } },
                clearable = false,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Repeats", modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.isRecurring,
                    onCheckedChange = { viewModel.update { s -> s.copy(isRecurring = it) } },
                )
            }

            DateField(
                label = "Marked done on (optional)",
                value = uiState.completedDate,
                onValueChange = { picked -> viewModel.update { s -> s.copy(completedDate = picked) } },
            )
            if (uiState.completedDate == null) {
                OutlinedButton(
                    onClick = { viewModel.update { s -> s.copy(completedDate = LocalDate.now().toString()) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Mark done today") }
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
            title = { Text("Delete this compliance item?") },
            text = { Text("This removes it from your records. Anything already synced is removed there too.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; viewModel.delete(onBack) }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    val suggestion = uiState.nextItemSuggestion
    if (suggestion != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissNextItemSuggestion,
            title = { Text("Add the next reminder?") },
            text = {
                Text(
                    "${labelFor(COMPLIANCE_CATEGORY_CHOICES, suggestion.category)} — due ${formatDate(suggestion.dueDate)}. " +
                        "You can change the date after adding it.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmNextItem()
                    scope.launch { snackbarHostState.showSnackbar("Next reminder added") }
                }) { Text("Add") }
            },
            dismissButton = { OutlinedButton(onClick = viewModel::dismissNextItemSuggestion) { Text("Not now") } },
        )
    }
}
