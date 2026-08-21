package com.ops.app.ui.employees

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.PAY_RATE_TYPE_CHOICES
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.labelFor
import java.math.BigDecimal

@Composable
fun EmployeeListScreen(
    onBack: () -> Unit,
    onOpenEmployee: (String) -> Unit,
    onNewEmployee: () -> Unit,
    viewModel: EmployeeListViewModel = hiltViewModel(),
) {
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    EmployeeListContent(employees = employees, onBack = onBack, onOpenEmployee = onOpenEmployee, onNewEmployee = onNewEmployee)
}

/** Stateless render of [EmployeeListScreen] — split out for the screenshot
 * pack (see android/README.md); not called from navigation directly. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListContent(
    employees: List<com.ops.app.data.local.entities.EmployeeEntity>,
    onBack: () -> Unit,
    onOpenEmployee: (String) -> Unit,
    onNewEmployee: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employees") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewEmployee) { Icon(Icons.Filled.Add, contentDescription = "New employee") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (employees.isEmpty()) {
                item {
                    EmptyState(
                        title = "No employees yet",
                        body = "Tap + to add who works for you and their agreed pay rate.",
                    )
                }
            } else {
                items(employees, key = { it.id }) { employee ->
                    ListItem(
                        headlineContent = { Text(employee.name) },
                        supportingContent = {
                            val rate = runCatching { BigDecimal(employee.payRate) }.getOrDefault(BigDecimal.ZERO)
                            val subtitle = listOfNotNull(
                                employee.role.ifBlank { null },
                                "${formatZar(rate)} ${labelFor(PAY_RATE_TYPE_CHOICES, employee.payRateType).lowercase()}",
                            ).joinToString(" · ")
                            Text(subtitle)
                        },
                        trailingContent = { SyncStateBadge(employee.syncState) },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenEmployee(employee.id) },
                    )
                }
            }
        }
    }
}
