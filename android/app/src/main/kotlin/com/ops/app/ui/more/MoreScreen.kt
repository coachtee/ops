package com.ops.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private data class MoreItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** The fifth bottom-nav slot's landing screen (see OpsDestinations.MORE) —
 * everything that isn't frequent enough to earn its own tab, one tap away
 * instead of crowding the bar. Stateless: no ViewModel, nothing to load,
 * just a list of destinations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenLeads: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenCompliance: () -> Unit,
    onOpenBusinessProfile: () -> Unit,
) {
    val items = listOf(
        MoreItem("Leads", Icons.Filled.Handshake, onOpenLeads),
        MoreItem("Reports", Icons.Filled.Assessment, onOpenReports),
        MoreItem("Suppliers", Icons.Filled.LocalShipping, onOpenSuppliers),
        MoreItem("Employees", Icons.Filled.Group, onOpenEmployees),
        MoreItem("Compliance", Icons.Filled.Rule, onOpenCompliance),
        MoreItem("Business profile & settings", Icons.Filled.Business, onOpenBusinessProfile),
    )

    Scaffold(topBar = { TopAppBar(title = { Text("More") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding)) {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(item.label) },
                    leadingContent = { Icon(item.icon, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable(onClick = item.onClick),
                )
            }
        }
    }
}
