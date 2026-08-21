package com.ops.app.ui.leads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
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
import com.ops.app.ui.components.LEAD_SOURCE_CHOICES
import com.ops.app.ui.components.LabeledDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLeadScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: NewLeadViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New lead") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Fast capture — just enough to follow up. You can add more later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                uiState.name,
                { viewModel.update { s -> s.copy(name = it) } },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                uiState.phone,
                { viewModel.update { s -> s.copy(phone = it) } },
                label = { Text("Phone") },
                placeholder = { Text("+27 82 123 4567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                uiState.email,
                { viewModel.update { s -> s.copy(email = it) } },
                label = { Text("Email (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            LabeledDropdown(
                label = "How did they find you?",
                options = LEAD_SOURCE_CHOICES,
                selected = uiState.source,
                onSelected = { viewModel.update { s -> s.copy(source = it) } },
            )
            OutlinedTextField(
                uiState.enquiry,
                { viewModel.update { s -> s.copy(enquiry = it) } },
                label = { Text("What do they need? (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                label = "Follow up on (optional)",
                value = uiState.followUpDate,
                onValueChange = { viewModel.update { s -> s.copy(followUpDate = it) } },
            )
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Save lead") }
        }
    }
}
