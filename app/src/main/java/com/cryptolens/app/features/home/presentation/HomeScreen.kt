package com.cryptolens.app.features.home.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.cryptolens.app.ui.theme.*

@Composable
fun HomeScreen(
    onCoinClick: (String) -> Unit,
    onSeeAllMarket: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark)
    ) {
        // ── Top Bar ────────────────────────────────────────────────────────
        HomeTopBar(
            userName = uiState.user?.displayName?.ifBlank { null },
            onRefresh = { viewModel.refresh() },
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // ── Portfolio Banner ───────────────────────────────────────────
            PortfolioBanner()

            // ── Quick Actions ──────────────────────────────────────────────
            QuickActionsRow()

            HorizontalDivider(color = DividerColor)

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

                    HorizontalDivider(color = DividerColor)

                    // Top Gainers
                    SectionHeader(title = "Top Gainers 🚀", onSeeAll = onSeeAllMarket)
                    uiState.topGainers.forEach { coin ->
                        CompactCoinRow(coin = coin, onCoinClick = { onCoinClick(coin.id) })
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }

                    HorizontalDivider(color = DividerColor)

                    // Top Losers
                    SectionHeader(title = "Top Losers 📉", onSeeAll = onSeeAllMarket)
                    uiState.topLosers.forEach { coin ->
                        CompactCoinRow(coin = coin, onCoinClick = { onCoinClick(coin.id) })
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                }
            }

            // ── AI Insight Banner ──────────────────────────────────────────
            AiInsightBanner()

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(userName: String?, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceElevated)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(BinanceYellow),
                contentAlignment = Alignment.Center,
            ) {
                Text("CL", color = Color(0xFF1A1400), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    if (userName != null) "Hello, $userName 👋" else "CryptoLens",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                if (userName != null) {
                    Text("Welcome back", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
    Column(
        modifier = Modifier.fillMaxWidth().background(SurfaceDark)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Portfolio Value", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Visibility, contentDescription = null,
                tint = TextTertiary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("$0.00", style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowDropUp, contentDescription = null,
                tint = GreenUp, modifier = Modifier.size(20.dp))
            Text("+$0.00 (0.00%)", style = MaterialTheme.typography.bodyMedium, color = GreenUp)
            Text(" Today", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
        }
    }
}

// ── Quick Actions ─────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow() {
    val actions = listOf(
        Icons.Default.AddCircleOutline    to "Deposit",
        Icons.Default.RemoveCircleOutline to "Withdraw",
        Icons.Default.SwapHoriz          to "Convert",
        Icons.Default.SendToMobile       to "Transfer",
    )
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceDark)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        actions.forEach { (icon, label) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {},
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(SurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = label, tint = BinanceYellow, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

// ── Section Headers ───────────────────────────────────────────────────────────

@Composable
private fun TrendingSectionHeader(onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Markets", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.weight(1f))
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
    LazyRow(
        contentPadding      = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier            = Modifier.padding(bottom = 12.dp),
    ) {
        items(coins, key = { it.id }) { coin ->
            TrendingCoinCard(coin = coin, onClick = { onCoinClick(coin.id) })
        }
    }
}

@Composable
private fun TrendingCoinCard(coin: Coin, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(0.5.dp, DividerColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model              = coin.imageUrl,
            contentDescription = coin.name,
            modifier           = Modifier.size(36.dp).clip(CircleShape),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = coin.symbol,
            style     = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color     = TextPrimary,
            maxLines  = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "$${coin.currentPrice.formatPrice()}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "${"%.2f".format(coin.priceChangePercent24h)}%",
            style = MaterialTheme.typography.labelSmall,
            color = if (coin.isPositive) GreenUp else RedDown,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Compact Coin Row ──────────────────────────────────────────────────────────

@Composable
private fun CompactCoinRow(coin: Coin, onCoinClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCoinClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            Text(
                text = "${if (coin.isPositive) "+" else ""}${"%.2f".format(coin.priceChangePercent24h)}%",
                style = MaterialTheme.typography.labelSmall,
                color = if (coin.isPositive) GreenUp else RedDown,
                fontWeight = FontWeight.SemiBold,
            )
        }
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
        modifier = Modifier.fillMaxWidth().padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .border(1.dp, BinanceYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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
