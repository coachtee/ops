package com.ops.app.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.data.local.entities.EmployeeEntity
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.LabeledDropdown

private const val NO_EMPLOYEE = ""

@Composable
fun ScheduleVisitScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: ScheduleVisitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    ScheduleVisitContent(
        uiState = uiState,
        employees = employees,
        onBack = onBack,
        onUpdate = viewModel::update,
        onSave = { viewModel.save(onSaved) },
    )
}

/** Stateless render of [ScheduleVisitScreen] — split out for the screenshot
 * pack (see android/README.md); not called from navigation directly. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleVisitContent(
    uiState: ScheduleVisitUiState,
    employees: List<EmployeeEntity>,
    onBack: () -> Unit,
    onUpdate: ((ScheduleVisitUiState) -> ScheduleVisitUiState) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule visit") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateField(
                label = "Date",
                value = uiState.scheduledDate,
                onValueChange = { if (it != null) onUpdate { s -> s.copy(scheduledDate = it) } },
                clearable = false,
            )
            OutlinedTextField(
                value = uiState.startTime.orEmpty(),
                onValueChange = { onUpdate { s -> s.copy(startTime = it.ifBlank { null }) } },
                label = { Text("Time (optional)") },
                placeholder = { Text("e.g. 09:00") },
                modifier = Modifier.fillMaxWidth(),
            )
            LabeledDropdown(
                label = "Assign to (optional)",
                options = listOf(NO_EMPLOYEE to "Not assigned") + employees.map { it.id to it.name },
                selected = uiState.employeeId ?: NO_EMPLOYEE,
                onSelected = { onUpdate { s -> s.copy(employeeId = it.ifBlank { null }) } },
            )

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(if (uiState.isSaving) "Scheduling…" else "Schedule visit") }
        }
    }
}
