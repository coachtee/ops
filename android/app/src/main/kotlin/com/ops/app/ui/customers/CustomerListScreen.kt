package com.ops.app.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.EmptyState

/** [isPicking] is true when this screen was reached via a Home quick action
 * that needs a customer first (New quote / New invoice / Record payment) —
 * see OpsDestinations.CUSTOMERS_PICKABLE. It only changes the title/hint
 * text and whether the FAB is shown; the list itself is identical. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    isPicking: Boolean,
    onOpenCustomer: (String) -> Unit,
    onNewCustomer: () -> Unit,
    viewModel: CustomerListViewModel = hiltViewModel(),
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (isPicking) "Choose a customer" else "Customers") }) },
        floatingActionButton = {
            if (!isPicking) {
                FloatingActionButton(onClick = onNewCustomer) { Icon(Icons.Filled.Add, contentDescription = "New customer") }
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Search customers") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            if (customers.isEmpty()) {
                item {
                    EmptyState(
                        title = "No customers yet",
                        body = "Convert a lead, or add a customer directly from here.",
                    )
                }
            } else {
                items(customers, key = { it.id }) { customer ->
                    ListItem(
                        headlineContent = { Text(customer.name) },
                        supportingContent = { Text(listOfNotNull(customer.phone.ifBlank { null }, customer.city.ifBlank { null }).joinToString(" · ")) },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenCustomer(customer.id) },
                    )
                }
            }
        }
    }
}
