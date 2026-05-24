package com.cryptolens.app.features.converter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptolens.app.core.domain.util.formatLargeNumber
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.ui.theme.BackgroundDark
import com.cryptolens.app.ui.theme.BinanceYellow
import com.cryptolens.app.ui.theme.BorderColor
import com.cryptolens.app.ui.theme.DividerColor
import com.cryptolens.app.ui.theme.GreenUp
import com.cryptolens.app.ui.theme.GreenUpSurface
import com.cryptolens.app.ui.theme.RedDown
import com.cryptolens.app.ui.theme.RedDownSurface
import com.cryptolens.app.ui.theme.SurfaceDark
import com.cryptolens.app.ui.theme.SurfaceElevated
import com.cryptolens.app.ui.theme.SurfaceVariant
import com.cryptolens.app.ui.theme.TextPrimary
import com.cryptolens.app.ui.theme.TextSecondary
import com.cryptolens.app.ui.theme.TextTertiary
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConverterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            ConverterTopBar(
                isLoading = uiState.isLoading,
                onBack = onNavigateBack,
                onRefresh = { viewModel.onEvent(ConverterEvent.Refresh) },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading && uiState.assets.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BinanceYellow)
            }
            uiState.error != null && uiState.assets.isEmpty() -> ConverterError(
                message = uiState.error,
                onRetry = { viewModel.onEvent(ConverterEvent.Refresh) },
                modifier = Modifier.padding(padding),
            )
            else -> ConverterContent(
                state = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (uiState.pickerTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(ConverterEvent.HidePicker) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
        ) {
            AssetPickerSheet(
                state = uiState,
                onEvent = viewModel::onEvent,
            )
        }
    }
}

@Composable
private fun ConverterTopBar(
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = TextPrimary)
        }
        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = BinanceYellow)
        Spacer(Modifier.width(8.dp))
        Text(
            "Converter",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (isLoading) {
            CircularProgressIndicator(
                color = BinanceYellow,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh prices", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun ConverterContent(
    state: ConverterUiState,
    onEvent: (ConverterEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(BackgroundDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ConverterCard(
                state = state,
                onEvent = onEvent,
            )
        }
        item {
            QuickPairsSection(
                state = state,
                onEvent = onEvent,
            )
        }
        item {
            RateSection(state = state)
        }
        item {
            MarketContextSection(state = state)
        }
    }
}

@Composable
private fun ConverterCard(
    state: ConverterUiState,
    onEvent: (ConverterEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Convert", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            AmountAssetRow(
                label = "From",
                amount = state.amountInput,
                asset = state.fromAsset,
                amountEditable = true,
                onAmountChange = { onEvent(ConverterEvent.AmountChanged(it)) },
                onAssetClick = { onEvent(ConverterEvent.ShowPicker(ConverterPickerTarget.FROM)) },
            )

            Box(
                modifier = Modifier.fillMaxWidth().height(42.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = { onEvent(ConverterEvent.SwapAssets) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant),
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Swap assets", tint = BinanceYellow)
                }
            }

            AmountAssetRow(
                label = "To",
                amount = state.convertedAmount.formatAmount(),
                asset = state.toAsset,
                amountEditable = false,
                onAmountChange = {},
                onAssetClick = { onEvent(ConverterEvent.ShowPicker(ConverterPickerTarget.TO)) },
            )
        }
    }
}

@Composable
private fun AmountAssetRow(
    label: String,
    amount: String,
    asset: ConverterAsset?,
    amountEditable: Boolean,
    onAmountChange: (String) -> Unit,
    onAssetClick: () -> Unit,
) {
    Column {
        Text(label, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (amountEditable) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("0", color = TextTertiary) },
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    errorBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
)
            } else {
                Text(
                    amount,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(10.dp))
            AssetPill(asset = asset, onClick = onAssetClick)
        }
    }
}

@Composable
private fun AssetPill(
    asset: ConverterAsset?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(SurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetIcon(asset = asset, size = 24)
        Spacer(Modifier.width(6.dp))
        Text(
            asset?.symbol ?: "--",
            color = TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun QuickPairsSection(
    state: ConverterUiState,
    onEvent: (ConverterEvent) -> Unit,
) {
    val pairs = listOf(
        Triple("BTC/ETH", "bitcoin", "ethereum"),
        Triple("ETH/SOL", "ethereum", "solana"),
        Triple("BTC/USD", "bitcoin", "usd"),
        Triple("ETH/USD", "ethereum", "usd"),
        Triple("SOL/USD", "solana", "usd"),
    ).filter { (_, fromId, toId) ->
        state.assets.any { it.id == fromId } && state.assets.any { it.id == toId }
    }
    if (pairs.isEmpty()) return

    Column {
        Text("Quick pairs", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pairs, key = { "${it.second}-${it.third}" }) { (label, fromId, toId) ->
                OutlinedButton(
                    onClick = { onEvent(ConverterEvent.PairSelected(fromId, toId)) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun RateSection(state: ConverterUiState) {
    val from = state.fromAsset ?: return
    val to = state.toAsset ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Rate", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            RateRow("1 ${from.symbol}", "${state.directRate.formatAmount()} ${to.symbol}")
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
            RateRow("1 ${to.symbol}", "${state.inverseRate.formatAmount()} ${from.symbol}")
        }
    }
}

@Composable
private fun RateRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MarketContextSection(state: ConverterUiState) {
    val assets = listOfNotNull(state.fromAsset, state.toAsset).distinctBy { it.id }
    if (assets.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                "Market context",
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
            assets.forEachIndexed { index, asset ->
                MarketContextRow(asset = asset)
                if (index < assets.lastIndex) {
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(start = 58.dp))
                }
            }
        }
    }
}

@Composable
private fun MarketContextRow(asset: ConverterAsset) {
    val changeColor = if (asset.isPositive) GreenUp else RedDown
    val changeBg = if (asset.isPositive) GreenUpSurface else RedDownSurface
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetIcon(asset = asset, size = 34)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(asset.symbol, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(asset.name, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(asset.priceUsd.formatUsd(), color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (!asset.isFiat) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(changeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "${if (asset.isPositive) "+" else "-"}${abs(asset.priceChangePercent24h).formatPercentText()}",
                        color = changeColor,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetPickerSheet(
    state: ConverterUiState,
    onEvent: (ConverterEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.pickerTarget == ConverterPickerTarget.FROM) "Select source" else "Select target",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onEvent(ConverterEvent.HidePicker) }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onEvent(ConverterEvent.SearchChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            placeholder = { Text("Search coin or symbol", color = TextTertiary) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            errorBorderColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
)
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(420.dp),
        ) {
            items(state.filteredAssets, key = { it.id }) { asset ->
                AssetPickerRow(
                    asset = asset,
                    selected = asset.id == state.fromAsset?.id || asset.id == state.toAsset?.id,
                    onClick = { onEvent(ConverterEvent.AssetSelected(asset.id)) },
                )
                HorizontalDivider(color = DividerColor)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AssetPickerRow(
    asset: ConverterAsset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetIcon(asset = asset, size = 36)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(asset.symbol, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(asset.name, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(asset.priceUsd.formatUsd(), color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            if (selected) {
                Text("Selected", color = BinanceYellow, style = MaterialTheme.typography.labelSmall)
            } else if (asset.rank > 0) {
                Text("#${asset.rank}", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AssetIcon(asset: ConverterAsset?, size: Int) {
    if (asset?.isFiat == true) {
        Box(
            modifier = Modifier.size(size.dp).clip(CircleShape).background(BinanceYellow),
            contentAlignment = Alignment.Center,
        ) {
            Text("$", color = Color(0xFF1A1400), fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.48).sp)
        }
    } else {
        AsyncImage(
            model = asset?.imageUrl,
            contentDescription = asset?.name,
            modifier = Modifier.size(size.dp).clip(CircleShape),
        )
    }
}

@Composable
private fun ConverterError(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Unable to load converter", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message.orEmpty(), color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onRetry) {
            Text("Retry", color = BinanceYellow)
        }
    }
}

private fun Double.formatAmount(): String = when {
    this == 0.0 -> "0"
    this >= 1_000_000 -> formatLargeNumber()
    this >= 1.0 -> "%.6f".format(this).trimTrailingZeroes()
    else -> "%.10f".format(this).trimTrailingZeroes()
}

private fun Double.formatUsd(): String = if (this >= 1_000_000) {
    "$${formatLargeNumber()}"
} else {
    "$${formatPrice()}"
}

private fun Double.formatPercentText(): String = "${"%.2f".format(this)}%"

private fun String.trimTrailingZeroes(): String =
    trimEnd('0').trimEnd('.').ifBlank { "0" }
