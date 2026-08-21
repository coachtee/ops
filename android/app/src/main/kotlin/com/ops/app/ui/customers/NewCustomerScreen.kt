package com.ops.app.ui.customers

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.CUSTOMER_TYPE_CHOICES
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.PROVINCE_CHOICES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCustomerScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: NewCustomerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New customer") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(uiState.name, { viewModel.update { s -> s.copy(name = it) } }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            LabeledDropdown("Customer type", CUSTOMER_TYPE_CHOICES, uiState.customerType, { viewModel.update { s -> s.copy(customerType = it) } })
            OutlinedTextField(
                uiState.phone, { viewModel.update { s -> s.copy(phone = it) } },
                label = { Text("Phone") }, placeholder = { Text("+27 82 123 4567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                uiState.email, { viewModel.update { s -> s.copy(email = it) } },
                label = { Text("Email (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(uiState.addressLine1, { viewModel.update { s -> s.copy(addressLine1 = it) } }, label = { Text("Address line 1 (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(uiState.addressLine2, { viewModel.update { s -> s.copy(addressLine2 = it) } }, label = { Text("Address line 2 (optional)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(uiState.suburb, { viewModel.update { s -> s.copy(suburb = it) } }, label = { Text("Suburb") }, modifier = Modifier.weight(1f))
                OutlinedTextField(uiState.city, { viewModel.update { s -> s.copy(city = it) } }, label = { Text("City") }, modifier = Modifier.weight(1f))
            }
            LabeledDropdown("Province", PROVINCE_CHOICES, uiState.province, { viewModel.update { s -> s.copy(province = it) } })
            OutlinedTextField(
                uiState.postalCode, { viewModel.update { s -> s.copy(postalCode = it) } },
                label = { Text("Postal code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(uiState.notes, { viewModel.update { s -> s.copy(notes = it) } }, label = { Text("Notes (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Button(onClick = { viewModel.save(onSaved) }, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Save customer")
            }
        }
    }
}
