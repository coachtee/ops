package com.ops.app.ui.businesssetup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.ErrorBanner
import com.ops.app.ui.components.LabeledDropdown
import com.ops.app.ui.components.PROVINCE_CHOICES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BusinessSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: BusinessSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            } ?: return@launch
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            viewModel.updateForm { it.copy(logoBytes = bytes, logoMimeType = mimeType) }
        }
    }

    BusinessSetupContent(
        uiState = uiState,
        onSetMode = viewModel::setMode,
        onUpdateForm = viewModel::updateForm,
        onNextStep = viewModel::nextStep,
        onPreviousStep = viewModel::previousStep,
        onPickLogo = { logoPicker.launch("image/*") },
        onSubmitCreate = { viewModel.submitCreate(onSetupComplete) },
        onSubmitSignIn = { viewModel.submitSignIn(onSetupComplete) },
        onUpdateSignInEmail = viewModel::updateSignInEmail,
        onUpdateSignInPassword = viewModel::updateSignInPassword,
    )
}

/** Stateless render of [BusinessSetupScreen] — split out for the
 * screenshot pack (see android/README.md); not called from navigation
 * directly. */
@Composable
fun BusinessSetupContent(
    uiState: BusinessSetupUiState,
    onSetMode: (BusinessSetupMode) -> Unit,
    onUpdateForm: ((BusinessSetupForm) -> BusinessSetupForm) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onPickLogo: () -> Unit,
    onSubmitCreate: () -> Unit,
    onSubmitSignIn: () -> Unit,
    onUpdateSignInEmail: (String) -> Unit,
    onUpdateSignInPassword: (String) -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("O", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
            Text("Let's set up your business", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Run your business from your phone — quotes, jobs and invoices, even offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            val selectedTab = if (uiState.mode == BusinessSetupMode.CREATE) 0 else 1
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { onSetMode(BusinessSetupMode.CREATE) }, text = { Text("Create business") })
                Tab(selected = selectedTab == 1, onClick = { onSetMode(BusinessSetupMode.SIGN_IN) }, text = { Text("Sign in") })
            }

            Column(Modifier.padding(top = 16.dp)) {
                if (uiState.errorMessage != null) {
                    ErrorBanner(
                        message = uiState.errorMessage!!,
                        onRetry = if (uiState.mode == BusinessSetupMode.CREATE) {
                            { onSubmitCreate() }
                        } else {
                            { onSubmitSignIn() }
                        },
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                if (uiState.mode == BusinessSetupMode.CREATE) {
                    CreateBusinessWizard(
                        uiState = uiState,
                        onFormChange = onUpdateForm,
                        onNext = onNextStep,
                        onBack = onPreviousStep,
                        onPickLogo = onPickLogo,
                        onFinish = onSubmitCreate,
                    )
                } else {
                    SignInForm(
                        email = uiState.signInEmail,
                        password = uiState.signInPassword,
                        isLoading = uiState.isLoading,
                        onEmailChange = onUpdateSignInEmail,
                        onPasswordChange = onUpdateSignInPassword,
                        onSubmit = onSubmitSignIn,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateBusinessWizard(
    uiState: BusinessSetupUiState,
    onFormChange: ((BusinessSetupForm) -> BusinessSetupForm) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onPickLogo: () -> Unit,
    onFinish: () -> Unit,
) {
    val form = uiState.form
    Text(
        text = when (uiState.step) {
            0 -> "Step 1 of 3 — Your details"
            1 -> "Step 2 of 3 — Address"
            else -> "Step 3 of 3 — Logo"
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    when (uiState.step) {
        0 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(form.firstName, { onFormChange { f -> f.copy(firstName = it) } }, label = { Text("First name") }, modifier = Modifier.weight(1f))
                OutlinedTextField(form.lastName, { onFormChange { f -> f.copy(lastName = it) } }, label = { Text("Last name") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(
                form.email, { onFormChange { f -> f.copy(email = it) } },
                label = { Text("Your email") }, placeholder = { Text("thabo@thabosplumbing.co.za") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                form.password, { onFormChange { f -> f.copy(password = it) } },
                label = { Text("Password (min 8 characters)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                form.businessName, { onFormChange { f -> f.copy(businessName = it) } },
                label = { Text("Business name") }, placeholder = { Text("Thabo's Plumbing & Maintenance") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(form.tradingName, { onFormChange { f -> f.copy(tradingName = it) } }, label = { Text("Trading name (optional)") }, modifier = Modifier.fillMaxWidth())
            LabeledDropdown("Industry", INDUSTRY_CHOICES, form.industry, { onFormChange { f -> f.copy(industry = it) } }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                form.businessPhone, { onFormChange { f -> f.copy(businessPhone = it) } },
                label = { Text("Business phone") }, placeholder = { Text("+27 82 123 4567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                form.businessEmail, { onFormChange { f -> f.copy(businessEmail = it) } },
                label = { Text("Business email (optional)") }, placeholder = { Text("info@thabosplumbing.co.za") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Registered for VAT?", modifier = Modifier.weight(1f))
                Switch(checked = form.isVatRegistered, onCheckedChange = { onFormChange { f -> f.copy(isVatRegistered = it) } })
            }
            if (form.isVatRegistered) {
                OutlinedTextField(form.vatNumber, { onFormChange { f -> f.copy(vatNumber = it) } }, label = { Text("VAT number") }, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(form.registrationNumber, { onFormChange { f -> f.copy(registrationNumber = it) } }, label = { Text("CIPC registration number (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.taxNumber, { onFormChange { f -> f.copy(taxNumber = it) } }, label = { Text("SARS income tax number (optional)") }, modifier = Modifier.fillMaxWidth())
        }

        1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
            OutlinedTextField(form.addressLine1, { onFormChange { f -> f.copy(addressLine1 = it) } }, label = { Text("Address line 1") }, placeholder = { Text("12 Vygie Street") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.addressLine2, { onFormChange { f -> f.copy(addressLine2 = it) } }, label = { Text("Address line 2 (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.suburb, { onFormChange { f -> f.copy(suburb = it) } }, label = { Text("Suburb") }, placeholder = { Text("Delft") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.city, { onFormChange { f -> f.copy(city = it) } }, label = { Text("City / Town") }, placeholder = { Text("Cape Town") }, modifier = Modifier.fillMaxWidth())
            LabeledDropdown("Province", PROVINCE_CHOICES, form.province, { onFormChange { f -> f.copy(province = it) } }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                form.postalCode, { onFormChange { f -> f.copy(postalCode = it) } },
                label = { Text("Postal code") }, placeholder = { Text("7100") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        else -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
            Text(
                "Add your logo — it'll appear on your quotes and invoices. You can skip this and add it later from Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onPickLogo, modifier = Modifier.padding(top = 16.dp)) {
                Text(if (form.logoBytes == null) "Choose logo image" else "Logo selected — change")
            }
        }
    }

    Row(Modifier.padding(top = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (uiState.step > 0) {
            OutlinedButton(onClick = onBack, enabled = !uiState.isLoading) { Text("Back") }
        } else {
            Column {}
        }
        if (uiState.step < 2) {
            Button(onClick = onNext, enabled = uiState.canGoNext) { Text("Next") }
        } else {
            Button(onClick = onFinish, enabled = !uiState.isLoading) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create business")
                }
            }
        }
    }
}

@Composable
private fun SignInForm(
    email: String,
    password: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
        OutlinedTextField(
            email, onEmailChange, label = { Text("Email") },
            placeholder = { Text("thabo@thabosplumbing.co.za") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            password, onPasswordChange, label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onSubmit,
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Sign in")
            }
        }
    }
}
