package com.ops.app.ui.money

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.EXPENSE_CATEGORY_CHOICES
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.INVOICE_STATUS_CHOICES
import com.ops.app.ui.components.PAYMENT_METHOD_CHOICES
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.labelFor

@Composable
fun MoneyScreen(
    onOpenInvoice: (String) -> Unit,
    onOpenExpense: (String) -> Unit,
    onNewExpense: () -> Unit,
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Money") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewExpense) { Icon(Icons.Filled.Add, contentDescription = "Record expense") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item { SectionHeader("Outstanding, oldest first") }
            if (uiState.outstandingInvoices.isEmpty()) {
                item { EmptyState("Nothing outstanding", "Every sent invoice is paid up.") }
            } else {
                items(uiState.outstandingInvoices, key = { it.id }) { invoice ->
                    val outstanding = runCatching {
                        java.math.BigDecimal(invoice.total).subtract(java.math.BigDecimal(invoice.amountPaid))
                    }.getOrDefault(java.math.BigDecimal.ZERO)
                    ListItem(
                        headlineContent = { Text(uiState.customerNames[invoice.customerId] ?: "Customer") },
                        supportingContent = { Text("${invoice.number ?: "Draft"} · ${labelFor(INVOICE_STATUS_CHOICES, invoice.status)} · issued ${formatDate(invoice.issueDate)}") },
                        trailingContent = { Text(formatZar(outstanding), color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenInvoice(invoice.id) },
                    )
                }
            }

            item { SectionHeader("Payments received") }
            if (uiState.paymentsReceived.isEmpty()) {
                item { EmptyState("No payments yet", "Recorded payments will show up here.") }
            } else {
                items(uiState.paymentsReceived, key = { it.id }) { payment ->
                    ListItem(
                        headlineContent = { Text(uiState.customerNames[payment.customerId] ?: "Customer") },
                        supportingContent = { Text("${formatDate(payment.paidDate)} · ${labelFor(PAYMENT_METHOD_CHOICES, payment.method)}") },
                        trailingContent = { Text(formatZar(payment.amount)) },
                        modifier = Modifier.fillMaxWidth().let { m ->
                            if (payment.invoiceId != null) m.clickable { onOpenInvoice(payment.invoiceId) } else m
                        },
                    )
                }
            }

            item { SectionHeader("Expenses") }
            if (uiState.expenses.isEmpty()) {
                item { EmptyState("No expenses recorded", "Tap + to record money spent on materials, fuel, and more.") }
            } else {
                items(uiState.expenses, key = { it.id }) { expense ->
                    ListItem(
                        headlineContent = { Text(expense.description.ifBlank { labelFor(EXPENSE_CATEGORY_CHOICES, expense.category) }) },
                        supportingContent = { Text("${formatDate(expense.date)} · ${labelFor(EXPENSE_CATEGORY_CHOICES, expense.category)}") },
                        trailingContent = { Text(formatZar(expense.amount)) },
                        overlineContent = { SyncStateBadge(expense.syncState) },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenExpense(expense.id) },
                    )
                }
            }
        }
    }
}
