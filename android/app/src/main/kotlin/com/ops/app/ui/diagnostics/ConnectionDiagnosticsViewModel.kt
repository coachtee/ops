package com.ops.app.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.BuildConfig
import com.ops.app.data.datastore.AuthPreferences
import com.ops.app.data.datastore.DevServerPreferences
import com.ops.app.data.remote.ConnectionDiagnosis
import com.ops.app.data.remote.OpsApiService
import com.ops.app.data.repository.BusinessRepository
import com.ops.app.data.sync.SyncChipState
import com.ops.app.data.sync.SyncManager
import com.ops.app.data.sync.SyncOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DiagnosticCheckState { IDLE, RUNNING, OK, FAILED }

data class DiagnosticCheckResult(
    val state: DiagnosticCheckState = DiagnosticCheckState.IDLE,
    val message: String? = null,
)

data class ConnectionDiagnosticsUiState(
    val apiEndpoint: String = "",
    val isSignedIn: Boolean = false,
    val signedInEmail: String? = null,
    val businessName: String? = null,
    val pendingSyncCount: Int = 0,
    val lastSuccessfulSyncAt: String? = null,
    val lastSyncError: String? = null,
    val connectionCheck: DiagnosticCheckResult = DiagnosticCheckResult(),
    val authCheck: DiagnosticCheckResult = DiagnosticCheckResult(),
    val syncCheck: DiagnosticCheckResult = DiagnosticCheckResult(),
)

/**
 * Debug-only — see ui/settings/BusinessProfileScreen's "Developer options"
 * for the analogous existing gate — this screen exists so a real-device
 * connectivity problem can be diagnosed on the phone itself, without
 * guessing from a raw "failed to connect" message or reasoning about it
 * remotely. Read-only except for the three test actions; it never changes
 * app state other than what [SyncManager.syncNow] itself already does.
 */
@HiltViewModel
class ConnectionDiagnosticsViewModel @Inject constructor(
    private val apiService: OpsApiService,
    private val authPreferences: AuthPreferences,
    devServerPreferences: DevServerPreferences,
    businessRepository: BusinessRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val connectionCheck = MutableStateFlow(DiagnosticCheckResult())
    private val authCheck = MutableStateFlow(DiagnosticCheckResult())
    private val syncCheck = MutableStateFlow(DiagnosticCheckResult())

    // combine() only has typed overloads up to 5 flows — nested here for the
    // same reason as HomeViewModel's businessLeadsJobs/invoicesPaymentsExpenses.
    private val identity = combine(
        devServerPreferences.serverUrlOverride,
        authPreferences.isSignedIn,
        authPreferences.signedInEmail,
    ) { override, signedIn, email -> Triple(override, signedIn, email) }

    private val syncState = combine(
        businessRepository.observe(),
        syncManager.observeChipState(),
    ) { business, chipState -> Pair(business?.name, chipState) }

    private val lastSync = combine(
        authPreferences.lastSyncSuccessAt,
        authPreferences.lastSyncError,
    ) { success, error -> Pair(success, error) }

    private val checks = combine(connectionCheck, authCheck, syncCheck) { c, a, s -> Triple(c, a, s) }

    val uiState: StateFlow<ConnectionDiagnosticsUiState> = combine(
        identity,
        syncState,
        lastSync,
        checks,
    ) { (override, signedIn, email), (businessName, chipState), (lastSuccess, lastError), (connCheck, aCheck, sCheck) ->
        ConnectionDiagnosticsUiState(
            apiEndpoint = override ?: BuildConfig.BASE_URL,
            isSignedIn = signedIn,
            signedInEmail = email,
            businessName = businessName,
            pendingSyncCount = when (chipState) {
                is SyncChipState.Pending -> chipState.count
                is SyncChipState.Failed -> chipState.count
                else -> 0
            },
            lastSuccessfulSyncAt = lastSuccess,
            lastSyncError = lastError,
            connectionCheck = connCheck,
            authCheck = aCheck,
            syncCheck = sCheck,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionDiagnosticsUiState())

    fun testConnection() {
        viewModelScope.launch {
            connectionCheck.value = DiagnosticCheckResult(DiagnosticCheckState.RUNNING)
            connectionCheck.value = try {
                val response = apiService.health()
                DiagnosticCheckResult(DiagnosticCheckState.OK, "Reachable — ${response.service}, database: ${response.database}")
            } catch (e: Exception) {
                DiagnosticCheckResult(DiagnosticCheckState.FAILED, ConnectionDiagnosis.from(e).userMessage)
            }
        }
    }

    /** Exercises the current access token against a real authenticated
     * endpoint (not just checking whether a token string is stored) — a
     * stored-but-expired-or-rejected token looks identical to a valid one
     * until it's actually used. */
    fun testAuthentication() {
        viewModelScope.launch {
            authCheck.value = DiagnosticCheckResult(DiagnosticCheckState.RUNNING)
            authCheck.value = try {
                val business = apiService.getBusiness()
                DiagnosticCheckResult(DiagnosticCheckState.OK, "Authenticated as ${business.name}")
            } catch (e: Exception) {
                DiagnosticCheckResult(DiagnosticCheckState.FAILED, ConnectionDiagnosis.from(e, isAuthEndpoint = true).userMessage)
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncCheck.value = DiagnosticCheckResult(DiagnosticCheckState.RUNNING)
            syncCheck.value = when (val outcome = syncManager.syncNow()) {
                is SyncOutcome.Success -> DiagnosticCheckResult(DiagnosticCheckState.OK, "Sync completed")
                is SyncOutcome.Failed -> DiagnosticCheckResult(DiagnosticCheckState.FAILED, outcome.message)
                is SyncOutcome.NotSignedIn -> DiagnosticCheckResult(DiagnosticCheckState.FAILED, "Not signed in")
            }
        }
    }
}
