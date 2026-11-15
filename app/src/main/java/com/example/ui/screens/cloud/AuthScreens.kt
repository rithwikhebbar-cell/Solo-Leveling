package com.example.ui.screens.cloud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.model.AuthStatus
import com.example.domain.model.AuthValidator
import com.example.domain.model.PlayerIdentity
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.CloudSyncUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudFeedbackBanner(
    feedbackMessage: String?,
    errorMessage: String?,
    isBusy: Boolean = false
) {
    if (isBusy) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = SystemColors.ElectricCyan
            )
            Text(
                text = "Executing System Request...",
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.ElectricCyan
            )
        }
    }
    if (!errorMessage.isNullOrBlank()) {
        Surface(
            color = SystemColors.DangerCrimson.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, SystemColors.DangerCrimson),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.PureIce,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
    if (!feedbackMessage.isNullOrBlank()) {
        Surface(
            color = SystemColors.VerifiedEmerald.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, SystemColors.VerifiedEmerald),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = feedbackMessage,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.PureIce,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun hudFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SystemColors.ElectricCyan,
    unfocusedBorderColor = SystemColors.PanelBorderCyan,
    focusedLabelColor = SystemColors.ElectricCyan,
    unfocusedLabelColor = SystemColors.MutedSilver,
    focusedTextColor = SystemColors.PureIce,
    unfocusedTextColor = SystemColors.PureIce
)

// Screen 1: Welcome / Account Gateway
@Composable
fun AccountGatewayScreen(
    uiState: CloudSyncUiState,
    onOpenSignUp: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenSyncDashboard: () -> Unit,
    onOpenAccountSettings: () -> Unit,
    onContinueOffline: () -> Unit
) {
    val identity = uiState.identity
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Account & Cloud Gateway",
                    subtitle = "Supabase-backed player identity with full offline-first continuity"
                )
            }

            if (!uiState.summary.isCloudConfigured) {
                item {
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.WarningAmber) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.CloudOff, contentDescription = "Cloud not configured", tint = SystemColors.WarningAmber)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("OFFLINE VAULT MODE ACTIVE", style = MaterialTheme.typography.labelLarge, color = SystemColors.WarningAmber)
                                Text(
                                    "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets to enable live cloud sync and multi-device login. All local RPG, Habit, Physical, and Academic modules work 100% offline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MetallicSilver
                                )
                            }
                        }
                    }
                }
            }

            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = if (identity != null) Icons.Rounded.CloudDone else Icons.Rounded.Security,
                                contentDescription = "Player identity status",
                                tint = SystemColors.ElectricCyan
                            )
                            Text(
                                text = if (identity != null) "LINKED HUNTER IDENTITY" else "PLAYER IDENTITY VAULT",
                                style = MaterialTheme.typography.titleMedium,
                                color = SystemColors.ElectricCyan
                            )
                        }
                        if (identity != null) {
                            Text("Codename: ${identity.displayName}", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                            Text("Email: ${identity.email} • ${identity.authStatus.label}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                            Text("Identity UUID: ${identity.playerId}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onOpenSyncDashboard,
                                    modifier = Modifier.weight(1f).testTag("gateway_open_sync_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                                ) {
                                    Text("SYNC DASHBOARD")
                                }
                                OutlinedButton(
                                    onClick = onOpenAccountSettings,
                                    modifier = Modifier.weight(1f).testTag("gateway_account_settings_button"),
                                    border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                                ) {
                                    Text("ACCOUNT", color = SystemColors.PureIce)
                                }
                            }
                        } else {
                            Text(
                                text = "Link an account to preserve your Hunter profile across Android devices, protect progress with deterministic server validation, and enable encrypted-checksum cloud backups.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.MetallicSilver
                            )
                            Button(
                                onClick = onOpenSignUp,
                                modifier = Modifier.fillMaxWidth().testTag("gateway_signup_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                            ) {
                                Text("AWAKEN NEW CLOUD ACCOUNT")
                            }
                            OutlinedButton(
                                onClick = onOpenLogin,
                                modifier = Modifier.fillMaxWidth().testTag("gateway_login_button"),
                                border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                            ) {
                                Text("SIGN IN TO EXISTING ACCOUNT", color = SystemColors.PureIce)
                            }
                            TextButton(
                                onClick = onContinueOffline,
                                modifier = Modifier.fillMaxWidth().testTag("gateway_offline_button")
                            ) {
                                Text("CONTINUE IN OFFLINE VAULT MODE", color = SystemColors.MutedSilver)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Screen 2: Sign Up
@Composable
fun SignUpScreen(
    uiState: CloudSyncUiState,
    onSignUp: (email: String, password: String, displayName: String) -> Unit,
    onOpenLogin: () -> Unit
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Create System Account",
                    subtitle = "Establishes your persistent Player Identity across devices"
                )
            }
            item {
                CloudFeedbackBanner(
                    feedbackMessage = uiState.feedbackMessage,
                    errorMessage = localError ?: uiState.errorMessage,
                    isBusy = uiState.isBusy
                )
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it.take(32); localError = null },
                            label = { Text("Hunter Display Name") },
                            singleLine = true,
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("signup_name_input")
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; localError = null },
                            label = { Text("Email Address") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("signup_email_input")
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; localError = null },
                            label = { Text("Password (8+ chars, upper, lower, number)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("signup_password_input")
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; localError = null },
                            label = { Text("Confirm Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("signup_confirm_input")
                        )
                        Button(
                            onClick = {
                                val nameCheck = AuthValidator.validateDisplayName(displayName)
                                val emailCheck = AuthValidator.validateEmail(email)
                                val pwCheck = AuthValidator.validatePassword(password)
                                when {
                                    !nameCheck.isValid -> localError = nameCheck.errorMessage
                                    !emailCheck.isValid -> localError = emailCheck.errorMessage
                                    !pwCheck.isValid -> localError = pwCheck.errorMessage
                                    password != confirmPassword -> localError = "Passwords do not match."
                                    else -> onSignUp(email.trim(), password, displayName.trim())
                                }
                            },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth().testTag("signup_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                        ) {
                            Text("CREATE CLOUD IDENTITY")
                        }
                        TextButton(onClick = onOpenLogin, modifier = Modifier.fillMaxWidth()) {
                            Text("Already registered? Sign In", color = SystemColors.ArcaneViolet)
                        }
                    }
                }
            }
        }
    }
}

// Screen 3: Login
@Composable
fun LoginScreen(
    uiState: CloudSyncUiState,
    onLogin: (email: String, password: String) -> Unit,
    onOpenForgotPassword: () -> Unit,
    onOpenSignUp: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Hunter Authentication",
                    subtitle = "Sign in to synchronize progression with your linked devices"
                )
            }
            item {
                CloudFeedbackBanner(
                    feedbackMessage = uiState.feedbackMessage,
                    errorMessage = localError ?: uiState.errorMessage,
                    isBusy = uiState.isBusy
                )
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; localError = null },
                            label = { Text("Email Address") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("login_email_input")
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; localError = null },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                        )
                        Button(
                            onClick = {
                                val emailCheck = AuthValidator.validateEmail(email)
                                if (!emailCheck.isValid) {
                                    localError = emailCheck.errorMessage
                                } else if (password.isBlank()) {
                                    localError = "Enter your password."
                                } else {
                                    onLogin(email.trim(), password)
                                }
                            },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth().testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                        ) {
                            Text("AUTHENTICATE")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onOpenForgotPassword, modifier = Modifier.testTag("login_forgot_button")) {
                                Text("Forgot Password?", color = SystemColors.MutedSilver)
                            }
                            TextButton(onClick = onOpenSignUp, modifier = Modifier.testTag("login_signup_button")) {
                                Text("Create Account", color = SystemColors.ElectricCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Screen 4: Email Verification
@Composable
fun EmailVerificationScreen(
    uiState: CloudSyncUiState,
    onResendEmail: () -> Unit,
    onRefreshStatus: () -> Unit,
    onContinueOffline: () -> Unit
) {
    val identity = uiState.identity
    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Email Verification",
                    subtitle = "Confirm inbox ownership to unlock full cloud synchronization"
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, uiState.errorMessage, uiState.isBusy)
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = if (identity?.emailVerified == true) Icons.Rounded.MarkEmailRead else Icons.Rounded.Email,
                            contentDescription = "Email verification",
                            tint = if (identity?.emailVerified == true) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (identity?.emailVerified == true) "EMAIL VERIFIED" else "VERIFICATION LINK SENT",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "Account: ${identity?.email ?: "Unlinked"} • Status: ${identity?.authStatus?.label ?: AuthStatus.SIGNED_OUT.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        Button(
                            onClick = onRefreshStatus,
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth().testTag("verify_refresh_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                        ) {
                            Text("CHECK VERIFICATION STATUS")
                        }
                        OutlinedButton(
                            onClick = onResendEmail,
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth().testTag("verify_resend_button"),
                            border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                        ) {
                            Text("RESEND VERIFICATION EMAIL", color = SystemColors.PureIce)
                        }
                        TextButton(onClick = onContinueOffline, modifier = Modifier.fillMaxWidth()) {
                            Text("WORK OFFLINE IN THE MEANTIME", color = SystemColors.MutedSilver)
                        }
                    }
                }
            }
        }
    }
}

// Screen 5: Forgot Password
@Composable
fun ForgotPasswordScreen(
    uiState: CloudSyncUiState,
    onSendRecovery: (String) -> Unit,
    onOpenResetPassword: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Password Recovery",
                    subtitle = "Request a secure recovery link via Supabase Auth"
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, localError ?: uiState.errorMessage, uiState.isBusy)
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; localError = null },
                            label = { Text("Registered Email Address") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("forgot_email_input")
                        )
                        Button(
                            onClick = {
                                val check = AuthValidator.validateEmail(email)
                                if (!check.isValid) localError = check.errorMessage else onSendRecovery(email.trim())
                            },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth().testTag("forgot_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                        ) {
                            Text("SEND RECOVERY EMAIL")
                        }
                        TextButton(onClick = onOpenResetPassword, modifier = Modifier.fillMaxWidth()) {
                            Text("Already have an active recovery session? Set New Password", color = SystemColors.ArcaneViolet)
                        }
                    }
                }
            }
        }
    }
}

// Screen 6: Reset Password
@Composable
fun ResetPasswordScreen(
    uiState: CloudSyncUiState,
    onResetPassword: (String) -> Unit
) {
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Reset Password",
                    subtitle = "Updates your Supabase account credentials securely"
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, localError ?: uiState.errorMessage, uiState.isBusy)
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.LockReset, contentDescription = "Reset password", tint = SystemColors.ArcaneViolet)
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; localError = null },
                            label = { Text("New Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("reset_password_input")
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; localError = null },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("reset_confirm_input")
                        )
                        Button(
                            onClick = {
                                val check = AuthValidator.validatePassword(newPassword)
                                when {
                                    !check.isValid -> localError = check.errorMessage
                                    newPassword != confirmPassword -> localError = "Passwords do not match."
                                    else -> onResetPassword(newPassword)
                                }
                            },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth().testTag("reset_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                        ) {
                            Text("UPDATE PASSWORD")
                        }
                    }
                }
            }
        }
    }
}

// Screen 7 & Sync UX 9: Account Settings & Management
@Composable
fun AccountSettingsScreen(
    uiState: CloudSyncUiState,
    onUpdateDisplayName: (String) -> Unit,
    onToggleCloudConsent: (Boolean) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onOpenSyncDashboard: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenBackups: () -> Unit,
    onOpenResetPassword: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onOpenLogin: () -> Unit
) {
    val identity: PlayerIdentity? = uiState.identity
    var nameInput by rememberSaveable(identity?.displayName) { mutableStateOf(identity?.displayName ?: "") }
    val fmt = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Account Management",
                    subtitle = "Cloud Identity, privacy consent, multi-device sessions & deletion"
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, uiState.errorMessage, uiState.isBusy)
            }
            if (identity == null) {
                item {
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("No Cloud Account Linked", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                            Text(
                                "You are running in local Offline Vault mode. Sign in or create an account to enable multi-device synchronization and cloud backups.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                            Button(
                                onClick = onOpenLogin,
                                modifier = Modifier.fillMaxWidth().testTag("account_signin_cta"),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                            ) {
                                Text("SIGN IN OR REGISTER")
                            }
                        }
                    }
                }
            } else {
                item {
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.AccountCircle, contentDescription = "Account profile", tint = SystemColors.ElectricCyan)
                                Column {
                                    Text(identity.displayName, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                    Text("${identity.email} • ${identity.accountStatus.label}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                }
                            }
                            Text("Player UUID: ${identity.playerId}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                            Text("Auth User ID: ${identity.supabaseUserId}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                            Text(
                                "Last Sync: ${identity.lastSyncAtEpochMs?.let { fmt.format(Date(it)) } ?: "Not synced yet"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SystemColors.VerifiedEmerald
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it.take(32) },
                                label = { Text("Hunter Display Name") },
                                singleLine = true,
                                colors = hudFieldColors(),
                                modifier = Modifier.fillMaxWidth().testTag("account_display_name_input")
                            )
                            Button(
                                onClick = { onUpdateDisplayName(nameInput) },
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth().testTag("account_save_name_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                            ) {
                                Text("SAVE DISPLAY NAME")
                            }
                        }
                    }
                }

                item {
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("PRIVACY & SYNCHRONIZATION CONTROLS", style = MaterialTheme.typography.titleSmall, color = SystemColors.ArcaneViolet)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("Cloud Sync Data Consent", style = MaterialTheme.typography.bodyMedium, color = SystemColors.PureIce)
                                    Text("Permit encrypted-transit synchronization of progress to your RLS-protected Supabase vault.", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                }
                                Switch(
                                    checked = identity.cloudSyncConsentGranted,
                                    onCheckedChange = onToggleCloudConsent,
                                    colors = SwitchDefaults.colors(checkedThumbColor = SystemColors.ObsidianBlack, checkedTrackColor = SystemColors.ElectricCyan)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("Background Auto-Sync (WorkManager)", style = MaterialTheme.typography.bodyMedium, color = SystemColors.PureIce)
                                    Text("Synchronize automatically on login and network restoration.", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                }
                                Switch(
                                    checked = identity.autoSyncEnabled,
                                    onCheckedChange = onToggleAutoSync,
                                    colors = SwitchDefaults.colors(checkedThumbColor = SystemColors.ObsidianBlack, checkedTrackColor = SystemColors.ElectricCyan)
                                )
                            }
                        }
                    }
                }

                item {
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.SystemBlue) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = onOpenSyncDashboard, modifier = Modifier.fillMaxWidth().testTag("account_sync_dashboard_btn")) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, tint = SystemColors.ElectricCyan)
                                Spacer(Modifier.width(8.dp))
                                Text("CLOUD SYNC DASHBOARD", color = SystemColors.PureIce)
                            }
                            OutlinedButton(onClick = onOpenDevices, modifier = Modifier.fillMaxWidth().testTag("account_devices_btn")) {
                                Icon(Icons.Rounded.Devices, contentDescription = null, tint = SystemColors.ArcaneViolet)
                                Spacer(Modifier.width(8.dp))
                                Text("REGISTERED DEVICES (${uiState.devices.size})", color = SystemColors.PureIce)
                            }
                            OutlinedButton(onClick = onOpenBackups, modifier = Modifier.fillMaxWidth().testTag("account_backups_btn")) {
                                Icon(Icons.Rounded.Backup, contentDescription = null, tint = SystemColors.VerifiedEmerald)
                                Spacer(Modifier.width(8.dp))
                                Text("CLOUD BACKUPS & RESTORE", color = SystemColors.PureIce)
                            }
                            OutlinedButton(onClick = onOpenResetPassword, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.LockReset, contentDescription = null, tint = SystemColors.MetallicSilver)
                                Spacer(Modifier.width(8.dp))
                                Text("CHANGE PASSWORD", color = SystemColors.PureIce)
                            }
                            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().testTag("account_signout_btn")) {
                                Text("SIGN OUT (STAY IN OFFLINE MODE)", color = SystemColors.WarningAmber)
                            }
                            Button(
                                onClick = onOpenDeleteAccount,
                                modifier = Modifier.fillMaxWidth().testTag("account_delete_nav_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.DangerCrimson, contentColor = SystemColors.PureIce)
                            ) {
                                Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("DELETE CLOUD ACCOUNT")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Controlled Account Deletion Screen (Section 12)
@Composable
fun AccountDeletionScreen(
    uiState: CloudSyncUiState,
    onConfirmDelete: (confirmationPhrase: String, password: String) -> Unit
) {
    var phrase by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Permanent Account Deletion",
                    subtitle = "Server-side deletion via authenticated Edge Function"
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, localError ?: uiState.errorMessage, uiState.isBusy)
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.DangerCrimson) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("WHAT WILL BE PERMANENTLY ERASED", style = MaterialTheme.typography.titleMedium, color = SystemColors.DangerCrimson)
                        Text(
                            "• Your Supabase authentication user & active tokens across all devices.\n" +
                                "• Cloud player identity, device registry, backups, and progression history.\n" +
                                "• Local sync queue, sync cursors, and linked account metadata.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; localError = null },
                            label = { Text("Re-enter Current Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("delete_password_input")
                        )
                        OutlinedTextField(
                            value = phrase,
                            onValueChange = { phrase = it; localError = null },
                            label = { Text("Type DELETE MY ACCOUNT to confirm") },
                            singleLine = true,
                            colors = hudFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("delete_phrase_input")
                        )
                        Button(
                            onClick = {
                                if (phrase.trim() != "DELETE MY ACCOUNT") {
                                    localError = "Type DELETE MY ACCOUNT exactly to proceed."
                                } else {
                                    onConfirmDelete(phrase.trim(), password)
                                }
                            },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth().testTag("delete_confirm_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.DangerCrimson, contentColor = SystemColors.PureIce)
                        ) {
                            Text("PERMANENTLY DELETE ACCOUNT")
                        }
                    }
                }
            }
        }
    }
}
