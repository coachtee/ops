package com.ops.app.ui.expenses

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.ui.components.DateField
import com.ops.app.ui.components.EXPENSE_CATEGORY_CHOICES
import com.ops.app.ui.components.ErrorBanner
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.formatZar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val NO_JOB = ""
private const val NO_SUPPLIER = ""

@Composable
fun ExpenseEditScreen(
    onBack: () -> Unit,
    viewModel: ExpenseEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        val expenseId = uiState.expenseId
        if (success && uri != null && expenseId != null) {
            scope.launch {
                copyToPermanentReceiptFile(context, uri, expenseId)?.let { viewModel.attachReceipt(it) }
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val expenseId = uiState.expenseId
        if (uri != null && expenseId != null) {
            scope.launch {
                copyToPermanentReceiptFile(context, uri, expenseId)?.let { viewModel.attachReceipt(it) }
            }
        }
    }

    ExpenseEditContent(
        uiState = uiState,
        jobs = jobs,
        suppliers = suppliers,
        onBack = onBack,
        onUpdate = viewModel::update,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onRetryReceiptUpload = viewModel::retryReceiptUpload,
        onTakePhoto = {
            scope.launch {
                val uri = withContext(Dispatchers.IO) { createTempCameraUri(context) }
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        },
        onChoosePhoto = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )
}

/** Stateless render of [ExpenseEditScreen] — split out for the screenshot
 * pack (see android/README.md); not called from navigation directly. The
 * receipt-capture buttons are passed in as plain callbacks since the
 * actual camera/gallery activity-result launchers need to stay registered
 * in [ExpenseEditScreen] itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditContent(
    uiState: ExpenseEditUiState,
    jobs: List<JobEntity>,
    suppliers: List<SupplierEntity>,
    onBack: () -> Unit,
    onUpdate: ((ExpenseEditUiState) -> ExpenseEditUiState) -> Unit,
    onSave: (() -> Unit) -> Unit,
    onDelete: (() -> Unit) -> Unit,
    onRetryReceiptUpload: () -> Unit,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.expenseId == null) "New expense" else "Expense") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (uiState.expenseId != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete expense")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) return@Scaffold

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.syncState?.let { SyncStateBadge(it) }
            if (uiState.syncState != null) {
                Text(
                    "This expense is money out — it doesn't need a customer, just what was spent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { onUpdate { s -> s.copy(amount = it, amountError = null) } },
                label = { Text("Amount paid (R)") },
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Includes VAT?", modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.isVatApplicable,
                    onCheckedChange = { onUpdate { s -> s.copy(isVatApplicable = it) } },
                )
            }
            if (uiState.isVatApplicable) {
                Text(
                    "VAT: ${formatZar(uiState.vatAmount)} of this total — for your SARS records, not added on top.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LabeledDropdown(
                label = "Category",
                options = EXPENSE_CATEGORY_CHOICES,
                selected = uiState.category,
                onSelected = { onUpdate { s -> s.copy(category = it) } },
            )

            DateField(
                label = "Date",
                value = uiState.date,
                onValueChange = { picked -> picked?.let { onUpdate { s -> s.copy(date = it, dateError = null) } } },
                clearable = false,
            )
            uiState.dateError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { onUpdate { s -> s.copy(description = it) } },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledDropdown(
                label = "Job / project (optional)",
                options = listOf(NO_JOB to "None") + jobs.map { it.id to (it.number ?: it.title) },
                selected = uiState.jobId ?: NO_JOB,
                onSelected = { onUpdate { s -> s.copy(jobId = it.ifBlank { null }) } },
            )

            LabeledDropdown(
                label = "Supplier (optional)",
                options = listOf(NO_SUPPLIER to "None") + suppliers.map { it.id to it.name },
                selected = uiState.supplierId ?: NO_SUPPLIER,
                onSelected = { onUpdate { s -> s.copy(supplierId = it.ifBlank { null }) } },
            )

            SectionHeader("Receipt")
            when {
                uiState.expenseId == null -> Text(
                    "Save this expense first, then add a photo of the receipt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    val previewModel: Any? = uiState.localReceiptPath?.let { File(it) } ?: uiState.receiptUrl
                    if (previewModel != null) {
                        AsyncImage(
                            model = previewModel,
                            contentDescription = "Receipt photo",
                            modifier = Modifier.size(120.dp).padding(bottom = 4.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onTakePhoto) { Text("Take photo") }
                        OutlinedButton(onClick = onChoosePhoto) { Text("Choose photo") }
                    }
                    when (uiState.receiptSyncState) {
                        ReceiptSyncState.PENDING -> Text("Saved on this phone — uploads when back online.", style = MaterialTheme.typography.bodySmall)
                        ReceiptSyncState.UPLOADING -> Text("Uploading…", style = MaterialTheme.typography.bodySmall)
                        ReceiptSyncState.FAILED -> ErrorBanner(
                            message = uiState.receiptSyncError ?: "Couldn't upload the photo.",
                            onRetry = onRetryReceiptUpload,
                        )
                        else -> {}
                    }
                }
            }

            Button(
                onClick = {
                    onSave {
                        scope.launch { snackbarHostState.showSnackbar("Saved") }
                    }
                },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(if (uiState.isSaving) "Saving…" else "Save") }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this expense?") },
            text = { Text("This removes it from your records. Anything already synced is removed there too.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; onDelete(onBack) }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

private fun createTempCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "receipts_tmp").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Copies a captured/picked photo into permanent app-private storage
 * ([Context.getFilesDir]/receipts/) — see ExpenseEntity's doc comment on
 * why a receipt can't just be held as bytes in memory. Named by expense id
 * so re-attaching a photo cleanly replaces the previous one. */
private suspend fun copyToPermanentReceiptFile(context: Context, source: Uri, expenseId: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "receipts").apply { mkdirs() }
            val dest = File(dir, "$expenseId.jpg")
            val copied = context.contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) dest.absolutePath else null
        }.getOrNull()
    }
