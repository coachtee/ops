package com.ops.app.ui.schedule

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.ui.components.ErrorBanner
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.StatusBadge
import com.ops.app.ui.components.SyncStateBadge
import com.ops.app.ui.components.VISIT_STATUS_CHOICES
import com.ops.app.ui.components.addressLines
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.labelFor
import com.ops.app.ui.components.visitStatusTone
import com.ops.coredomain.VisitStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VisitDetailScreen(
    onBack: () -> Unit,
    onCreateInvoice: (customerId: String, jobId: String, quoteId: String?) -> Unit,
    viewModel: VisitDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            scope.launch {
                copyToPermanentVisitPhotoFile(context, uri, viewModel.visitId)?.let { viewModel.attachPhoto(it) }
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                copyToPermanentVisitPhotoFile(context, uri, viewModel.visitId)?.let { viewModel.attachPhoto(it) }
            }
        }
    }

    VisitDetailContent(
        uiState = uiState,
        onBack = onBack,
        onStart = viewModel::start,
        onUpdateStatus = viewModel::updateStatus,
        onUpdateNotes = viewModel::updateNotes,
        onComplete = viewModel::complete,
        onRetryPhoto = viewModel::retryPhoto,
        onCreateInvoice = { customerId, jobId -> onCreateInvoice(customerId, jobId, uiState.job?.quoteId) },
        onTakePhoto = {
            scope.launch {
                val uri = withContext(Dispatchers.IO) { createTempVisitCameraUri(context) }
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        },
        onChoosePhoto = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )
}

/** Stateless render of [VisitDetailScreen] — split out for the screenshot
 * pack (see android/README.md); not called from navigation directly.
 *
 * The field workflow the whole Scheduling feature exists for: contextual
 * to [VisitDetailUiState.visit]'s status so a technician only ever sees
 * the one action that makes sense right now — Start, then Add note/Take
 * photo/Complete, then (once the job's fully done) Create invoice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailContent(
    uiState: VisitDetailUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onComplete: () -> Unit,
    onRetryPhoto: () -> Unit,
    onCreateInvoice: (customerId: String, jobId: String) -> Unit,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val visit = uiState.visit
    var noteDraft by remember(visit?.id) { mutableStateOf(visit?.notes.orEmpty()) }

    fun launchIntent(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar("No app found to handle that.") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visit") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (visit == null) return@Scaffold
        val customer = uiState.customer
        val job = uiState.job

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(customer?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                    Text(job?.title.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(labelFor(VISIT_STATUS_CHOICES, visit.status), visitStatusTone(visit.status))
            }
            SyncStateBadge(visit.syncState)

            customer?.let { c ->
                addressLines(c.addressLine1, c.addressLine2, c.suburb, c.city, c.postalCode).forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "Scheduled ${formatDate(visit.scheduledDate)}" + (visit.startTime?.let { " at $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer?.phone.orEmpty()}"))) },
                    enabled = !customer?.phone.isNullOrBlank(),
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Call, contentDescription = null); Text(" Call", modifier = Modifier.padding(start = 4.dp)) }
                OutlinedButton(
                    onClick = { launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("smsto:${customer?.phone.orEmpty()}"))) },
                    enabled = !customer?.phone.isNullOrBlank(),
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.Chat, contentDescription = null); Text(" WhatsApp", modifier = Modifier.padding(start = 4.dp)) }
            }

            when (visit.status) {
                VisitStatus.SCHEDULED.wire, VisitStatus.EN_ROUTE.wire -> {
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Start visit") }
                }
                VisitStatus.IN_PROGRESS.wire -> {
                    SectionHeader("Notes")
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onUpdateNotes(noteDraft) },
                        enabled = noteDraft != visit.notes,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save note") }

                    SectionHeader("Photo")
                    VisitPhotoPreviewAndActions(visit.localPhotoPath, visit.photoUrl, visit.photoSyncState, visit.photoSyncError, onTakePhoto, onChoosePhoto, onRetryPhoto)

                    Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Complete visit") }
                }
                VisitStatus.COMPLETED.wire -> {
                    if (visit.notes.isNotBlank()) {
                        SectionHeader("Notes")
                        Text(visit.notes)
                    }
                    if (visit.photoUrl != null || visit.localPhotoPath != null) {
                        SectionHeader("Photo")
                        VisitPhotoPreviewAndActions(visit.localPhotoPath, visit.photoUrl, visit.photoSyncState, visit.photoSyncError, onTakePhoto, onChoosePhoto, onRetryPhoto)
                    }
                    if (job != null && customer != null) {
                        Button(
                            onClick = { onCreateInvoice(customer.id, job.id) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) { Text("Create invoice") }
                    }
                }
                else -> {}
            }

            SectionHeader("Status")
            LabeledDropdown(
                label = "Visit status",
                options = VISIT_STATUS_CHOICES,
                selected = visit.status,
                onSelected = onUpdateStatus,
            )
        }
    }
}

@Composable
private fun VisitPhotoPreviewAndActions(
    localPath: String?,
    photoUrl: String?,
    photoSyncState: String,
    photoSyncError: String?,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    onRetryPhoto: () -> Unit,
) {
    val previewModel: Any? = localPath?.let { File(it) } ?: photoUrl
    if (previewModel != null) {
        AsyncImage(model = previewModel, contentDescription = "Visit photo", modifier = Modifier.size(120.dp).padding(bottom = 4.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onTakePhoto) { Text("Take photo") }
        OutlinedButton(onClick = onChoosePhoto) { Text("Choose photo") }
    }
    when (photoSyncState) {
        ReceiptSyncState.PENDING -> Text("Saved on this phone — uploads when back online.", style = MaterialTheme.typography.bodySmall)
        ReceiptSyncState.UPLOADING -> Text("Uploading…", style = MaterialTheme.typography.bodySmall)
        ReceiptSyncState.FAILED -> ErrorBanner(message = photoSyncError ?: "Couldn't upload the photo.", onRetry = onRetryPhoto)
        else -> {}
    }
}

private fun createTempVisitCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "visit_photos_tmp").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Copies a captured/picked photo into permanent app-private storage —
 * same pattern as ExpenseEditScreen's copyToPermanentReceiptFile. */
private suspend fun copyToPermanentVisitPhotoFile(context: Context, source: Uri, visitId: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "visit_photos").apply { mkdirs() }
            val dest = File(dir, "$visitId.jpg")
            val copied = context.contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) dest.absolutePath else null
        }.getOrNull()
    }
