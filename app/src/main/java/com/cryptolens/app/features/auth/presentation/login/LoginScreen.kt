package com.cryptolens.app.features.auth.presentation.login

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.ui.theme.*

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showForgotPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginEffect.NavigateToHome  -> onNavigateToHome()
                is LoginEffect.ShowError       -> snackbarHostState.showSnackbar(effect.message)
                is LoginEffect.ShowSuccess     -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData    = data,
                    containerColor  = SurfaceVariant,
                    contentColor    = TextPrimary,
                    actionColor     = BinanceYellow,
                )
            }
        },
        containerColor = BackgroundDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo & title
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BinanceYellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("CL", color = Color(0xFF1A1400), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(16.dp))
                Text("Welcome back", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text("Log in to your CryptoLens account", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(Modifier.height(40.dp))

            // Email field
            CryptoTextField(
                value         = uiState.email,
                onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                label         = "Email",
                leadingIcon   = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                error = uiState.emailError,
            )

            Spacer(Modifier.height(14.dp))

            // Password field
            CryptoTextField(
                value         = uiState.password,
                onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                label         = "Password",
                leadingIcon   = Icons.Outlined.Lock,
                isPassword    = true,
                isPasswordVisible = uiState.isPasswordVisible,
                onTogglePassword = { viewModel.onEvent(LoginEvent.TogglePasswordVisibility) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    viewModel.onEvent(LoginEvent.Submit)
                }),
                error = uiState.passwordError,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.onEvent(LoginEvent.RememberLoginChanged(!uiState.rememberLogin))
                        }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = uiState.rememberLogin,
                        onCheckedChange = { checked ->
                            viewModel.onEvent(LoginEvent.RememberLoginChanged(checked))
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = BinanceYellow,
                            uncheckedColor = TextSecondary,
                            checkmarkColor = Color(0xFF1A1400),
                        ),
                    )
                    Text(
                        "Remember me",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                TextButton(onClick = { showForgotPassword = true }) {
                    Text("Forgot password?", color = BinanceYellow, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(28.dp))

            // Login button
            Button(
                onClick  = {
                    keyboardController?.hide()
                    viewModel.onEvent(LoginEvent.Submit)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !uiState.isLoading,
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = BinanceYellow,
                    contentColor   = Color(0xFF1A1400),
                    disabledContainerColor = BinanceYellow.copy(alpha = 0.5f),
                ),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(20.dp),
                        color     = Color(0xFF1A1400),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
                Text("  or  ", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
            }

            Spacer(Modifier.height(24.dp))

            // Register prompt
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                Text("Don't have an account? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick      = onNavigateToRegister,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        "Register Now",
                        color      = BinanceYellow,
                        fontWeight = FontWeight.SemiBold,
                        style      = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // Forgot password dialog
    if (showForgotPassword) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPassword = false },
            onSubmit  = { email ->
                showForgotPassword = false
                viewModel.onEvent(LoginEvent.ForgotPassword(email))
            },
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor  = TextSecondary,
        title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
        text  = {
            Column {
                Text("Enter your email and we'll send you a link to reset your password.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                CryptoTextField(
                    value         = email,
                    onValueChange = { email = it },
                    label         = "Email",
                    leadingIcon   = Icons.Outlined.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(email) }) {
                Text("Send", color = BinanceYellow, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

// ── Shared TextField Component ────────────────────────────────────────────────

@Composable
fun CryptoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    error: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            label         = { Text(label) },
            leadingIcon   = {
                Icon(leadingIcon, contentDescription = null, tint = if (error != null) RedDown else TextSecondary)
            },
            trailingIcon  = if (isPassword) ({
                IconButton(onClick = { onTogglePassword?.invoke() }) {
                    Icon(
                        imageVector       = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint              = TextSecondary,
                    )
                }
            }) else null,
            visualTransformation = if (isPassword && !isPasswordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError         = error != null,
            singleLine      = true,
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(8.dp),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor     = Color.Transparent,
                focusedLabelColor    = BinanceYellow,
                unfocusedLabelColor  = TextSecondary,
                errorLabelColor      = RedDown,
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
                cursorColor          = BinanceYellow,
                focusedContainerColor   = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant,
                errorContainerColor     = RedDownSurface,
                focusedLeadingIconColor  = BinanceYellow,
                unfocusedLeadingIconColor = TextSecondary,
            ),
        )
        AnimatedVisibility(visible = error != null) {
            error?.let {
                Row(
                    modifier            = Modifier.padding(start = 4.dp, top = 4.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = RedDown, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(it, color = RedDown, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
