package com.cryptolens.app.features.market.presentation.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptolens.app.core.domain.util.formatLargeNumber
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.market.domain.model.ChartMarketType
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.CoinDetail
import com.cryptolens.app.features.market.domain.model.FuturesMarketInfo
import com.cryptolens.app.features.market.domain.model.FuturesRiskLevel
import com.cryptolens.app.features.market.domain.model.FuturesSentiment
import com.cryptolens.app.features.market.domain.model.FuturesSentimentBias
import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.features.news.domain.model.NewsItem
import com.cryptolens.app.features.news.presentation.NewsRow
import com.cryptolens.app.features.portfolio.domain.model.PortfolioAsset
import com.cryptolens.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Entry Point ───────────────────────────────────────────────────────────────

@Composable
fun CoinDetailScreen(
    onNavigateBack: () -> Unit,
    onSetAlert: (Coin, AlertType) -> Unit = { _, _ -> },
    onSeeAllNews: (String) -> Unit = {},
    onOpenPortfolio: () -> Unit = {},
    viewModel: CoinDetailViewModel = hiltViewModel(),
) {
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CoinDetailEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
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
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> CoinDetailLoading(onNavigateBack)
                uiState.error != null -> CoinDetailError(
                    message = uiState.error!!,
                    onRetry = { viewModel.onEvent(CoinDetailEvent.Retry) },
                    onBack  = onNavigateBack,
                )
                uiState.coinDetail != null -> CoinDetailContent(
                    coinDetail        = uiState.coinDetail!!,
                    liveTicker        = uiState.liveTicker,
                    klines            = uiState.klines,
                    markPriceKlines   = uiState.markPriceKlines,
                    futuresMarketInfo = uiState.futuresMarketInfo,
                    futuresSentiment  = uiState.futuresSentiment,
                    selectedInterval  = uiState.selectedInterval,
                    selectedMarketType = uiState.selectedMarketType,
                    selectedChartType = uiState.selectedChartType,
                    isKlinesLoading   = uiState.isKlinesLoading,
                    chartError        = uiState.chartError,
                    isWatchlisted     = uiState.isWatchlisted,
                    portfolioAsset    = uiState.portfolioAsset,
                    portfolioTotalValue = uiState.portfolioTotalValue,
                    relatedNews       = uiState.relatedNews,
                    isNewsLoading     = uiState.isNewsLoading,
                    newsError         = uiState.newsError,
                    onBack            = onNavigateBack,
                    onSetAlert        = onSetAlert,
                    onSeeAllNews      = onSeeAllNews,
                    onOpenPortfolio   = onOpenPortfolio,
                    onIntervalChange  = { viewModel.onEvent(CoinDetailEvent.IntervalChanged(it)) },
                    onMarketTypeChange = { viewModel.onEvent(CoinDetailEvent.MarketTypeChanged(it)) },
                    onChartTypeChange = { viewModel.onEvent(CoinDetailEvent.ChartTypeChanged(it)) },
                    onWatchlistToggle = { viewModel.onEvent(CoinDetailEvent.ToggleWatchlist) },
                )
            }
        }
    }
}

// ── Main Content ──────────────────────────────────────────────────────────────

@Composable
private fun CoinDetailContent(
    coinDetail: CoinDetail,
    liveTicker: PriceTicker?,
    klines: List<Kline>,
    markPriceKlines: List<Kline>,
    futuresMarketInfo: FuturesMarketInfo?,
    futuresSentiment: FuturesSentiment?,
    selectedInterval: ChartInterval,
    selectedMarketType: ChartMarketType,
    selectedChartType: ChartDisplayType,
    isKlinesLoading: Boolean,
    chartError: String?,
    isWatchlisted: Boolean,
    portfolioAsset: PortfolioAsset?,
    portfolioTotalValue: Double,
    relatedNews: List<NewsItem>,
    isNewsLoading: Boolean,
    newsError: String?,
    onBack: () -> Unit,
    onSetAlert: (Coin, AlertType) -> Unit,
    onSeeAllNews: (String) -> Unit,
    onOpenPortfolio: () -> Unit,
    onIntervalChange: (ChartInterval) -> Unit,
    onMarketTypeChange: (ChartMarketType) -> Unit,
    onChartTypeChange: (ChartDisplayType) -> Unit,
    onWatchlistToggle: () -> Unit,
) {
    val coin         = coinDetail.coin
    val displayPrice = liveTicker?.price ?: coin.currentPrice
    val displayPct   = liveTicker?.priceChangePercent ?: coin.priceChangePercent24h
    val isPositive   = displayPct >= 0
    val displayCoin   = coin.copy(currentPrice = displayPrice)

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        CoinDetailTopBar(
            coin              = coin,
            isWatchlisted     = isWatchlisted,
            onBack            = onBack,
            onWatchlistToggle = onWatchlistToggle,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            PriceHeader(
                displayPrice = displayPrice,
                displayPct   = displayPct,
                isPositive   = isPositive,
                isLive       = liveTicker != null,
            )
            AlertQuickActionsRow(
                symbol = coin.symbol,
                onSetAlert = { alertType -> onSetAlert(displayCoin, alertType) },
            )
            ChartControlsRow(
                selectedMarketType = selectedMarketType,
                selectedChartType = selectedChartType,
                onMarketTypeChange = onMarketTypeChange,
                onChartTypeChange = onChartTypeChange,
            )

            if (futuresMarketInfo != null && selectedMarketType == ChartMarketType.FUTURES) {
                FuturesInfoPanel(info = futuresMarketInfo)
            }
            if (futuresSentiment != null && selectedMarketType == ChartMarketType.FUTURES) {
                FuturesSentimentPanel(sentiment = futuresSentiment)
            }

            if (chartError != null) {
                ChartNotice(message = chartError)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp)),
            ) {
                if (isKlinesLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.align(Alignment.Center),
                        color       = BinanceYellow,
                        strokeWidth = 2.dp,
                    )
                } else {
                    val visibleMarkPrices = if (selectedMarketType == ChartMarketType.FUTURES) {
                        markPriceKlines
                    } else {
                        emptyList()
                    }
                    when (selectedChartType) {
                        ChartDisplayType.CANDLE -> CandlestickChart(
                            klines = klines,
                            markPriceKlines = visibleMarkPrices,
                            modifier = Modifier.fillMaxSize(),
                        )
                        ChartDisplayType.LINE -> LinePriceChart(
                            klines = klines,
                            markPriceKlines = visibleMarkPrices,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            IntervalSelector(selected = selectedInterval, onSelect = onIntervalChange)
            Spacer(Modifier.height(10.dp))
            QuickStatsRow(coin = coin)
            Spacer(Modifier.height(10.dp))
            if (portfolioAsset != null) {
                YourHoldingsSection(
                    asset = portfolioAsset,
                    totalPortfolioValue = portfolioTotalValue,
                    currentPrice = displayPrice,
                    onOpenPortfolio = onOpenPortfolio,
                )
                Spacer(Modifier.height(10.dp))
            }
            MarketInfoSection(coin = coin, coinDetail = coinDetail)
            Spacer(Modifier.height(10.dp))
            CoinNewsSection(
                symbol = coin.symbol,
                news = relatedNews,
                isLoading = isNewsLoading,
                error = newsError,
                onSeeAll = { onSeeAllNews(coin.symbol) },
            )
            Spacer(Modifier.height(10.dp))

            if (coinDetail.description.isNotBlank()) {
                AboutSection(description = coinDetail.description)
                HorizontalDivider(color = DividerColor)
            }

            Spacer(Modifier.height(100.dp))
        }

        BuySellBar(symbol = coin.symbol, onOpenPortfolio = onOpenPortfolio)
    }
}

@Composable
private fun ChartControlsRow(
    selectedMarketType: ChartMarketType,
    selectedChartType: ChartDisplayType,
    onMarketTypeChange: (ChartMarketType) -> Unit,
    onChartTypeChange: (ChartDisplayType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Chart",
            color = TextTertiary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        CompactMarketToggle(
            selected = selectedMarketType,
            onSelect = onMarketTypeChange,
        )
        Spacer(Modifier.width(8.dp))
        CompactChartToggle(
            selected = selectedChartType,
            onSelect = onChartTypeChange,
        )
    }
}

@Composable
private fun CompactMarketToggle(
    selected: ChartMarketType,
    onSelect: (ChartMarketType) -> Unit,
) {
    CompactSegmentedIconToggle(
        items = listOf(
            CompactToggleItem(
                selected = selected == ChartMarketType.SPOT,
                icon = Icons.Default.AttachMoney,
                contentDescription = "Spot market",
                onClick = { onSelect(ChartMarketType.SPOT) },
            ),
            CompactToggleItem(
                selected = selected == ChartMarketType.FUTURES,
                icon = Icons.Default.SwapHoriz,
                contentDescription = "Futures market",
                onClick = { onSelect(ChartMarketType.FUTURES) },
            ),
        ),
    )
}

@Composable
private fun CompactChartToggle(
    selected: ChartDisplayType,
    onSelect: (ChartDisplayType) -> Unit,
) {
    CompactSegmentedIconToggle(
        items = listOf(
            CompactToggleItem(
                selected = selected == ChartDisplayType.CANDLE,
                icon = Icons.Default.BarChart,
                contentDescription = "Candlestick chart",
                onClick = { onSelect(ChartDisplayType.CANDLE) },
            ),
            CompactToggleItem(
                selected = selected == ChartDisplayType.LINE,
                icon = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = "Line chart",
                onClick = { onSelect(ChartDisplayType.LINE) },
            ),
        ),
    )
}

private data class CompactToggleItem(
    val selected: Boolean,
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

@Composable
private fun CompactSegmentedIconToggle(
    items: List<CompactToggleItem>,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, BorderColor.copy(alpha = 0.75f)), RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (item.selected) BinanceYellow.copy(alpha = 0.16f)
                        else Color.Transparent,
                    )
                    .clickable(onClick = item.onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.contentDescription,
                    tint = if (item.selected) BinanceYellow else TextSecondary,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun FuturesSentimentPanel(sentiment: FuturesSentiment) {
    val accent = when (sentiment.bias) {
        FuturesSentimentBias.BULLISH -> GreenUp
        FuturesSentimentBias.BEARISH -> RedDown
        FuturesSentimentBias.LONG_CROWDED -> BinanceYellow
        FuturesSentimentBias.SHORT_CROWDED -> BinanceYellow
        FuturesSentimentBias.NEUTRAL -> TextSecondary
        FuturesSentimentBias.UNAVAILABLE -> TextTertiary
    }
    val riskText = when (sentiment.riskLevel) {
        FuturesRiskLevel.LOW -> "Low risk"
        FuturesRiskLevel.MEDIUM -> "Medium risk"
        FuturesRiskLevel.HIGH -> "High risk"
        FuturesRiskLevel.UNKNOWN -> "Unknown risk"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = sentiment.title,
                    color = accent,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = sentiment.summary,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = riskText,
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SentimentMetric(
                label = "Funding",
                value = "${"%.4f".format(sentiment.fundingRatePercent)}%",
                modifier = Modifier.weight(1f),
            )
            SentimentMetric(
                label = "Premium",
                value = "${"%.3f".format(sentiment.premiumPercent)}%",
                modifier = Modifier.weight(1f),
            )
            SentimentMetric(
                label = "OI 24h",
                value = "${"%.2f".format(sentiment.openInterestChangePercent)}%",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))
        sentiment.signals.take(3).forEach { signal ->
            Text(
                text = "- $signal",
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SentimentMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(label, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            color = TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FuturesInfoPanel(info: FuturesMarketInfo) {
    val fundingPercent = info.lastFundingRate * 100.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FuturesMetric(
                label = "Mark",
                value = "$${info.markPrice.formatPrice()}",
                modifier = Modifier.weight(1f),
            )
            FuturesMetric(
                label = "Index",
                value = "$${info.indexPrice.formatPrice()}",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FuturesMetric(
                label = "Funding",
                value = "${"%.4f".format(fundingPercent)}%",
                valueColor = if (fundingPercent >= 0) GreenUp else RedDown,
                modifier = Modifier.weight(1f),
            )
            FuturesMetric(
                label = "Open Interest",
                value = info.openInterest.formatLargeNumber(),
                modifier = Modifier.weight(1f),
            )
        }
        if (info.nextFundingTime > 0L) {
            Spacer(Modifier.height(8.dp))
            val formatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
            Text(
                text = "Next funding: ${formatter.format(Date(info.nextFundingTime))}",
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun FuturesMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ChartNotice(message: String) {
    Text(
        text = message,
        color = BinanceYellow,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BinanceYellow.copy(alpha = 0.08f))
            .border(1.dp, BinanceYellow.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun AlertQuickActionsRow(
    symbol: String,
    onSetAlert: (AlertType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Create $symbol alert",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AlertQuickActionButton(
                label = "Price",
                icon = Icons.Default.AttachMoney,
                onClick = { onSetAlert(AlertType.PRICE_LIMIT) },
                modifier = Modifier.weight(1f),
            )
            AlertQuickActionButton(
                label = "Volume",
                icon = Icons.Default.BarChart,
                onClick = { onSetAlert(AlertType.VOLUME) },
                modifier = Modifier.weight(1f),
            )
            AlertQuickActionButton(
                label = "M.Cap",
                icon = Icons.Default.PieChart,
                onClick = { onSetAlert(AlertType.MARKET_CAP) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AlertQuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.95f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceVariant.copy(alpha = 0.55f),
            contentColor = BinanceYellow,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun CoinDetailTopBar(
    coin: Coin,
    isWatchlisted: Boolean,
    onBack: () -> Unit,
    onWatchlistToggle: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint     = TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }

        Row(
            modifier              = Modifier.weight(1f),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AsyncImage(
                model              = coin.imageUrl,
                contentDescription = coin.name,
                modifier           = Modifier.size(24.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text       = coin.symbol,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
            )
            Text(
                text  = "/USDT",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        // Star button — only here in detail screen
        IconButton(onClick = onWatchlistToggle) {
            Icon(
                imageVector        = if (isWatchlisted) Icons.Default.Star
                                     else Icons.Default.StarBorder,
                contentDescription = if (isWatchlisted) "Remove from watchlist"
                                     else "Add to watchlist",
                tint               = if (isWatchlisted) BinanceYellow else TextSecondary,
            )
        }
    }
}

// ── Price Header ──────────────────────────────────────────────────────────────

@Composable
private fun PriceHeader(
    displayPrice: Double,
    displayPct: Double,
    isPositive: Boolean,
    isLive: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        if (isLive) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GreenUp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "LIVE",
                    color      = GreenUp,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        AnimatedContent(
            targetState    = displayPrice,
            label          = "detail_price",
            transitionSpec = {
                (fadeIn() + slideInVertically { -it / 4 }) togetherWith
                (fadeOut() + slideOutVertically { it / 4 })
            },
        ) { price ->
            Text(
                text       = "$${price.formatPrice()}",
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                color      = if (isPositive) GreenUp else RedDown,
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isPositive) GreenUpSurface else RedDownSurface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowDropUp
                                      else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint     = if (isPositive) GreenUp else RedDown,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text       = "${"%.2f".format(kotlin.math.abs(displayPct))}%",
                        color      = if (isPositive) GreenUp else RedDown,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                "24h Change",
                color    = TextSecondary,
                style    = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

// ── Interval Selector ─────────────────────────────────────────────────────────

@Composable
private fun IntervalSelector(
    selected: ChartInterval,
    onSelect: (ChartInterval) -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        ChartInterval.entries.forEach { interval ->
            val isSelected = selected == interval
            Box(
                modifier         = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) SurfaceVariant else Color.Transparent)
                    .clickable { onSelect(interval) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = interval.label,
                    color      = if (isSelected) BinanceYellow else TextSecondary,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ── Quick Stats ───────────────────────────────────────────────────────────────

@Composable
private fun QuickStatsRow(coin: Coin) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatItem("24h High", "$${coin.high24h.formatPrice()}", GreenUp)
        VerticalDivider(modifier = Modifier.height(36.dp), color = DividerColor)
        StatItem("24h Low",  "$${coin.low24h.formatPrice()}",  RedDown)
        VerticalDivider(modifier = Modifier.height(36.dp), color = DividerColor)
        StatItem("Volume",   coin.volume24h.formatLargeNumber(), TextPrimary)
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = color,
        )
    }
}

// ── Market Info ───────────────────────────────────────────────────────────────

@Composable
private fun YourHoldingsSection(
    asset: PortfolioAsset,
    totalPortfolioValue: Double,
    currentPrice: Double,
    onOpenPortfolio: () -> Unit,
) {
    val pricedAsset = asset.copy(currentPrice = currentPrice)
    val allocation = if (totalPortfolioValue > 0.0) {
        (pricedAsset.currentValue / totalPortfolioValue) * 100.0
    } else {
        pricedAsset.allocationPercent
    }
    val profitColor = if (pricedAsset.isProfit) GreenUp else RedDown

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Your Holdings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenPortfolio, contentPadding = PaddingValues(0.dp)) {
                Text("Portfolio", color = BinanceYellow, style = MaterialTheme.typography.labelLarge)
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = BinanceYellow,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HoldingMetric(
                label = "Value",
                value = "$${pricedAsset.currentValue.formatLargeNumber()}",
                modifier = Modifier.weight(1f),
            )
            HoldingMetric(
                label = "Quantity",
                value = "${pricedAsset.quantity.formatLargeNumber()} ${pricedAsset.symbol}",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HoldingMetric(
                label = "Avg Buy",
                value = "$${pricedAsset.averageBuyPrice.formatPrice()}",
                modifier = Modifier.weight(1f),
            )
            HoldingMetric(
                label = "P&L",
                value = "${if (pricedAsset.profitLoss >= 0) "+" else "-"}$${kotlin.math.abs(pricedAsset.profitLoss).formatLargeNumber()} (${kotlin.math.abs(pricedAsset.profitLossPercent).formatPrice()}%)",
                valueColor = profitColor,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        HoldingMetric(
            label = "Portfolio Allocation",
            value = "${allocation.formatPrice()}%",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HoldingMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MarketInfoSection(coin: Coin, coinDetail: CoinDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            "Market Info",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
        )
        Spacer(Modifier.height(14.dp))

        val rows = buildList {
            add("Market Cap Rank" to coin.rank.formatRank())
            add("Market Cap" to coin.marketCap.formatUsdLarge())
            add("24h Volume" to coin.volume24h.formatUsdLarge())
            add("24h High" to coin.high24h.formatUsdPrice())
            add("24h Low" to coin.low24h.formatUsdPrice())
            add("All Time High" to "${coinDetail.allTimeHigh.formatUsdPrice()}${coinDetail.allTimeHighDate.formatDateSuffix()}")
            add("All Time Low" to "${coinDetail.allTimeLow.formatUsdPrice()}${coinDetail.allTimeLowDate.formatDateSuffix()}")
            add("Circulating Supply" to coin.circulatingSupply.formatSupply(coin.symbol))
            add("Total Supply" to coinDetail.totalSupply.formatSupply(coin.symbol))
            if (coinDetail.maxSupply > 0.0) {
                add("Max Supply" to coinDetail.maxSupply.formatSupply(coin.symbol))
            }
        }

        rows.forEachIndexed { idx, (label, value) ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    value,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color      = TextPrimary,
                )
            }
            if (idx < rows.lastIndex) {
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            }
        }
    }
}

// ── About ─────────────────────────────────────────────────────────────────────

private fun Int.formatRank(): String = if (this > 0) "#$this" else "N/A"

private fun Double.formatUsdPrice(): String = if (this > 0.0) "$${formatPrice()}" else "N/A"

private fun Double.formatUsdLarge(): String = if (this > 0.0) "$${formatLargeNumber()}" else "N/A"

private fun Double.formatSupply(symbol: String): String =
    if (this > 0.0) "${formatLargeNumber()} $symbol" else "N/A"

private fun String.formatDateSuffix(): String {
    if (isBlank()) return ""
    val parsed = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(this)
    }.getOrNull()
    val label = if (parsed != null) {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(parsed)
    } else {
        take(10)
    }
    return " ($label)"
}

@Composable
private fun AboutSection(description: String) {
    var expanded by remember { mutableStateOf(false) }
    val cleanDesc = description
        .replace(Regex("<[^>]+>"), "")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            "About",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text     = cleanDesc,
            style    = MaterialTheme.typography.bodyMedium,
            color    = TextSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
        )
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick        = { expanded = !expanded },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text  = if (expanded) "Show less" else "Read more",
                color = BinanceYellow,
                style = MaterialTheme.typography.labelMedium,
            )
            Icon(
                imageVector        = if (expanded) Icons.Default.ExpandLess
                                     else Icons.Default.ExpandMore,
                contentDescription = null,
                tint               = BinanceYellow,
                modifier           = Modifier.size(16.dp),
            )
        }
    }
}

// ── Buy/Sell Bar ──────────────────────────────────────────────────────────────

@Composable
private fun BuySellBar(symbol: String, onOpenPortfolio: () -> Unit) {
    Surface(
        color           = BackgroundDark,
        tonalElevation  = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick  = onOpenPortfolio,
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = GreenUp,
                    contentColor   = Color.White,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Track Buy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick  = onOpenPortfolio,
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(1.dp, RedDown.copy(alpha = 0.72f)),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceDark,
                    contentColor = RedDown,
                ),
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Track Sell", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ── Loading / Error ───────────────────────────────────────────────────────────

@Composable
private fun CoinNewsSection(
    symbol: String,
    news: List<NewsItem>,
    isLoading: Boolean,
    error: String?,
    onSeeAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$symbol News",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSeeAll, contentPadding = PaddingValues(0.dp)) {
                Text("More", color = BinanceYellow, style = MaterialTheme.typography.labelLarge)
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = BinanceYellow,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = BinanceYellow,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            }
            news.isNotEmpty() -> news.take(3).forEachIndexed { index, item ->
                NewsRow(item = item, compact = true)
                if (index < news.take(3).lastIndex) {
                    HorizontalDivider(
                        color = DividerColor,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 64.dp),
                    )
                }
            }
            error != null -> Text(
                text = error,
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            else -> Text(
                text = "No related news yet.",
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CoinDetailLoading(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(BackgroundDark)) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(SurfaceElevated)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint     = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BinanceYellow)
        }
    }
}

@Composable
private fun CoinDetailError(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(BackgroundDark)) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(SurfaceElevated)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint     = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.SignalWifiOff,
                contentDescription = null,
                tint     = TextSecondary,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.bodyMedium,
                modifier  = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = BinanceYellow,
                    contentColor   = Color(0xFF1A1400),
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Try Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}
