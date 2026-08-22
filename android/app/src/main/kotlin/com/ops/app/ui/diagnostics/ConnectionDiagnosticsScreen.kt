package com.ops.app.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.BuildConfig
import com.ops.app.ui.components.FormSectionLabel
import com.ops.app.ui.components.SectionHeader

@Composable
fun ConnectionDiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: ConnectionDiagnosticsViewModel = hiltViewModel(),
) {
    // Defense in depth on top of BusinessProfileScreen already hiding the
    // entry-point button outside debug builds: this makes the route itself
    // functionally inert in a release build too (e.g. against a deep link
    // or a replayed back stack), not just unreachable via the normal UI.
    if (!BuildConfig.DEBUG) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectionDiagnosticsContent(
        uiState = uiState,
        onBack = onBack,
        onTestConnection = viewModel::testConnection,
        onTestAuthentication = viewModel::testAuthentication,
        onSyncNow = viewModel::syncNow,
    )
}

/** Stateless render of [ConnectionDiagnosticsScreen] — not called from
 * navigation directly. Debug-only (see OpsNavGraph — this route is only
 * reachable from Business Profile's Developer options, which is itself
 * gated on BuildConfig.DEBUG), so it never ships in a release build. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDiagnosticsContent(
    uiState: ConnectionDiagnosticsUiState,
    onBack: () -> Unit,
    onTestConnection: () -> Unit,
    onTestAuthentication: () -> Unit,
    onSyncNow: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection diagnostics") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FormSectionLabel("API endpoint")
            Text(uiState.apiEndpoint, style = MaterialTheme.typography.bodyLarge)

            FormSectionLabel("Authenticated user")
            Text(
                if (uiState.isSignedIn) uiState.signedInEmail ?: "Signed in" else "Not signed in",
                style = MaterialTheme.typography.bodyLarge,
            )

            FormSectionLabel("Business")
            Text(uiState.businessName ?: "—", style = MaterialTheme.typography.bodyLarge)

            FormSectionLabel("Sync")
            Text("Pending changes: ${uiState.pendingSyncCount}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Last successful sync: ${uiState.lastSuccessfulSyncAt ?: "never"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            uiState.lastSyncError?.let {
                Text(
                    "Last sync error: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            SectionHeader("Tests")

            DiagnosticCheckRow(label = "Test connection", result = uiState.connectionCheck, onClick = onTestConnection)
            DiagnosticCheckRow(label = "Test authentication", result = uiState.authCheck, onClick = onTestAuthentication)
            DiagnosticCheckRow(label = "Sync now", result = uiState.syncCheck, onClick = onSyncNow)
        }
    }
}

@Composable
private fun DiagnosticCheckRow(label: String, result: DiagnosticCheckResult, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onClick, enabled = result.state != DiagnosticCheckState.RUNNING) { Text(label) }
            if (result.state == DiagnosticCheckState.RUNNING) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        result.message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (result.state == DiagnosticCheckState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
