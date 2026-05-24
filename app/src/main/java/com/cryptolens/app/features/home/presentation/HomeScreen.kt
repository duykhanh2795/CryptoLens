package com.cryptolens.app.features.home.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.SendToMobile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptolens.app.core.domain.util.formatLargeNumber
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.news.presentation.NewsPreviewSection
import com.cryptolens.app.features.wallet.domain.model.TrendingWallet
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.presentation.TrendingWalletRow
import com.cryptolens.app.ui.theme.*

@Composable
fun HomeScreen(
    onCoinClick: (String) -> Unit,
    onSeeAllMarket: () -> Unit,
    onWalletClick: (WalletChain, String) -> Unit,
    onSeeAllWallets: () -> Unit,
    onSeeAllNews: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
    ) {
        // ── Top Bar ────────────────────────────────────────────────────────
        HomeTopBar(
            userName = uiState.user?.displayName?.ifBlank { null },
            onRefresh = { viewModel.refresh() },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Portfolio Banner ───────────────────────────────────────────
            PortfolioBanner()

            // ── Quick Actions ──────────────────────────────────────────────
            QuickActionsRow()

            // ── Market Overview ────────────────────────────────────────────
            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BinanceYellow, strokeWidth = 2.dp)
                }
                uiState.error != null -> ErrorBanner(
                    message = uiState.error!!,
                    onRetry = { viewModel.refresh() },
                )
                else -> {
                    // Trending horizontal scroll
                    TrendingSectionHeader(onSeeAll = onSeeAllMarket)
                    TrendingRow(coins = uiState.trendingCoins, onCoinClick = onCoinClick)

                    MarketMoveSection(title = "Top Gainers", coins = uiState.topGainers, onSeeAll = onSeeAllMarket) {
                        onCoinClick(it)
                    }

                    MarketMoveSection(title = "Top Losers", coins = uiState.topLosers, onSeeAll = onSeeAllMarket) {
                        onCoinClick(it)
                    }

                    if (uiState.trendingWallets.isNotEmpty()) {
                        TrendingWalletsHomeSection(
                            wallets = uiState.trendingWallets,
                            onSeeAll = onSeeAllWallets,
                            onWalletClick = onWalletClick,
                        )
                    }
                }
            }

            NewsPreviewSection(onSeeAll = onSeeAllNews)

            // ── AI Insight Banner ──────────────────────────────────────────
            AiInsightBanner()

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun TrendingWalletsHomeSection(
    wallets: List<TrendingWallet>,
    onSeeAll: () -> Unit,
    onWalletClick: (WalletChain, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(title = "Trending Addresses", onSeeAll = onSeeAll)
        wallets.forEach { wallet ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                color = SurfaceVariant,
                shape = RoundedCornerShape(10.dp),
            ) {
                TrendingWalletRow(
                    wallet = wallet,
                    compact = true,
                    onClick = { onWalletClick(wallet.chain, wallet.address) },
                )
            }
        }
    }
}

@Composable
private fun MarketMoveSection(
    title: String,
    coins: List<Coin>,
    onSeeAll: () -> Unit,
    onCoinClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(bottom = 10.dp),
    ) {
        SectionHeader(title = title, onSeeAll = onSeeAll)
        coins.forEachIndexed { index, coin ->
            CompactCoinRow(
                coin = coin,
                onCoinClick = { onCoinClick(coin.id) },
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            if (index != coins.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HomeTopBar(userName: String?, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("CL", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    if (userName != null) "Hello, $userName" else "CryptoLens",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                if (userName != null) {
                    Text("Market dashboard", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = TextPrimary)
        }
    }
}

// ── Portfolio Banner ──────────────────────────────────────────────────────────

@Composable
private fun PortfolioBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1A1A1C),
                        Color(0xFF0E0E10),
                        Color(0xFF2A2A2D),
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Portfolio Value", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        BalanceChip("USD")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$0.00",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text("+$0.00  0.00%", style = MaterialTheme.typography.labelMedium, color = GreenUp)
                }
                Spacer(Modifier.width(8.dp))
                Text("Today", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PortfolioMiniMetric("Invested", "$0.00", Icons.Default.AccountBalanceWallet, Modifier.weight(1f))
                PortfolioMiniMetric("Assets", "0", Icons.Default.PieChart, Modifier.weight(1f))
                PortfolioMiniMetric("Alerts", "0", Icons.Default.NotificationsNone, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BalanceChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(99.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
private fun PortfolioMiniMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ── Quick Actions ─────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow() {
    val actions = listOf(
        Icons.Default.AddCircleOutline    to "Deposit",
        Icons.Default.RemoveCircleOutline to "Withdraw",
        Icons.Default.SwapHoriz          to "Convert",
        Icons.AutoMirrored.Filled.SendToMobile to "Transfer",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { (icon, label) ->
            ActionTile(
                icon = icon,
                label = label,
                selected = label == "Convert",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White.copy(alpha = 0.10f) else SurfaceVariant)
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.12f) else BorderColor,
                RoundedCornerShape(12.dp),
            )
            .clickable {}
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) Color.White.copy(alpha = 0.12f) else SurfaceDark),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = if (selected) TextPrimary else TextSecondary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Section Headers ───────────────────────────────────────────────────────────

@Composable
private fun TrendingSectionHeader(onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Market Pulse", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Live prices and momentum", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        TextButton(onClick = onSeeAll, contentPadding = PaddingValues(0.dp)) {
            Text("See All", color = BinanceYellow, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = BinanceYellow, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSeeAll, contentPadding = PaddingValues(0.dp)) {
            Text("More", color = BinanceYellow, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = BinanceYellow, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Trending Horizontal Scroll ────────────────────────────────────────────────

@Composable
private fun TrendingRow(coins: List<Coin>, onCoinClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(coins, key = { it.id }) { coin ->
                TrendingCoinCard(coin = coin, onClick = { onCoinClick(coin.id) })
            }
        }
    }
}

@Composable
private fun TrendingCoinCard(coin: Coin, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coin.imageUrl,
                contentDescription = coin.name,
                modifier = Modifier.size(34.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = coin.symbol,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                )
                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text  = "$${coin.currentPrice.formatPrice()}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        PercentPill(
            text = "${if (coin.isPositive) "+" else ""}${"%.2f".format(coin.priceChangePercent24h)}%",
            positive = coin.isPositive,
        )
    }
}

// ── Compact Coin Row ──────────────────────────────────────────────────────────

@Composable
private fun CompactCoinRow(coin: Coin, onCoinClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onCoinClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = coin.imageUrl, contentDescription = coin.name,
            modifier = Modifier.size(36.dp).clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(coin.symbol, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(coin.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$${coin.currentPrice.formatPrice()}", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium, color = TextPrimary)
            PercentPill(
                text = "${if (coin.isPositive) "+" else ""}${"%.2f".format(coin.priceChangePercent24h)}%",
                positive = coin.isPositive,
            )
        }
    }
}

@Composable
private fun PercentPill(text: String, positive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background((if (positive) GreenUp else RedDown).copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (positive) GreenUp else RedDown,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Error Banner ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.WifiOff, contentDescription = null,
            tint = TextSecondary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onRetry,
            border  = BorderStroke(1.dp, BinanceYellow),
            colors  = ButtonDefaults.outlinedButtonColors(contentColor = BinanceYellow),
            shape   = RoundedCornerShape(8.dp),
        ) { Text("Retry") }
    }
}

// ── AI Insight Banner ─────────────────────────────────────────────────────────

@Composable
private fun AiInsightBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable {}
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(BinanceYellow.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = BinanceYellow, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("AI Market Insight", style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Get AI-powered Buy/Sell signals in Vietnamese",
                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BinanceYellow)
        }
    }
}
