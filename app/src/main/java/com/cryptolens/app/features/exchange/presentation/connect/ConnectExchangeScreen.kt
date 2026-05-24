package com.cryptolens.app.features.exchange.presentation.connect

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.features.exchange.domain.model.ExchangeType
import com.cryptolens.app.ui.theme.*

@Composable
fun ConnectExchangeScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConnectExchangeViewModel = hiltViewModel(),
) {
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ConnectExchangeEffect.ShowError   -> snackbar.showSnackbar(effect.message)
                is ConnectExchangeEffect.ShowSuccess -> snackbar.showSnackbar(effect.message)
                is ConnectExchangeEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
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
        ) {
            // ── Top Bar ────────────────────────────────────────────────────
            ConnectTopBar(
                step       = uiState.step,
                onBack     = {
                    when (uiState.step) {
                        ConnectStep.SELECT_EXCHANGE -> onNavigateBack()
                        ConnectStep.ENTER_KEYS      -> onNavigateBack()
                        ConnectStep.VALIDATED       ->
                            viewModel.onEvent(ConnectExchangeEvent.BackToEnterKeys)
                    }
                },
            )

            // ── Step Indicator ─────────────────────────────────────────────
            StepIndicator(currentStep = uiState.step)

            // ── Step Content ───────────────────────────────────────────────
            AnimatedContent(
                targetState    = uiState.step,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                    (fadeOut() + slideOutHorizontally { -it / 4 })
                },
                label = "step_content",
            ) { step ->
                when (step) {
                    ConnectStep.SELECT_EXCHANGE -> SelectExchangeStep(
                        onExchangeSelected = { type ->
                            viewModel.onEvent(ConnectExchangeEvent.ExchangeSelected(type))
                        }
                    )
                    ConnectStep.ENTER_KEYS -> EnterKeysStep(
                        uiState  = uiState,
                        onEvent  = viewModel::onEvent,
                    )
                    ConnectStep.VALIDATED -> ValidatedStep(
                        uiState  = uiState,
                        onEvent  = viewModel::onEvent,
                    )
                }
            }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun ConnectTopBar(step: ConnectStep, onBack: () -> Unit) {
    val title = when (step) {
        ConnectStep.SELECT_EXCHANGE -> "Connect Exchange"
        ConnectStep.ENTER_KEYS      -> "Enter API Keys"
        ConnectStep.VALIDATED       -> "Confirm Connection"
    }
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector        = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint               = TextPrimary,
                modifier           = Modifier.size(20.dp),
            )
        }
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
        )
    }
}

// ── Step Indicator ─────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(currentStep: ConnectStep) {
    val steps = listOf("Exchange", "API Keys", "Confirm")
    val currentIdx = ConnectStep.entries.indexOf(currentStep)

    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { idx, label ->
            val isDone    = idx < currentIdx
            val isCurrent = idx == currentIdx

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier         = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone    -> GreenUp
                                isCurrent -> BinanceYellow
                                else      -> SurfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text(
                            text       = "${idx + 1}",
                            color      = if (isCurrent) Color(0xFF1A1400) else TextTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isDone    -> GreenUp
                        isCurrent -> BinanceYellow
                        else      -> TextTertiary
                    },
                )
            }

            // Connector line (between steps)
            if (idx < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .padding(horizontal = 4.dp)
                        .background(if (idx < currentIdx) GreenUp else SurfaceVariant),
                )
            }
        }
    }
}

// ── Step 1: Select Exchange ────────────────────────────────────────────────────

@Composable
private fun SelectExchangeStep(onExchangeSelected: (ExchangeType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text  = "Select Exchange",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Choose the exchange you want to import your portfolio from.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))

        ExchangeType.entries.forEach { exchange ->
            ExchangeCard(
                exchange  = exchange,
                onClick   = { onExchangeSelected(exchange) },
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Security note
        SecurityInfoCard()
    }
}

@Composable
private fun ExchangeCard(exchange: ExchangeType, onClick: () -> Unit) {
    val isAvailable = exchange == ExchangeType.BINANCE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
                        .then(if (isAvailable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Exchange icon placeholder
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = exchange.displayName.take(2).uppercase(),
                color      = if (isAvailable) BinanceYellow else TextTertiary,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 16.sp,
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = exchange.displayName,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isAvailable) TextPrimary else TextTertiary,
                )
                if (!isAvailable) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text  = "Coming Soon",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                        )
                    }
                }
            }
            Text(
                text  = if (isAvailable) "Import trades · Read-only access"
                        else "Support coming in future update",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }

        if (isAvailable) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint     = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Step 2: Enter API Keys ─────────────────────────────────────────────────────

@Composable
private fun EnterKeysStep(
    uiState: ConnectExchangeUiState,
    onEvent: (ConnectExchangeEvent) -> Unit,
) {
    val focusManager  = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler    = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        // Exchange badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BinanceYellow.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = uiState.selectedExchange.displayName.take(2).uppercase(),
                    color      = BinanceYellow,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 13.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text       = "${uiState.selectedExchange.displayName} API Keys",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                )
                Text(
                    text  = "Read-only permission required",
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenUp,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // How to get API key button
        OutlinedButton(
            onClick = {
                uriHandler.openUri(uiState.selectedExchange.apiDocsUrl)
            },
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = BinanceYellow),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("How to create API Key on ${uiState.selectedExchange.displayName}")
        }

        Spacer(Modifier.height(20.dp))

        // Label field
        ApiKeyTextField(
            value         = uiState.label,
            onValueChange = { onEvent(ConnectExchangeEvent.LabelChanged(it)) },
            label         = "Nickname (optional)",
            placeholder   = "e.g. My Binance Main",
            leadingIcon   = Icons.AutoMirrored.Outlined.Label,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
        )

        Spacer(Modifier.height(14.dp))

        // API Key field
        ApiKeyTextField(
            value         = uiState.apiKey,
            onValueChange = { onEvent(ConnectExchangeEvent.ApiKeyChanged(it)) },
            label         = "API Key",
            placeholder   = "Paste your API Key here",
            leadingIcon   = Icons.Outlined.VpnKey,
            isPassword    = !uiState.isApiKeyVisible,
            trailingIcon  = {
                Row {
                    IconButton(onClick = { onEvent(ConnectExchangeEvent.ToggleApiKeyVisibility) }) {
                        Icon(
                            if (uiState.isApiKeyVisible) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = TextSecondary,
                        )
                    }
                    IconButton(onClick = {
                        val clip = clipboardManager.getText()?.text ?: return@IconButton
                        onEvent(ConnectExchangeEvent.ApiKeyChanged(clip))
                    }) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste", tint = TextSecondary)
                    }
                }
            },
            error         = uiState.apiKeyError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
        )

        Spacer(Modifier.height(14.dp))

        // API Secret field
        ApiKeyTextField(
            value         = uiState.apiSecret,
            onValueChange = { onEvent(ConnectExchangeEvent.ApiSecretChanged(it)) },
            label         = "API Secret",
            placeholder   = "Paste your API Secret here",
            leadingIcon   = Icons.Outlined.Lock,
            isPassword    = !uiState.isApiSecretVisible,
            trailingIcon  = {
                Row {
                    IconButton(onClick = { onEvent(ConnectExchangeEvent.ToggleApiSecretVisibility) }) {
                        Icon(
                            if (uiState.isApiSecretVisible) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = TextSecondary,
                        )
                    }
                    IconButton(onClick = {
                        val clip = clipboardManager.getText()?.text ?: return@IconButton
                        onEvent(ConnectExchangeEvent.ApiSecretChanged(clip))
                    }) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste", tint = TextSecondary)
                    }
                }
            },
            error         = uiState.apiSecretError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )

        Spacer(Modifier.height(20.dp))

        SecurityInfoCard()

        Spacer(Modifier.height(24.dp))

        // Validate button
        Button(
            onClick  = { onEvent(ConnectExchangeEvent.ValidateAndConnect) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled  = !uiState.isValidating,
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = BinanceYellow,
                contentColor           = Color(0xFF1A1400),
                disabledContainerColor = BinanceYellow.copy(alpha = 0.5f),
                disabledContentColor   = Color(0xFF1A1400).copy(alpha = 0.5f),
            ),
        ) {
            if (uiState.isValidating) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = Color(0xFF1A1400),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text("Validating...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Validate & Connect", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ── Step 3: Validated / Confirm ────────────────────────────────────────────────

@Composable
private fun ValidatedStep(
    uiState: ConnectExchangeUiState,
    onEvent: (ConnectExchangeEvent) -> Unit,
) {
    val validation = uiState.validation ?: return

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        // Success icon
        Box(
            modifier         = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(GreenUpSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Verified,
                contentDescription = null,
                tint     = GreenUp,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text       = "API Key Verified!",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = GreenUp,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = "Your ${uiState.selectedExchange.displayName} account is ready to connect.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        // Account info card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .padding(16.dp),
        ) {
            Text(
                text       = "Account Details",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
            )
            Spacer(Modifier.height(12.dp))

            ValidationRow("Exchange",     uiState.selectedExchange.displayName)
            ValidationRow("Account Type", validation.accountType.ifBlank { "Spot" })
            ValidationRow(
                label = "Permissions",
                value = buildString {
                    append(if (validation.canRead) "✅ Read" else "❌ Read")
                    append("  ")
                    append(if (!validation.canTrade) "✅ No Trade" else "⚠️ Trade")
                }
            )
            ValidationRow(
                label = "API Key",
                value = if (uiState.apiKey.length > 8)
                    uiState.apiKey.take(4) + "••••" + uiState.apiKey.takeLast(4)
                else "••••••••"
            )
            if (uiState.label.isNotBlank()) {
                ValidationRow("Nickname", uiState.label)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Warning note about read-only
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(GreenUpSurface)
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint     = GreenUp,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "Your API Secret is encrypted with AES-256 and stored only on this device. " +
                        "CryptoLens can never make trades on your behalf.",
                style = MaterialTheme.typography.labelSmall,
                color = GreenUp,
            )
        }

        Spacer(Modifier.height(28.dp))

        // Save connection button
        Button(
            onClick  = { onEvent(ConnectExchangeEvent.SaveConnection) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled  = !uiState.isSaving,
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = BinanceYellow,
                contentColor           = Color(0xFF1A1400),
                disabledContainerColor = BinanceYellow.copy(alpha = 0.5f),
                disabledContentColor   = Color(0xFF1A1400).copy(alpha = 0.5f),
            ),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = Color(0xFF1A1400),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text("Saving...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save & Connect", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Back button
        OutlinedButton(
            onClick  = { onEvent(ConnectExchangeEvent.BackToEnterKeys) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
        ) {
            Text("Edit API Keys")
        }
    }
}

// ── Shared Components ──────────────────────────────────────────────────────────

@Composable
private fun ValidationRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium, color = TextPrimary)
    }
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
}

@Composable
private fun SecurityInfoCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Default.Security,
            contentDescription = null,
            tint     = BinanceYellow,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text       = "Security Notice",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = BinanceYellow,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "• Use Read-Only API Keys only — no Withdraw or Trade permission\n" +
                        "• Your secret is AES-256 encrypted on-device\n" +
                        "• CryptoLens cannot make trades on your behalf\n" +
                        "• You can delete this connection anytime",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun ApiKeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column {
        OutlinedTextField(
            value              = value,
            onValueChange      = onValueChange,
            label              = { Text(label) },
            placeholder        = { Text(placeholder, color = TextTertiary) },
            leadingIcon        = {
                Icon(leadingIcon, contentDescription = null,
                    tint = if (error != null) RedDown else TextSecondary)
            },
            trailingIcon       = trailingIcon,
            visualTransformation = if (isPassword) PasswordVisualTransformation()
                                   else VisualTransformation.None,
            isError            = error != null,
            singleLine         = true,
            keyboardOptions    = keyboardOptions,
            keyboardActions    = keyboardActions,
            modifier           = Modifier.fillMaxWidth(),
            shape              = RoundedCornerShape(8.dp),
            colors             = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Color.Transparent,
                unfocusedBorderColor    = Color.Transparent,
                errorBorderColor        = Color.Transparent,
                focusedLabelColor       = BinanceYellow,
                unfocusedLabelColor     = TextSecondary,
                errorLabelColor         = RedDown,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                cursorColor             = BinanceYellow,
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
                    modifier          = Modifier.padding(start = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null,
                        tint = RedDown, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(it, color = RedDown, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
