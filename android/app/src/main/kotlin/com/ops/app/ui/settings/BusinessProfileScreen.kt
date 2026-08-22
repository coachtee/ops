package com.ops.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ops.app.BuildConfig
import com.ops.app.data.remote.dto.BusinessPatchDto
import com.ops.app.ui.businesssetup.INDUSTRY_CHOICES
import com.ops.app.ui.components.ErrorBanner
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.PROVINCE_CHOICES
import com.ops.app.ui.components.SectionHeader
import kotlinx.coroutines.launch

private data class EditableBusiness(
    val name: String = "",
    val tradingName: String = "",
    val registrationNumber: String = "",
    val taxNumber: String = "",
    val vatNumber: String = "",
    val isVatRegistered: Boolean = false,
    val industry: String = "other",
    val phone: String = "",
    val email: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val suburb: String = "",
    val city: String = "",
    val province: String = "",
    val postalCode: String = "",
)

@Composable
fun BusinessProfileScreen(
    onBack: () -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenCompliance: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: BusinessProfileViewModel = hiltViewModel(),
) {
    val business by viewModel.business.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val serverUrlOverride by viewModel.serverUrlOverride.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingLogoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingLogoMime by remember { mutableStateOf<String?>(null) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            } ?: return@launch
            pendingLogoBytes = bytes
            pendingLogoMime = context.contentResolver.getType(uri) ?: "image/jpeg"
        }
    }

    BusinessProfileContent(
        business = business,
        isSaving = isSaving,
        errorMessage = errorMessage,
        pendingLogoBytes = pendingLogoBytes,
        onBack = onBack,
        onOpenEmployees = onOpenEmployees,
        onOpenCompliance = onOpenCompliance,
        onOpenDiagnostics = onOpenDiagnostics,
        onPickLogo = { logoPicker.launch("image/*") },
        onSave = { fields -> viewModel.save(fields, pendingLogoBytes, pendingLogoMime) { pendingLogoBytes = null } },
        onLogout = { viewModel.logout(onLoggedOut) },
        showDeveloperOptions = BuildConfig.DEBUG,
        serverUrlOverride = serverUrlOverride,
        onSetServerUrlOverride = viewModel::setServerUrlOverride,
    )
}

/** Stateless render of [BusinessProfileScreen] — split out for the
 * screenshot pack (see android/README.md); not called from navigation
 * directly. The logo picker stays a plain callback since the actual
 * activity-result launcher needs to stay registered in
 * [BusinessProfileScreen] itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileContent(
    business: com.ops.app.data.local.entities.BusinessEntity?,
    isSaving: Boolean,
    errorMessage: String?,
    pendingLogoBytes: ByteArray?,
    onBack: () -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenCompliance: () -> Unit,
    onOpenDiagnostics: () -> Unit = {},
    onPickLogo: () -> Unit,
    onSave: (BusinessPatchDto) -> Unit,
    onLogout: () -> Unit,
    showDeveloperOptions: Boolean = false,
    serverUrlOverride: String? = null,
    onSetServerUrlOverride: (String?) -> Unit = {},
) {
    var form by remember(business?.id) {
        mutableStateOf(
            business?.let {
                EditableBusiness(
                    it.name, it.tradingName, it.registrationNumber, it.taxNumber, it.vatNumber,
                    it.isVatRegistered, it.industry, it.phone, it.email, it.addressLine1, it.addressLine2,
                    it.suburb, it.city, it.province, it.postalCode,
                )
            } ?: EditableBusiness(),
        )
    }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var serverUrlDraft by remember(serverUrlOverride) { mutableStateOf(serverUrlOverride.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pendingBytes = pendingLogoBytes
                when {
                    // A just-picked logo isn't uploaded yet (Save does that),
                    // so it's previewed by decoding straight from bytes
                    // rather than routing through Coil (which needs a
                    // URL/Uri/File model, not a raw ByteArray).
                    pendingBytes != null -> {
                        val bitmap = remember(pendingBytes) {
                            android.graphics.BitmapFactory.decodeByteArray(pendingBytes, 0, pendingBytes.size)?.asImageBitmap()
                        }
                        if (bitmap != null) {
                            Image(bitmap = bitmap, contentDescription = "New logo", modifier = Modifier.size(56.dp))
                        } else {
                            LogoPlaceholder(form.name)
                        }
                    }
                    business?.logoUrl != null -> AsyncImage(model = business.logoUrl, contentDescription = "Logo", modifier = Modifier.size(56.dp))
                    else -> LogoPlaceholder(form.name)
                }
                TextButton(onClick = onPickLogo, modifier = Modifier.padding(start = 12.dp)) {
                    Text("Change logo")
                }
            }

            if (errorMessage != null) {
                ErrorBanner(message = errorMessage!!, onRetry = null)
            }

            OutlinedTextField(form.name, { form = form.copy(name = it) }, label = { Text("Business name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.tradingName, { form = form.copy(tradingName = it) }, label = { Text("Trading name") }, modifier = Modifier.fillMaxWidth())
            LabeledDropdown("Industry", INDUSTRY_CHOICES, form.industry, { form = form.copy(industry = it) })
            OutlinedTextField(
                form.phone, { form = form.copy(phone = it) }, label = { Text("Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                form.email, { form = form.copy(email = it) }, label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(form.addressLine1, { form = form.copy(addressLine1 = it) }, label = { Text("Address line 1") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.addressLine2, { form = form.copy(addressLine2 = it) }, label = { Text("Address line 2") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(form.suburb, { form = form.copy(suburb = it) }, label = { Text("Suburb") }, modifier = Modifier.weight(1f))
                OutlinedTextField(form.city, { form = form.copy(city = it) }, label = { Text("City") }, modifier = Modifier.weight(1f))
            }
            LabeledDropdown("Province", PROVINCE_CHOICES, form.province, { form = form.copy(province = it) })
            OutlinedTextField(
                form.postalCode, { form = form.copy(postalCode = it) }, label = { Text("Postal code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Registered for VAT?", modifier = Modifier.weight(1f))
                Switch(checked = form.isVatRegistered, onCheckedChange = { form = form.copy(isVatRegistered = it) })
            }
            if (form.isVatRegistered) {
                OutlinedTextField(form.vatNumber, { form = form.copy(vatNumber = it) }, label = { Text("VAT number") }, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(form.registrationNumber, { form = form.copy(registrationNumber = it) }, label = { Text("CIPC registration number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.taxNumber, { form = form.copy(taxNumber = it) }, label = { Text("SARS income tax number") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    onSave(
                        BusinessPatchDto(
                            name = form.name, tradingName = form.tradingName, registrationNumber = form.registrationNumber,
                            taxNumber = form.taxNumber, vatNumber = form.vatNumber, isVatRegistered = form.isVatRegistered,
                            industry = form.industry, phone = form.phone, email = form.email, addressLine1 = form.addressLine1,
                            addressLine2 = form.addressLine2, suburb = form.suburb, city = form.city, province = form.province,
                            postalCode = form.postalCode,
                        ),
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(if (isSaving) "Saving…" else "Save changes") }

            OutlinedButton(
                onClick = onOpenEmployees,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text("Manage employees") }

            OutlinedButton(
                onClick = onOpenCompliance,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Compliance reminders") }

            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = if (showDeveloperOptions) 8.dp else 24.dp),
            ) { Text("Log out") }

            if (showDeveloperOptions) {
                SectionHeader("Developer options")
                Text(
                    "Debug builds only — points this install at a different Django server without a rebuild. " +
                        "Leave blank to use the build's default (the Android emulator's host alias).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = serverUrlDraft,
                    onValueChange = { serverUrlDraft = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.20:8000/") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(
                        onClick = { serverUrlDraft = ""; onSetServerUrlOverride(null) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Reset to default") }
                    Button(
                        onClick = { onSetServerUrlOverride(serverUrlDraft) },
                        enabled = serverUrlDraft.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                }
                OutlinedButton(
                    onClick = onOpenDiagnostics,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                ) { Text("Connection diagnostics") }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out?") },
            text = { Text("This clears everything saved on this phone. Anything already synced stays safe on the server.") },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; onLogout() }) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LogoPlaceholder(businessName: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(businessName.firstOrNull()?.toString() ?: "O", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}
