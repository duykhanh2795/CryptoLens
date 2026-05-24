package com.cryptolens.app.features.ai.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.features.ai.domain.model.AiChatMessage
import com.cryptolens.app.features.ai.domain.model.AiMessageRole
import com.cryptolens.app.ui.theme.BackgroundDark
import com.cryptolens.app.ui.theme.BinanceYellow
import com.cryptolens.app.ui.theme.BorderColor
import com.cryptolens.app.ui.theme.GreenUp
import com.cryptolens.app.ui.theme.SurfaceVariant
import com.cryptolens.app.ui.theme.TextPrimary
import com.cryptolens.app.ui.theme.TextSecondary
import com.cryptolens.app.ui.theme.TextTertiary
import kotlin.math.roundToInt

private data class StarterPrompt(
    val label: String,
    val text: String,
)

private val starterPrompts = listOf(
    StarterPrompt(
        label = "Fast",
        text = "Tin tức pháp lý crypto mới nhất đang ảnh hưởng gì tới thị trường?",
    ),
    StarterPrompt(
        label = "Fast",
        text = "Vì sao Hyperliquid đang được nhắc nhiều trong cộng đồng crypto gần đây?",
    ),
    StarterPrompt(
        label = "Deep Research",
        text = "Những altcoin như SUI, NEAR hoặc LINK đang có setup nào đáng chú ý?",
    ),
    StarterPrompt(
        label = "Backtest",
        text = "So sánh portfolio top 5 crypto equal-weight và market-cap-weight trong 2 năm.",
    ),
)

@Composable
fun AiAssistantOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    viewModel: AiAssistantViewModel = hiltViewModel(),
) {
    if (!visible) return

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var chatOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (chatOpen) {
            AiChatScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onClose = { chatOpen = false },
            )
        } else {
            FloatingAiButton(
                modifier = Modifier.align(Alignment.TopStart),
                onClick = { chatOpen = true },
            )
        }
    }
}

@Composable
private fun FloatingAiButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fabSize = 52.dp
        val margin = 16.dp
        val bottomMargin = 34.dp
        val topDock = 92.dp
        val fabSizePx = with(density) { fabSize.toPx() }
        val marginPx = with(density) { margin.toPx() }
        val bottomMarginPx = with(density) { bottomMargin.toPx() }
        val topDockPx = with(density) { topDock.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val minX = marginPx
        val minY = marginPx
        val maxX = (maxWidthPx - fabSizePx - marginPx).coerceAtLeast(minX)
        val maxY = (maxHeightPx - fabSizePx - bottomMarginPx).coerceAtLeast(minY)

        var x by rememberSaveable { mutableFloatStateOf(Float.NaN) }
        var y by rememberSaveable { mutableFloatStateOf(Float.NaN) }

        LaunchedEffect(maxX, maxY) {
            if (x.isNaN()) x = maxX
            if (y.isNaN()) y = topDockPx.coerceIn(minY, maxY)
            x = x.coerceIn(minX, maxX)
            y = y.coerceIn(minY, maxY)
        }

        AnimatedVisibility(
            visible = !x.isNaN() && !y.isNaN(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) },
        ) {
            Surface(
                modifier = Modifier
                    .size(fabSize)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                                        .background(
                        Brush.linearGradient(
                            colors = listOf(BinanceYellow, Color(0xFF7C6FE8), Color(0xFF9EA4AD)),
                            start = Offset.Zero,
                            end = Offset(180f, 180f),
                        ),
                    )
                    .clickable(onClick = onClick)
                    .pointerInput(maxX, maxY) {
                        detectDragGestures { _, dragAmount ->
                            x = (x + dragAmount.x).coerceIn(minX, maxX)
                            y = (y + dragAmount.y).coerceIn(minY, maxY)
                        }
                    },
                color = Color.Transparent,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BackgroundDark.copy(alpha = 0.66f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiChatScreen(
    uiState: AiAssistantUiState,
    onEvent: (AiAssistantEvent) -> Unit,
    onClose: () -> Unit,
) {
    var drawerOpen by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF17161B)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AiChatTopBar(
                onMenu = { drawerOpen = true },
                onClose = onClose,
            )

            if (uiState.messages.isEmpty()) {
                PromptStartContent(
                    modifier = Modifier.weight(1f),
                    onPrompt = { onEvent(AiAssistantEvent.PromptSelected(it)) },
                )
            } else {
                ChatMessages(
                    messages = uiState.messages,
                    isSending = uiState.isSending || uiState.isAnalyzing,
                    modifier = Modifier.weight(1f),
                )
            }

            if (uiState.error != null) {
                AiInlineError(
                    message = uiState.error,
                    onDismiss = { onEvent(AiAssistantEvent.DismissError) },
                )
            }

            ChatComposer(
                value = uiState.input,
                isSending = uiState.isSending || uiState.isAnalyzing,
                onValueChange = { onEvent(AiAssistantEvent.InputChanged(it)) },
                onSend = { onEvent(AiAssistantEvent.SendMessage) },
            )
        }

        if (drawerOpen) {
            AiSidePanel(
                onClose = { drawerOpen = false },
                onNewChat = {
                    onEvent(AiAssistantEvent.NewChat)
                    drawerOpen = false
                },
            )
        }
    }
}

@Composable
private fun AiChatTopBar(
    onMenu: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = TextPrimary, modifier = Modifier.size(31.dp))
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close AI", tint = TextSecondary, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun PromptStartContent(
    modifier: Modifier = Modifier,
    onPrompt: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text(
                "Select Prompt to Start",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
            )
        }
        items(starterPrompts) { prompt ->
            PromptCard(prompt = prompt, onClick = { onPrompt(prompt.text) })
        }
        item {
            Text(
                "Discover more prompts in prompt library.",
                color = TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun PromptCard(
    prompt: StarterPrompt,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFF202024),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PromptBadge(prompt.label)
            Spacer(Modifier.width(12.dp))
            Text(
                prompt.text,
                color = Color(0xFFD7D4DD),
                style = MaterialTheme.typography.titleMedium,
                lineHeight = MaterialTheme.typography.titleLarge.lineHeight,
            )
        }
    }
}

@Composable
private fun PromptBadge(label: String) {
    Surface(
        color = Color(0xFF2B2930),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (label == "Fast") Icons.Default.Bolt else Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ChatMessages(
    messages: List<AiChatMessage>,
    isSending: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            ChatMessageBubble(message)
        }
        if (isSending) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Surface(color = SurfaceVariant, shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(color = BinanceYellow, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Thinking", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: AiChatMessage) {
    val isUser = message.role == AiMessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) BinanceYellow.copy(alpha = 0.18f) else Color(0xFF202024),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp,
            ),
            modifier = Modifier.fillMaxWidth(0.86f),
        ) {
            Text(
                message.text,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ChatComposer(
    value: String,
    isSending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 18.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                placeholder = {
                    Text("Enter to wrap, tap send button to submit", color = TextTertiary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = BinanceYellow,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                ComposerChip("Auto")
                Spacer(Modifier.width(8.dp))
                Surface(color = GreenUp.copy(alpha = 0.18f), shape = RoundedCornerShape(9.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = GreenUp, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Private", color = GreenUp, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    color = if (value.isNotBlank() && !isSending) BinanceYellow else SurfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(enabled = value.isNotBlank() && !isSending, onClick = onSend),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Send",
                            tint = if (value.isNotBlank() && !isSending) Color(0xFF1A1400) else TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerChip(text: String) {
    Surface(color = Color(0xFF2B2930), shape = RoundedCornerShape(9.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AiInlineError(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        color = Color(0xFF32161B),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, color = Color(0xFFFF6B7D), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFFF6B7D), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AiSidePanel(
    onClose: () -> Unit,
    onNewChat: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable(onClick = onClose),
        )
        Surface(
            color = Color(0xFF202024),
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.68f)
                .widthIn(max = 330.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            shadowElevation = 18.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = Color.Transparent,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                Brush.linearGradient(
                                colors = listOf(Color(0xFF7C6FE8), Color(0xFF9EA4AD)),
                                    start = Offset.Zero,
                                    end = Offset(120f, 120f),
                                ),
                                CircleShape,
                            ),
                    ) {}
                    Spacer(Modifier.width(12.dp))
                    Text("CryptoLens AI", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Close panel", tint = TextSecondary)
                    }
                }
                HorizontalDivider(color = Color(0xFF333039))

                DrawerAction(Icons.Default.AddCircleOutline, "New chat", onNewChat)
                DrawerAction(Icons.Default.AutoAwesome, "Prompt Library", onClose)

                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary)
                        Spacer(Modifier.width(8.dp))
                        Text("Search chats", color = TextTertiary, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                DrawerChatRow("New chat")
                DrawerChatRow("New chat")

                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2830))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(color = Color(0xFFD73E0F), shape = CircleShape, modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("K", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("khanhnd_2795", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun DrawerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(27.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DrawerChatRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Color(0xFFD7D4DD), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextSecondary)
    }
}
