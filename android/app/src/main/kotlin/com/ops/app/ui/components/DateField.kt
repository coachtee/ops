package com.ops.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset

/** A read-only text field that opens a Material date picker — used for every
 * `YYYY-MM-DD` field in this app (lead follow-up date, quote/invoice dates,
 * job dates, payment date). [value] and the value passed to [onValueChange]
 * are both `YYYY-MM-DD` strings; null means "not set". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = formatDate(value, fallback = ""),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
            }
        },
        modifier = modifier.fillMaxWidth(),
    )

    if (showPicker) {
        val initialMillis = value?.let {
            runCatching { java.time.LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onValueChange(date.toString())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                if (clearable && value != null) {
                    TextButton(onClick = { onValueChange(null); showPicker = false }) { Text("Clear") }
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** The compact "[Follow up]" quick action used on lead rows and the lead
 * detail header — same date-picker mechanics as [DateField], but as a
 * single button rather than a field, so a list row can offer it alongside
 * Call/WhatsApp without a text field's visual weight. [onDateSelected]
 * receives the chosen date as a `YYYY-MM-DD` string. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickFollowUpButton(
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Follow up",
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showPicker = true }, modifier = modifier, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(" $label", style = MaterialTheme.typography.labelLarge)
    }

    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(date.toString())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}
