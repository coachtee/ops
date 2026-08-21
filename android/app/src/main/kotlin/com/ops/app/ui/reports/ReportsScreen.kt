package com.ops.app.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.EXPENSE_CATEGORY_CHOICES
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.labelFor
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MONTH_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

/**
 * REPORTS bottom-nav tab: one scrollable screen, no further navigation —
 * profit by month, biggest expense categories this month, VAT collected
 * vs paid this month. Every figure here is computed from data already
 * synced locally (see ReportsViewModel), same offline-first way Home's
 * stat cards work — this screen never waits on the network.
 */
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsContent(uiState)
}

/** Stateless render of [ReportsScreen] — split out for the screenshot pack
 * (see android/README.md); not called from navigation directly. */
@Composable
fun ReportsContent(uiState: ReportsUiState) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item { SectionHeader("Profit by month") }
            item {
                Text(
                    "Revenue is what you were actually paid, not what you invoiced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(uiState.monthlyProfit, key = { it.month.toString() }) { row ->
                ListItem(
                    headlineContent = { Text(row.month.format(MONTH_LABEL)) },
                    supportingContent = { Text("Revenue ${formatZar(row.revenue)} · Expenses ${formatZar(row.expenses)}") },
                    trailingContent = {
                        Text(
                            formatZar(row.profit),
                            fontWeight = FontWeight.Bold,
                            color = if (row.profit.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }

            item { SectionHeader("Biggest expenses this month") }
            if (uiState.expenseCategoriesThisMonth.isEmpty()) {
                item { EmptyState("No expenses yet this month", "Recorded expenses will be broken down by category here.") }
            } else {
                items(uiState.expenseCategoriesThisMonth, key = { it.category }) { row ->
                    ListItem(
                        headlineContent = { Text(labelFor(EXPENSE_CATEGORY_CHOICES, row.category)) },
                        trailingContent = { Text(formatZar(row.total)) },
                    )
                }
            }

            item { SectionHeader("VAT this month") }
            item {
                Text(
                    "For your own VAT201 prep with your bookkeeper — OPS doesn't calculate or file VAT for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VAT collected")
                    Text(formatZar(uiState.vatCollectedThisMonth))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VAT paid")
                    Text(formatZar(uiState.vatPaidThisMonth))
                }
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net VAT position", fontWeight = FontWeight.Bold)
                    Text(formatZar(uiState.netVatPosition), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
