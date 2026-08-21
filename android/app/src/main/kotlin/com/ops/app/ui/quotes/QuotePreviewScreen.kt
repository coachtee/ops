package com.ops.app.ui.quotes

import android.content.Intent
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.data.local.entities.BusinessEntity
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.local.entities.QuoteLineItemEntity
import com.ops.app.ui.components.BusinessLetterhead
import com.ops.app.ui.components.QUOTE_STATUS_CHOICES
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.StatusBadge
import com.ops.app.ui.components.TotalsLine
import com.ops.app.ui.components.addressLines
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.formatZar
import com.ops.app.ui.components.labelFor
import com.ops.app.ui.components.quoteStatusTone
import com.ops.coredomain.QuoteStatus

@Composable
fun QuotePreviewScreen(
    onBack: () -> Unit,
    onJobReady: (String) -> Unit,
    viewModel: QuotePreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    QuotePreviewContent(
        uiState = uiState,
        onBack = onBack,
        onMarkSent = viewModel::markSent,
        onMarkDeclined = viewModel::markDeclined,
        onMarkAccepted = { viewModel.markAccepted(onJobReady) },
    )
}

/** Stateless render of [QuotePreviewScreen] — split out for the screenshot
 * pack (see android/README.md); not called from navigation directly.
 *
 * The bottom action row is contextual to [quote]'s status, same as the
 * status badge in the header: a draft only offers "Send quote"; once sent,
 * the only two things left to record are the customer's Accepted/Declined
 * answer; once decided, there is nothing left to do here — the record
 * speaks for itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotePreviewContent(
    uiState: QuotePreviewUiState,
    onBack: () -> Unit,
    onMarkSent: () -> Unit,
    onMarkDeclined: () -> Unit,
    onMarkAccepted: () -> Unit,
) {
    val context = LocalContext.current
    val quote = uiState.quote

    fun shareQuote() {
        val text = buildShareText(uiState.business, uiState.customer, quote, uiState.lineItems)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Quote ${quote?.number.orEmpty()}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Send quote"))
        onMarkSent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quote?.number ?: "Draft quote") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { shareQuote() }) { Icon(Icons.Filled.Share, contentDescription = "Share quote") }
                },
            )
        },
    ) { padding ->
        if (quote == null) return@Scaffold

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            BusinessLetterhead(uiState.business)

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("QUOTE", style = MaterialTheme.typography.titleLarge)
                    Text(quote.number ?: "Draft — not yet synced", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Issued ${formatDate(quote.issueDate)}", style = MaterialTheme.typography.bodyMedium)
                    if (quote.validUntil != null) Text("Valid until ${formatDate(quote.validUntil)}", style = MaterialTheme.typography.bodyMedium)
                    StatusBadge(
                        labelFor(QUOTE_STATUS_CHOICES, quote.status),
                        quoteStatusTone(quote.status),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            SectionHeader("Bill to")
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

            TotalsLine("Subtotal", formatZar(quote.subtotal))
            if (quote.discountAmount != "0.00") TotalsLine("Discount", "-${formatZar(quote.discountAmount)}")
            if (quote.isVatApplicable) TotalsLine("VAT (15%)", formatZar(quote.vatAmount))
            TotalsLine("Total", formatZar(quote.total), emphasise = true)

            if (quote.notes.isNotBlank()) {
                SectionHeader("Notes")
                Text(quote.notes)
            }
            if (quote.terms.isNotBlank()) {
                SectionHeader("Terms")
                Text(quote.terms)
            }

            when (quote.status) {
                QuoteStatus.DRAFT.wire -> {
                    Button(onClick = { shareQuote() }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Text(" Send quote", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                QuoteStatus.SENT.wire -> {
                    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onMarkDeclined, modifier = Modifier.weight(1f)) { Text("Declined") }
                        Button(onClick = onMarkAccepted, modifier = Modifier.weight(1f)) { Text("Accepted") }
                    }
                }
                else -> Unit
            }
        }
    }
}

private fun buildShareText(
    business: BusinessEntity?,
    customer: CustomerEntity?,
    quote: QuoteEntity?,
    lineItems: List<QuoteLineItemEntity>,
): String {
    if (quote == null) return ""
    val sb = StringBuilder()
    sb.appendLine(business?.name.orEmpty())
    sb.appendLine("Quote ${quote.number ?: "(draft)"}")
    sb.appendLine("For: ${customer?.name.orEmpty()}")
    sb.appendLine()
    lineItems.forEach { sb.appendLine("${it.description} — ${it.quantity} x ${formatZar(it.unitPrice)} = ${formatZar(it.lineTotal)}") }
    sb.appendLine()
    sb.appendLine("Subtotal: ${formatZar(quote.subtotal)}")
    if (quote.isVatApplicable) sb.appendLine("VAT: ${formatZar(quote.vatAmount)}")
    sb.appendLine("Total: ${formatZar(quote.total)}")
    return sb.toString()
}
