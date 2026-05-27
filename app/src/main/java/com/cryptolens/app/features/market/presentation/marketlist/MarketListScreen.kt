package com.cryptolens.app.features.market.presentation.marketlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.cryptolens.app.ui.theme.*

@Composable
fun MarketListScreen(
    onCoinClick: (String) -> Unit,
    onNavigateToWatchlist: () -> Unit = {},
    viewModel: MarketListViewModel = hiltViewModel(),
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredCoins by viewModel.filteredCoins.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ── Top Bar ────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(BackgroundDark)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Markets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = "Live crypto prices",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNavigateToWatchlist) {
                Icon(
                    Icons.Outlined.StarOutline,
                    contentDescription = "Watchlist",
                    tint = BinanceYellow,
                )
            }
            IconButton(onClick = { viewModel.onEvent(MarketListEvent.Refresh) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
            }
        }

        // ── Search Bar ─────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint     = TextTertiary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value         = uiState.searchQuery,
                onValueChange = { viewModel.onEvent(MarketListEvent.SearchQueryChanged(it)) },
                modifier      = Modifier.weight(1f),
                textStyle     = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush   = SolidColor(AppAccent),
                singleLine    = true,
                decorationBox = { innerTextField ->
                    if (uiState.searchQuery.isEmpty()) {
                        Text(
                            "Search coins...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                        )
                    }
                    innerTextField()
                },
            )
            if (uiState.searchQuery.isNotEmpty()) {
                IconButton(
                    onClick  = { viewModel.onEvent(MarketListEvent.SearchQueryChanged("")) },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint     = TextTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // ── Tab Row ────────────────────────────────────────────────────────
        val tabs = listOf(
            MarketTab.ALL     to "All",
            MarketTab.GAINERS to "Gainers",
            MarketTab.LOSERS  to "Losers",
            MarketTab.NEW     to "New",
        )
        val selectedTabIndex = tabs.indexOfFirst { it.first == uiState.selectedTab }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor   = BackgroundDark,
            contentColor     = AppAccent,
            edgePadding      = 16.dp,
            indicator        = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier  = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height    = 2.dp,
                    color     = AppAccent,
                )
            },
            divider = {},
        ) {
            tabs.forEach { (tab, label) ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick  = { viewModel.onEvent(MarketListEvent.TabSelected(tab)) },
                    text     = {
                        Text(
                            text  = label,
                            color = if (uiState.selectedTab == tab) TextPrimary else TextSecondary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }

        // ── Column Header ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text("Name",       style = MaterialTheme.typography.labelSmall, color = TextTertiary,
                modifier = Modifier.weight(1f))
            Text("Last Price", style = MaterialTheme.typography.labelSmall, color = TextTertiary,
                modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text("24h Change", style = MaterialTheme.typography.labelSmall, color = TextTertiary,
                modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(2.dp))

        // ── Content ────────────────────────────────────────────────────────
        when {
            uiState.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BinanceYellow)
                }
            uiState.error != null ->
                MarketErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.onEvent(MarketListEvent.Refresh) },
                )
            else ->
                LazyColumn(
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(filteredCoins, key = { it.id }) { coin ->
                        CoinListItem(
                            coin          = coin,
                            isWatchlisted = coin.id in uiState.watchlistedIds,
                            onClick       = { onCoinClick(coin.id) },
                        )
                    }
                }
        }
    }
}

// ── Coin List Item — no star button, watchlist dot indicator only ──────────────

@Composable
private fun CoinListItem(
    coin: Coin,
    isWatchlisted: Boolean,
    onClick: () -> Unit,
) {
    val isPositive = coin.isPositive

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Coin icon + name
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(
                    model              = coin.imageUrl,
                    contentDescription = coin.name,
                    modifier           = Modifier.size(36.dp).clip(CircleShape),
                )
                // Subtle watchlist indicator dot — top-right of avatar
                if (isWatchlisted) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppAccent)
                            .align(Alignment.TopEnd),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text       = coin.symbol,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                )
                Text(
                    text     = coin.name,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = TextSecondary,
                    maxLines = 1,
                )
                Text(
                    text  = "Vol ${coin.volume24h.formatLargeNumber()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextTertiary,
                )
            }
        }

        // Price column
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            AnimatedContent(
                targetState    = coin.currentPrice,
                label          = "price_${coin.id}",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { price ->
                Text(
                text  = "$${price.formatPrice()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
            )
            }
            Text(
                text  = "${if (coin.priceChange24h >= 0) "+" else "-"}$${
                    kotlin.math.abs(coin.priceChange24h).formatPrice()
                }",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPositive) GreenUp else RedDown,
            )
        }

        Spacer(Modifier.width(8.dp))

        // 24h change badge
        Box(
            modifier         = Modifier
                .width(80.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (isPositive) GreenUpSurface else RedDownSurface)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector        = if (isPositive) Icons.Default.ArrowDropUp
                                        else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint               = if (isPositive) GreenUp else RedDown,
                    modifier           = Modifier.size(16.dp),
                )
                Text(
                    text  = "${"%.2f".format(kotlin.math.abs(coin.priceChangePercent24h))}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPositive) GreenUp else RedDown,
                )
            }
        }
    }
}

// ── Error State ───────────────────────────────────────────────────────────────

@Composable
private fun MarketErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.WifiOff, contentDescription = null,
            tint = TextSecondary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors  = ButtonDefaults.buttonColors(
                containerColor = AppAccent,
                contentColor   = BackgroundDark,
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("Retry", fontWeight = FontWeight.Bold)
        }
    }
}
