package com.ops.app.ui.invoices

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.FormSectionLabel
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.TotalsLine
import com.ops.app.ui.components.formatZar

@Composable
fun InvoiceEditScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: InvoiceEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InvoiceEditContent(
        uiState = uiState,
        onBack = onBack,
        onUpdate = viewModel::update,
        onUpdateLineItem = viewModel::updateLineItem,
        onAddLineItem = viewModel::addLineItem,
        onRemoveLineItem = viewModel::removeLineItem,
        onSave = { viewModel.save(onSaved) },
    )
}

/** Stateless render of [InvoiceEditScreen] — split out for the screenshot
 * pack (see android/README.md); not called from navigation directly.
 * Same fast-entry shape as [com.ops.app.ui.quotes.QuoteEditContent] —
 * line items pre-filled from the source quote when this invoice came from
 * one, a running total, one save action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditContent(
    uiState: InvoiceEditUiState,
    onBack: () -> Unit,
    onUpdate: ((InvoiceEditUiState) -> InvoiceEditUiState) -> Unit,
    onUpdateLineItem: (String, (InvoiceLineItemRow) -> InvoiceLineItemRow) -> Unit,
    onAddLineItem: () -> Unit,
    onRemoveLineItem: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.invoiceId == null) "New invoice" else uiState.number ?: "Draft invoice") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormSectionLabel("Customer")
            Text(uiState.customerName, style = MaterialTheme.typography.titleMedium)

            FormSectionLabel("Validity")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    "Issue date",
                    uiState.issueDate,
                    { if (it != null) onUpdate { s -> s.copy(issueDate = it) } },
                    modifier = Modifier.weight(1f),
                    clearable = false,
                )
                DateField("Due date", uiState.dueDate, { onUpdate { s -> s.copy(dueDate = it) } }, modifier = Modifier.weight(1f))
            }

            SectionHeader("Line items")
            uiState.lineItems.forEach { row ->
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = row.description,
                        onValueChange = { onUpdateLineItem(row.rowKey) { r -> r.copy(description = it) } },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = row.quantity,
                            onValueChange = { onUpdateLineItem(row.rowKey) { r -> r.copy(quantity = it) } },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = row.unitPrice,
                            onValueChange = { onUpdateLineItem(row.rowKey) { r -> r.copy(unitPrice = it) } },
                            label = { Text("Unit price (R)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveLineItem(row.rowKey) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove line")
                        }
                    }
                }
            }
            OutlinedButton(onClick = onAddLineItem, modifier = Modifier.fillMaxWidth()) { Text("+ Add line item") }

            FormSectionLabel("Totals")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.isVatApplicable, onCheckedChange = { onUpdate { s -> s.copy(isVatApplicable = it) } })
                Text("VAT applicable (15%)")
            }
            OutlinedTextField(
                value = uiState.discountAmount,
                onValueChange = { onUpdate { s -> s.copy(discountAmount = it) } },
                label = { Text("Discount (R)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            val totals = uiState.totals
            TotalsLine("Subtotal", formatZar(totals.subtotal))
            TotalsLine("VAT", formatZar(totals.vatAmount))
            TotalsLine("Total", formatZar(totals.total), emphasise = true)

            FormSectionLabel("Notes & terms (optional)")
            OutlinedTextField(uiState.notes, { onUpdate { s -> s.copy(notes = it) } }, label = { Text("Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(uiState.terms, { onUpdate { s -> s.copy(terms = it) } }, label = { Text("Terms") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            ) { Text("Save invoice") }
        }
    }
}
