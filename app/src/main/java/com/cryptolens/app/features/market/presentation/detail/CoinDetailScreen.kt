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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptolens.app.core.domain.util.formatLargeNumber
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.CoinDetail
import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.ui.theme.*

// ── Entry Point ───────────────────────────────────────────────────────────────

@Composable
fun CoinDetailScreen(
    onNavigateBack: () -> Unit,
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
                    selectedInterval  = uiState.selectedInterval,
                    isKlinesLoading   = uiState.isKlinesLoading,
                    isWatchlisted     = uiState.isWatchlisted,
                    onBack            = onNavigateBack,
                    onIntervalChange  = { viewModel.onEvent(CoinDetailEvent.IntervalChanged(it)) },
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
    selectedInterval: ChartInterval,
    isKlinesLoading: Boolean,
    isWatchlisted: Boolean,
    onBack: () -> Unit,
    onIntervalChange: (ChartInterval) -> Unit,
    onWatchlistToggle: () -> Unit,
) {
    val coin         = coinDetail.coin
    val displayPrice = liveTicker?.price ?: coin.currentPrice
    val displayPct   = liveTicker?.priceChangePercent ?: coin.priceChangePercent24h
    val isPositive   = displayPct >= 0

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        CoinDetailTopBar(
            coin              = coin,
            isWatchlisted     = isWatchlisted,
            onBack            = onBack,
            onWatchlistToggle = onWatchlistToggle,
        )

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            PriceHeader(
                displayPrice = displayPrice,
                displayPct   = displayPct,
                isPositive   = isPositive,
                isLive       = liveTicker != null,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(SurfaceDark),
            ) {
                if (isKlinesLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.align(Alignment.Center),
                        color       = BinanceYellow,
                        strokeWidth = 2.dp,
                    )
                } else {
                    CandlestickChart(
                        klines   = klines,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            IntervalSelector(selected = selectedInterval, onSelect = onIntervalChange)
            HorizontalDivider(color = DividerColor)
            QuickStatsRow(coin = coin)
            HorizontalDivider(color = DividerColor)
            MarketInfoSection(coin = coin, coinDetail = coinDetail)
            HorizontalDivider(color = DividerColor)

            if (coinDetail.description.isNotBlank()) {
                AboutSection(description = coinDetail.description)
                HorizontalDivider(color = DividerColor)
            }

            Spacer(Modifier.height(100.dp))
        }

        BuySellBar(symbol = coin.symbol)
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
            .background(SurfaceElevated)
            .padding(horizontal = 4.dp, vertical = 4.dp),
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

        IconButton(onClick = {}) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary)
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
            .background(SurfaceDark)
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
            .background(SurfaceDark)
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
            .background(SurfaceDark)
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
private fun MarketInfoSection(coin: Coin, coinDetail: CoinDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp),
    ) {
        Text(
            "Market Info",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
        )
        Spacer(Modifier.height(14.dp))

        val rows = listOf(
            "Market Cap Rank"    to "#${coin.rank}",
            "Market Cap"         to "$${coin.marketCap.formatLargeNumber()}",
            "24h Volume"         to "$${coin.volume24h.formatLargeNumber()}",
            "All Time High"      to "$${coinDetail.allTimeHigh.formatPrice()}",
            "All Time Low"       to "$${coinDetail.allTimeLow.formatPrice()}",
            "Circulating Supply" to "${coin.circulatingSupply.formatLargeNumber()} ${coin.symbol}",
        )

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
            .background(SurfaceDark)
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
private fun BuySellBar(symbol: String) {
    Surface(
        color           = SurfaceElevated,
        tonalElevation  = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick  = {},
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = GreenUp,
                    contentColor   = Color.White,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Buy $symbol", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick  = {},
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(8.dp),
                border   = BorderStroke(1.dp, RedDown),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedDown),
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sell $symbol", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ── Loading / Error ───────────────────────────────────────────────────────────

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
