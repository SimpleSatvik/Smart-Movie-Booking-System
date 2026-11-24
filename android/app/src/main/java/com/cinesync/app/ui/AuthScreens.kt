@file:OptIn(ExperimentalMaterial3Api::class)

package com.cinesync.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinesync.app.data.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

// =========================================================================
//  Authentication Flow (Sign In, Register, Forgot Password - Mock Image 10)
// =========================================================================

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit
) {
    var email by remember { mutableStateOf("akshitchawla.2k20@gmail.com") }
    var pass by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var needOtp by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf(Session.serverUrl) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    fun finish(r: AuthResponse) {
        if (r.token.isNullOrBlank()) {
            toast(r.message ?: "Login failed")
        } else {
            Session.save(r)
            onLoggedIn()
        }
    }

    AtmosphericBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Branding Logo & Tagline (Image 7)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CineSyncLogo(
                    size = 76.dp,
                    modifier = Modifier.clickable {
                        customUrl = Session.serverUrl
                        showServerDialog = true
                    }
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "CineSync",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary
                )
                Text(
                    text = "The Modern Cinematic Experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CineColors.TextSecondary
                )
            }

            Spacer(Modifier.height(32.dp))

            // 2. Sign In Liquid Glass Container (Image 10)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Welcome to CineSync",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Discover premieres, lock IMAX seats, and manage your ticket wallet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CineColors.TextSecondary
                )

                Spacer(Modifier.height(20.dp))

                // Email field
                GlassInputField(
                    value = email,
                    onChange = { email = it },
                    label = "Email or Phone",
                    type = KeyboardType.Email
                )

                Spacer(Modifier.height(12.dp))

                // Password field
                GlassInputField(
                    value = pass,
                    onChange = { pass = it },
                    label = "Password",
                    password = true
                )

                if (needOtp) {
                    Spacer(Modifier.height(12.dp))
                    GlassInputField(
                        value = otp,
                        onChange = { otp = it },
                        label = "Verification Code (OTP)",
                        type = KeyboardType.NumberPassword
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Primary "Sign In & Continue" Button
                LiquidButton(
                    text = if (busy) "Authenticating…" else if (needOtp) "Verify & Sign In" else "Sign In & Continue",
                    icon = Icons.Default.Lock,
                    enabled = !busy && email.isNotBlank() && (pass.isNotEmpty() || needOtp),
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                if (needOtp) {
                                    finish(Net.api.verifyLoginOtp(VerifyOtpRequest(email.trim(), otp.trim())))
                                } else {
                                    val r = Net.api.login(LoginRequest(email.trim(), pass))
                                    if (r.requires2fa && r.token.isNullOrBlank()) {
                                        needOtp = true
                                        toast(r.message ?: "Enter the code we emailed you")
                                    } else finish(r)
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: java.io.IOException) {
                                showServerDialog = true
                                toast("Can't reach backend. Please configure your laptop Wi-Fi IP.")
                            } catch (e: HttpException) {
                                if (e.code() in listOf(500, 502, 503, 504, 521, 522, 530)) {
                                    showServerDialog = true
                                    toast("Tunnel offline (${e.code()}). Update to your local Wi-Fi IP.")
                                } else {
                                    toast(errorMessage(e))
                                }
                            } catch (e: Exception) {
                                toast(errorMessage(e))
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // Divider OR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CineColors.GlassBorderSubtle)
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.labelSmall,
                        color = CineColors.TextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CineColors.GlassBorderSubtle)
                }

                Spacer(Modifier.height(16.dp))

                // "Continue as Guest" Button (logs in directly with verified seeded account for frictionless demo)
                LiquidSecondaryButton(
                    text = "Continue as Guest",
                    icon = Icons.Default.Person,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val guestRes = Net.api.login(LoginRequest("satvik@gmail.com", "password123"))
                                finish(guestRes)
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: java.io.IOException) {
                                showServerDialog = true
                                toast("Can't reach backend. Please configure your laptop Wi-Fi IP.")
                            } catch (e: HttpException) {
                                if (e.code() in listOf(500, 502, 503, 504, 521, 522, 530)) {
                                    showServerDialog = true
                                    toast("Tunnel offline (${e.code()}). Update to your local Wi-Fi IP.")
                                } else {
                                    toast(errorMessage(e))
                                }
                            } catch (e: Exception) {
                                toast(errorMessage(e))
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Forgot Password link
                TextButton(
                    onClick = onForgot,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Forgot password?",
                        color = CineColors.LaserCyan,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Create Account link at bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to CineSync? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CineColors.TextSecondary
                )
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.PrimaryCoral,
                    modifier = Modifier.clickable(onClick = onRegister)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Discreet Server Connection Link (Tap logo or this text to change IP)
            Text(
                text = "Server: ${Session.serverUrl}",
                style = MaterialTheme.typography.labelSmall,
                color = CineColors.TextMuted.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        customUrl = Session.serverUrl
                        showServerDialog = true
                    }
            )
        }

        // Liquid Glassmorphic Server IP Dialog
        if (showServerDialog) {
            AlertDialog(
                onDismissRequest = { showServerDialog = false },
                containerColor = Color(0xFF141724),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "Backend Server Connection",
                        color = CineColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "To test over the same Wi-Fi network, enter your laptop's Wi-Fi IP address (run 'hostname -I' on Linux Mint):",
                            color = CineColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(14.dp))
                        GlassInputField(
                            value = customUrl,
                            onChange = { customUrl = it },
                            label = "Server IP / URL",
                            type = KeyboardType.Uri
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Example: http://192.168.1.15:8080/ or https://...",
                            color = CineColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                confirmButton = {
                    LiquidButton(
                        text = "Save & Connect",
                        onClick = {
                            Session.updateServerUrl(customUrl.trim())
                            showServerDialog = false
                            toast("Server updated to: ${Session.serverUrl}")
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showServerDialog = false }) {
                        Text("Cancel", color = CineColors.TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun RegisterScreen(onDone: () -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    ScreenScaffold(title = "Create Account", onBack = onBack) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Join CineSync",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary
                )
                Text(
                    text = "We will dispatch a 6-digit verification code to your email.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CineColors.TextSecondary
                )

                Spacer(Modifier.height(20.dp))

                GlassInputField(value = name, onChange = { name = it }, label = "Full Name")
                Spacer(Modifier.height(10.dp))
                GlassInputField(value = email, onChange = { email = it }, label = "Email", type = KeyboardType.Email)
                Spacer(Modifier.height(10.dp))
                GlassInputField(value = pass, onChange = { pass = it }, label = "Password (min 6 chars)", password = true)

                if (otpSent) {
                    Spacer(Modifier.height(10.dp))
                    GlassInputField(value = otp, onChange = { otp = it }, label = "6-Digit Verification Code", type = KeyboardType.NumberPassword)
                }

                Spacer(Modifier.height(20.dp))

                if (!otpSent) {
                    LiquidButton(
                        text = if (busy) "Sending Code…" else "Send Verification Code",
                        enabled = !busy && name.isNotBlank() && email.contains('@') && pass.length >= 6,
                        onClick = {
                            scope.launchSafe(toast, { busy = false }) {
                                busy = true
                                val r = Net.api.sendRegistrationOtp(email.trim())
                                otpSent = true
                                toast(r.message ?: "Code sent to $email")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LiquidButton(
                        text = if (busy) "Completing Signup…" else "Verify & Create Account",
                        enabled = !busy && otp.length >= 4,
                        onClick = {
                            scope.launchSafe(toast, { busy = false }) {
                                busy = true
                                val r = Net.api.register(RegisterRequest(name.trim(), email.trim(), pass, otp.trim()))
                                if (r.token.isNullOrBlank()) {
                                    toast(r.message ?: "Registration failed")
                                } else {
                                    Session.save(r)
                                    toast("Welcome to CineSync!")
                                    onDone()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ForgotScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    ScreenScaffold(title = "Reset Password", onBack = onBack) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Recover Password",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary
                )
                Text(
                    text = "Enter your registered email to receive a password recovery code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CineColors.TextSecondary
                )

                Spacer(Modifier.height(20.dp))

                GlassInputField(value = email, onChange = { email = it }, label = "Email", type = KeyboardType.Email)

                if (step == 1) {
                    Spacer(Modifier.height(10.dp))
                    GlassInputField(value = otp, onChange = { otp = it }, label = "Recovery Code", type = KeyboardType.NumberPassword)
                    Spacer(Modifier.height(10.dp))
                    GlassInputField(value = pass, onChange = { pass = it }, label = "New Password (min 6 chars)", password = true)
                }

                Spacer(Modifier.height(20.dp))

                if (step == 0) {
                    LiquidButton(
                        text = if (busy) "Sending…" else "Send Recovery Code",
                        enabled = !busy && email.contains('@'),
                        onClick = {
                            scope.launchSafe(toast, { busy = false }) {
                                busy = true
                                val r = Net.api.forgotPassword(email.trim())
                                step = 1
                                toast(r.message ?: "Recovery code dispatched")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LiquidButton(
                        text = if (busy) "Resetting…" else "Set New Password",
                        enabled = !busy && otp.isNotBlank() && pass.length >= 6,
                        onClick = {
                            scope.launchSafe(toast, { busy = false }) {
                                busy = true
                                val r = Net.api.resetPassword(ResetPasswordRequest(email.trim(), otp.trim(), pass))
                                toast(r.message ?: "Password updated successfully!")
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassInputField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
    type: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = CineColors.TextSecondary) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else type),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CineColors.GlassWhiteSubtle,
            unfocusedContainerColor = CineColors.GlassWhiteSubtle,
            focusedBorderColor = CineColors.PrimaryCoral,
            unfocusedBorderColor = CineColors.GlassBorderSubtle,
            cursorColor = CineColors.PrimaryCoral,
            focusedTextColor = CineColors.TextPrimary,
            unfocusedTextColor = CineColors.TextPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
