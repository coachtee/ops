package com.ops.app.ui.invoices

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.data.local.entities.BusinessEntity
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.InvoiceLineItemEntity
import com.ops.app.ui.components.BusinessLetterhead
import com.ops.app.ui.components.INVOICE_STATUS_CHOICES
import com.ops.app.ui.components.TotalsLine
import com.ops.app.ui.components.addressLines
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.labelFor
import java.math.BigDecimal

@Composable
fun InvoicePreviewScreen(
    onBack: () -> Unit,
    onRecordPayment: (customerId: String, invoiceId: String) -> Unit,
    viewModel: InvoicePreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InvoicePreviewContent(
        uiState = uiState,
        onBack = onBack,
        onRecordPayment = onRecordPayment,
        onMarkSent = viewModel::markSent,
    )
}

/** Stateless render of [InvoicePreviewScreen] — split out for the
 * screenshot pack (see android/README.md); not called from navigation
 * directly. */
@Composable
fun InvoicePreviewContent(
    uiState: InvoicePreviewUiState,
    onBack: () -> Unit,
    onRecordPayment: (customerId: String, invoiceId: String) -> Unit,
    onMarkSent: () -> Unit,
) {
    val context = LocalContext.current
    val invoice = uiState.invoice

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(invoice?.number ?: "Draft invoice") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    // "Who owes me money" -> tap an outstanding invoice -> call
                    // the customer straight from the invoice (DISCOVERY.md,
                    // main user journey 4). No CALL_PHONE permission needed —
                    // ACTION_DIAL just opens the dialer pre-filled.
                    val customerPhone = uiState.customer?.phone
                    if (!customerPhone.isNullOrBlank()) {
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$customerPhone")))
                        }) { Icon(Icons.Filled.Call, contentDescription = "Call customer") }
                    }
                    IconButton(onClick = {
                        val text = buildShareText(uiState.business, uiState.customer, invoice, uiState.lineItems)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Invoice ${invoice?.number.orEmpty()}")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Send invoice"))
                        onMarkSent()
                    }) { Icon(Icons.Filled.Share, contentDescription = "Send") }
                },
            )
        },
    ) { padding ->
        if (invoice == null) return@Scaffold
        val outstanding = runCatching { BigDecimal(invoice.total).subtract(BigDecimal(invoice.amountPaid)) }.getOrDefault(BigDecimal.ZERO)

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            BusinessLetterhead(uiState.business)

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("INVOICE", style = MaterialTheme.typography.titleLarge)
                    Text(invoice.number ?: "Draft — not yet synced", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Issued ${formatDate(invoice.issueDate)}", style = MaterialTheme.typography.bodyMedium)
                    if (invoice.dueDate != null) Text("Due ${formatDate(invoice.dueDate)}", style = MaterialTheme.typography.bodyMedium)
                    Text(labelFor(INVOICE_STATUS_CHOICES, invoice.status), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            Text("Bill to", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            Text(uiState.customer?.name.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            uiState.customer?.let { c ->
                if (c.phone.isNotBlank()) Text(c.phone)
                addressLines(c.addressLine1, c.addressLine2, c.suburb, c.city, c.postalCode).forEach { Text(it) }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            uiState.lineItems.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(item.description)
                        Text("${item.quantity} × ${formatZar(item.unitPrice)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatZar(item.lineTotal))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            TotalsLine("Subtotal", formatZar(invoice.subtotal))
            if (invoice.discountAmount != "0.00") TotalsLine("Discount", "-${formatZar(invoice.discountAmount)}")
            if (invoice.isVatApplicable) TotalsLine("VAT (15%)", formatZar(invoice.vatAmount))
            TotalsLine("Total", formatZar(invoice.total), emphasise = true)
            TotalsLine("Paid", formatZar(invoice.amountPaid))
            TotalsLine("Outstanding", formatZar(outstanding), emphasise = true)

            if (uiState.payments.isNotEmpty()) {
                Text("Payments", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
                uiState.payments.forEach { payment ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${formatDate(payment.paidDate)} · ${payment.method}")
                        Text(formatZar(payment.amount))
                    }
                }
            }

            if (invoice.notes.isNotBlank()) {
                Text("Notes", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
                Text(invoice.notes)
            }
            if (invoice.terms.isNotBlank()) {
                Text("Terms", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                Text(invoice.terms)
            }

            Button(
                onClick = { onRecordPayment(invoice.customerId, invoice.id) },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text("Record payment") }
        }
    }
}

private fun buildShareText(
    business: BusinessEntity?,
    customer: CustomerEntity?,
    invoice: InvoiceEntity?,
    lineItems: List<InvoiceLineItemEntity>,
): String {
    if (invoice == null) return ""
    val sb = StringBuilder()
    sb.appendLine(business?.name.orEmpty())
    sb.appendLine("Invoice ${invoice.number ?: "(draft)"}")
    sb.appendLine("For: ${customer?.name.orEmpty()}")
    sb.appendLine()
    lineItems.forEach { sb.appendLine("${it.description} — ${it.quantity} x ${formatZar(it.unitPrice)} = ${formatZar(it.lineTotal)}") }
    sb.appendLine()
    sb.appendLine("Subtotal: ${formatZar(invoice.subtotal)}")
    if (invoice.isVatApplicable) sb.appendLine("VAT: ${formatZar(invoice.vatAmount)}")
    sb.appendLine("Total: ${formatZar(invoice.total)}")
    sb.appendLine("Paid: ${formatZar(invoice.amountPaid)}")
    return sb.toString()
}
