package com.ops.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ops.app.data.local.entities.BusinessEntity

/**
 * Branded header shown at the top of a quote/invoice preview — the
 * business's own uploaded logo (or a plain circle with its initial, if none)
 * plus name/address/contact, matching what the backend's HTML render would
 * show (see DISCOVERY.md section 7: "the same HTML is what the Android
 * WebView-based preview shows before Send" — this app renders the equivalent
 * natively in Compose rather than loading a WebView, same information, one
 * fewer moving part offline). This is the business's OWN branding — separate
 * from the OPS app's own "O" brand mark used on Splash/launcher.
 */
@Composable
fun BusinessLetterhead(business: BusinessEntity?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (business?.logoUrl != null) {
            AsyncImage(model = business.logoUrl, contentDescription = "${business.name} logo", modifier = Modifier.size(48.dp))
        } else {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(business?.name?.firstOrNull()?.toString() ?: "O", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(business?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
            addressLines(business?.addressLine1.orEmpty(), business?.addressLine2.orEmpty(), business?.suburb.orEmpty(), business?.city.orEmpty(), business?.postalCode.orEmpty())
                .forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (!business?.phone.isNullOrBlank()) {
                Text(business!!.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (business?.isVatRegistered == true && business.vatNumber.isNotBlank()) {
                Text("VAT: ${business.vatNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TotalsLine(label: String, value: String, emphasise: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        Text(value, style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
    }
}

/** Non-blank address lines, joined the way a letterhead expects: line1,
 * line2, then "suburb, city, postal code" on one line. */
fun addressLines(vararg parts: String): List<String> {
    val line1 = parts.getOrNull(0).orEmpty()
    val line2 = parts.getOrNull(1).orEmpty()
    val cityLine = listOfNotNull(
        parts.getOrNull(2)?.takeIf { it.isNotBlank() },
        parts.getOrNull(3)?.takeIf { it.isNotBlank() },
        parts.getOrNull(4)?.takeIf { it.isNotBlank() },
    ).joinToString(", ")
    return listOfNotNull(line1.takeIf { it.isNotBlank() }, line2.takeIf { it.isNotBlank() }, cityLine.takeIf { it.isNotBlank() })
}
