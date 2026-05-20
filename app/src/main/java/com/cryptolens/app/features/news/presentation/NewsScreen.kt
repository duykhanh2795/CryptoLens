package com.cryptolens.app.features.news.presentation

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.features.news.domain.model.NewsFilter
import com.cryptolens.app.features.news.domain.model.NewsItem
import com.cryptolens.app.features.news.domain.model.NewsSentiment
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

@Composable
fun NewsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title = uiState.currencySymbol?.let { "$it News" } ?: "Crypto News"

    Scaffold(containerColor = BackgroundDark) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundDark),
        ) {
            NewsTopBar(
                title = title,
                onBack = onNavigateBack,
                onRefresh = { viewModel.onEvent(NewsEvent.Refresh) },
            )

            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onEvent(NewsEvent.QueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(52.dp),
                singleLine = true,
                placeholder = { Text("Search news") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(NewsFilter.entries, key = { it.name }) { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.onEvent(NewsEvent.FilterChanged(filter)) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BinanceYellow.copy(alpha = 0.16f),
                            selectedLabelColor = BinanceYellow,
                            labelColor = TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.filter == filter,
                            borderColor = BorderColor,
                            selectedBorderColor = BinanceYellow.copy(alpha = 0.6f),
                        ),
                    )
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BinanceYellow)
                }
                uiState.error != null -> NewsError(
                    message = uiState.error!!,
                    onRetry = { viewModel.onEvent(NewsEvent.Refresh) },
                )
                uiState.filteredNews.isEmpty() -> EmptyNews()
                else -> NewsList(news = uiState.filteredNews)
            }
        }
    }
}

@Composable
private fun NewsTopBar(
    title: String,
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
        Text(
            title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh news", tint = TextSecondary)
        }
    }
}

@Composable
fun NewsPreviewSection(
    title: String = "News Feed",
    onSeeAll: () -> Unit,
    viewModel: NewsPreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "See All",
                color = BinanceYellow,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable(onClick = onSeeAll),
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BinanceYellow, modifier = Modifier.size(16.dp))
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BinanceYellow, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
            uiState.news.isEmpty() -> Text(
                uiState.error ?: "No news available yet.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            else -> Column {
                uiState.news.take(5).forEachIndexed { index, item ->
                    NewsRow(item = item, compact = true)
                    if (index < uiState.news.take(5).lastIndex) {
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsList(news: List<NewsItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(news, key = { it.id }) { item ->
            NewsRow(item = item, compact = false)
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 70.dp))
        }
    }
}

@Composable
fun NewsRow(
    item: NewsItem,
    compact: Boolean,
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(item.url) }
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 36.dp else 42.dp)
                .background(SurfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Newspaper, contentDescription = null, tint = BinanceYellow, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = TextPrimary,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.sourceTitle.ifBlank { item.sourceDomain },
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text("  •  ${item.publishedAt.relativeTime()}", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
            }
            if (!compact && item.currencies.isNotEmpty()) {
                Text(
                    item.currencies.take(4).joinToString("  "),
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.size(10.dp))
        SentimentBadge(item.sentiment)
    }
}

@Composable
private fun SentimentBadge(sentiment: NewsSentiment) {
    val color = when (sentiment) {
        NewsSentiment.BULLISH -> GreenUp
        NewsSentiment.BEARISH -> RedDown
        NewsSentiment.IMPORTANT -> BinanceYellow
        NewsSentiment.NEUTRAL -> TextSecondary
    }
    val surface = when (sentiment) {
        NewsSentiment.BULLISH -> GreenUpSurface
        NewsSentiment.BEARISH -> RedDownSurface
        NewsSentiment.IMPORTANT -> BinanceYellow.copy(alpha = 0.14f)
        NewsSentiment.NEUTRAL -> SurfaceVariant
    }
    Surface(
        color = surface,
        shape = RoundedCornerShape(99.dp),
    ) {
        Text(
            sentiment.label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun NewsError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Color(0xFF1A1400)),
        ) {
            Text("Retry", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyNews() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No news matches this filter.", color = TextSecondary, textAlign = TextAlign.Center)
    }
}

private fun Long.relativeTime(): String =
    DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
