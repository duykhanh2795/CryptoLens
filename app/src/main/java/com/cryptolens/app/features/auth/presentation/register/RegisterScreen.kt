package com.cryptolens.app.features.auth.presentation.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
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
import com.cryptolens.app.features.auth.presentation.login.CryptoTextField
import com.cryptolens.app.ui.theme.*

@Composable
fun RegisterScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RegisterEffect.NavigateToHome  -> onNavigateToHome()
                is RegisterEffect.ShowError       -> snackbarHostState.showSnackbar(effect.message)
                is RegisterEffect.ShowSuccess     -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = SurfaceVariant,
                    contentColor   = TextPrimary,
                    actionColor    = BinanceYellow,
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
        ) {
            // Top bar
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                Text(
                    text       = "Create Account",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(8.dp))

                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(BinanceYellow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("CL", color = Color(0xFF1A1400), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Join CryptoLens", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("Start your smart crypto journey", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(32.dp))

                // Display Name
                CryptoTextField(
                    value           = uiState.displayName,
                    onValueChange   = { viewModel.onEvent(RegisterEvent.DisplayNameChanged(it)) },
                    label           = "Full Name",
                    leadingIcon     = Icons.Outlined.Person,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction    = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    error           = uiState.displayNameError,
                )

                Spacer(Modifier.height(14.dp))

                // Email
                CryptoTextField(
                    value           = uiState.email,
                    onValueChange   = { viewModel.onEvent(RegisterEvent.EmailChanged(it)) },
                    label           = "Email",
                    leadingIcon     = Icons.Outlined.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction    = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    error           = uiState.emailError,
                )

                Spacer(Modifier.height(14.dp))

                // Password
                CryptoTextField(
                    value             = uiState.password,
                    onValueChange     = { viewModel.onEvent(RegisterEvent.PasswordChanged(it)) },
                    label             = "Password",
                    leadingIcon       = Icons.Outlined.Lock,
                    isPassword        = true,
                    isPasswordVisible = uiState.isPasswordVisible,
                    onTogglePassword  = { viewModel.onEvent(RegisterEvent.TogglePasswordVisibility) },
                    keyboardOptions   = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    error           = uiState.passwordError,
                )

                // Password strength indicator
                AnimatedVisibility(visible = uiState.password.isNotEmpty()) {
                    PasswordStrengthBar(password = uiState.password)
                }

                Spacer(Modifier.height(14.dp))

                // Confirm Password
                CryptoTextField(
                    value             = uiState.confirmPassword,
                    onValueChange     = { viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
                    label             = "Confirm Password",
                    leadingIcon       = Icons.Outlined.LockOpen,
                    isPassword        = true,
                    isPasswordVisible = uiState.isConfirmVisible,
                    onTogglePassword  = { viewModel.onEvent(RegisterEvent.ToggleConfirmVisibility) },
                    keyboardOptions   = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        viewModel.onEvent(RegisterEvent.Submit)
                    }),
                    error = uiState.confirmPasswordError,
                )

                Spacer(Modifier.height(20.dp))

                // Terms checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariant)
                        .clickable { viewModel.onEvent(RegisterEvent.ToggleTerms) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Checkbox(
                        checked  = uiState.agreedToTerms,
                        onCheckedChange = { viewModel.onEvent(RegisterEvent.ToggleTerms) },
                        colors   = CheckboxDefaults.colors(
                            checkedColor         = BinanceYellow,
                            uncheckedColor       = TextSecondary,
                            checkmarkColor       = Color(0xFF1A1400),
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = "I agree to the Terms of Service and Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Register button
                Button(
                    onClick  = {
                        keyboardController?.hide()
                        viewModel.onEvent(RegisterEvent.Submit)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled  = !uiState.isLoading,
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = BinanceYellow,
                        contentColor           = Color(0xFF1A1400),
                        disabledContainerColor = BinanceYellow.copy(alpha = 0.5f),
                    ),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = Color(0xFF1A1400),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Login prompt
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("Already have an account? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onNavigateBack, contentPadding = PaddingValues(0.dp)) {
                        Text("Log In", color = BinanceYellow, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
    val strength = when {
        password.length >= 8 &&
        password.any { it.isUpperCase() } &&
        password.any { it.isDigit() } &&
        password.any { !it.isLetterOrDigit() } -> 4
        password.length >= 8 &&
        password.any { it.isUpperCase() } &&
        password.any { it.isDigit() } -> 3
        password.length >= 6 &&
        password.any { it.isDigit() } -> 2
        else -> 1
    }
    val (label, color) = when (strength) {
        4    -> "Strong" to GreenUp
        3    -> "Good"   to Color(0xFF87CEEB)
        2    -> "Fair"   to BinanceYellow
        else -> "Weak"   to RedDown
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < strength) color else SurfaceVariant),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Password strength: $label",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
