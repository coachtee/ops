package com.ops.app.ui.suppliers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.SyncStateBadge

@Composable
fun SupplierListScreen(
    onBack: () -> Unit,
    onOpenSupplier: (String) -> Unit,
    onNewSupplier: () -> Unit,
    viewModel: SupplierListViewModel = hiltViewModel(),
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    SupplierListContent(suppliers = suppliers, onBack = onBack, onOpenSupplier = onOpenSupplier, onNewSupplier = onNewSupplier)
}

/** Stateless render of [SupplierListScreen] — split out for the screenshot
 * pack (see android/README.md); not called from navigation directly. */
@Composable
fun SupplierListContent(
    suppliers: List<com.ops.app.data.local.entities.SupplierEntity>,
    onBack: () -> Unit,
    onOpenSupplier: (String) -> Unit,
    onNewSupplier: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suppliers") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewSupplier) { Icon(Icons.Filled.Add, contentDescription = "New supplier") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (suppliers.isEmpty()) {
                item {
                    EmptyState(
                        title = "No suppliers yet",
                        body = "Tap + to add who you buy materials or services from.",
                    )
                }
            } else {
                items(suppliers, key = { it.id }) { supplier ->
                    ListItem(
                        headlineContent = { Text(supplier.name) },
                        supportingContent = {
                            val subtitle = listOfNotNull(
                                supplier.contactPerson.ifBlank { null },
                                supplier.phone.ifBlank { null },
                            ).joinToString(" · ")
                            if (subtitle.isNotBlank()) Text(subtitle)
                        },
                        trailingContent = { SyncStateBadge(supplier.syncState) },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenSupplier(supplier.id) },
                    )
                }
            }
        }
    }
}
