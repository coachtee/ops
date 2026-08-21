package com.ops.app.ui.quotes

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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.formatZar
import com.ops.coredomain.QuoteStatus

@Composable
fun QuoteEditScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: QuoteEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.quoteId == null) "New quote" else uiState.number ?: "Draft quote") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("For: ${uiState.customerName}", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Issue date", uiState.issueDate, { if (it != null) viewModel.update { s -> s.copy(issueDate = it) } }, modifier = Modifier.weight(1f), clearable = false)
                DateField("Valid until", uiState.validUntil, { viewModel.update { s -> s.copy(validUntil = it) } }, modifier = Modifier.weight(1f))
            }

            SectionHeader("Line items")
            uiState.lineItems.forEach { row ->
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = row.description,
                        onValueChange = { viewModel.updateLineItem(row.rowKey) { r -> r.copy(description = it) } },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = row.quantity,
                            onValueChange = { viewModel.updateLineItem(row.rowKey) { r -> r.copy(quantity = it) } },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = row.unitPrice,
                            onValueChange = { viewModel.updateLineItem(row.rowKey) { r -> r.copy(unitPrice = it) } },
                            label = { Text("Unit price (R)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.removeLineItem(row.rowKey) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove line")
                        }
                    }
                }
            }
            OutlinedButton(onClick = viewModel::addLineItem, modifier = Modifier.fillMaxWidth()) { Text("+ Add line item") }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = uiState.isVatApplicable, onCheckedChange = { viewModel.update { s -> s.copy(isVatApplicable = it) } })
                Text("VAT applicable (15%)")
            }
            OutlinedTextField(
                value = uiState.discountAmount,
                onValueChange = { viewModel.update { s -> s.copy(discountAmount = it) } },
                label = { Text("Discount (R)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            val totals = uiState.totals
            TotalsRow("Subtotal", formatZar(totals.subtotal))
            TotalsRow("VAT", formatZar(totals.vatAmount))
            TotalsRow("Total", formatZar(totals.total), emphasise = true)

            OutlinedTextField(uiState.notes, { viewModel.update { s -> s.copy(notes = it) } }, label = { Text("Notes (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(uiState.terms, { viewModel.update { s -> s.copy(terms = it) } }, label = { Text("Terms (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = { viewModel.save(asStatus = QuoteStatus.DRAFT.wire, onSaved = onSaved) },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            ) { Text("Save quote") }
        }
    }
}

@Composable
private fun TotalsRow(label: String, value: String, emphasise: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        Text(value, style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
    }
}
