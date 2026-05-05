package com.cryptolens.app.features.market.presentation.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CandlestickChart(
    klines: List<Kline>,
    modifier: Modifier = Modifier,
) {
    if (klines.isEmpty()) {
        Box(modifier.background(SurfaceDark), contentAlignment = Alignment.Center) {
            Text("No chart data", color = TextTertiary, fontSize = 13.sp)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val dateFormat   = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    // Selected candle info overlay
    val selected = selectedIndex?.let { klines.getOrNull(it) }

    Column(modifier = modifier.background(SurfaceDark)) {
        // Crosshair info bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(SurfaceVariant)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (selected != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CandleInfoChip("O", selected.open, Color(0xFF87CEEB))
                    CandleInfoChip("H", selected.high, GreenUp)
                    CandleInfoChip("L", selected.low,  RedDown)
                    CandleInfoChip("C", selected.close,
                        if (selected.close >= selected.open) GreenUp else RedDown)
                    Text(
                        text  = dateFormat.format(Date(selected.openTime)),
                        color = TextTertiary,
                        fontSize = 11.sp,
                    )
                }
            } else {
                Text("Tap candle for details", color = TextTertiary, fontSize = 11.sp)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(klines) {
                    detectTapGestures { offset ->
                        val candleWidth = size.width.toFloat() / klines.size
                        val idx = (offset.x / candleWidth).toInt().coerceIn(0, klines.lastIndex)
                        selectedIndex = if (selectedIndex == idx) null else idx
                    }
                }
                .pointerInput(klines) {
                    detectHorizontalDragGestures { change, _ ->
                        val candleWidth = size.width.toFloat() / klines.size
                        val idx = (change.position.x / candleWidth).toInt()
                            .coerceIn(0, klines.lastIndex)
                        selectedIndex = idx
                    }
                }
        ) {
            drawCandlesticks(
                klines        = klines,
                selectedIndex = selectedIndex,
                textMeasurer  = textMeasurer,
            )
        }

        // Volume bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            drawVolumeBars(klines = klines, selectedIndex = selectedIndex)
        }
    }
}

@Composable
private fun CandleInfoChip(label: String, value: Double, color: Color) {
    Row {
        Text(label, color = TextTertiary, fontSize = 11.sp)
        Text(" ${value.formatPrice()}", color = color, fontSize = 11.sp)
    }
}

private fun DrawScope.drawCandlesticks(
    klines: List<Kline>,
    selectedIndex: Int?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    if (klines.isEmpty()) return

    val paddingH  = 8.dp.toPx()
    val paddingV  = 24.dp.toPx()
    val chartW    = size.width - paddingH * 2
    val chartH    = size.height - paddingV * 2

    val highMax = klines.maxOf { it.high }
    val lowMin  = klines.minOf { it.low }
    val range   = (highMax - lowMin).coerceAtLeast(0.000001)

    fun priceToY(price: Double): Float =
        (paddingV + (1.0 - (price - lowMin) / range) * chartH).toFloat()

    val candleW     = chartW / klines.size
    val bodyW       = (candleW * 0.6f).coerceAtLeast(2f)
    val wickW       = 1.5f

    // Grid lines
    val gridPrices = listOf(highMax, (highMax + lowMin) / 2, lowMin)
    gridPrices.forEach { price ->
        val y = priceToY(price)
        drawLine(
            color       = DividerColor,
            start       = Offset(paddingH, y),
            end         = Offset(size.width - paddingH, y),
            strokeWidth = 0.5f,
            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
        )
        val measured = textMeasurer.measure(
            text  = "$${price.formatPrice()}",
            style = TextStyle(color = TextTertiary, fontSize = 10.sp),
        )
        drawText(measured, topLeft = Offset(size.width - measured.size.width - 4f, y - measured.size.height / 2))
    }

    klines.forEachIndexed { i, candle ->
        val x       = paddingH + i * candleW + candleW / 2
        val isGreen = candle.close >= candle.open
        val color   = if (isGreen) GreenUp else RedDown
        val isSelected = selectedIndex == i

        val openY  = priceToY(candle.open)
        val closeY = priceToY(candle.close)
        val highY  = priceToY(candle.high)
        val lowY   = priceToY(candle.low)

        val bodyTop    = minOf(openY, closeY)
        val bodyBottom = maxOf(openY, closeY)
        val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(1f)

        // Selection highlight
        if (isSelected) {
            drawRect(
                color    = Color.White.copy(alpha = 0.05f),
                topLeft  = Offset(x - candleW / 2, 0f),
                size     = androidx.compose.ui.geometry.Size(candleW, size.height),
            )
            drawLine(
                color       = TextTertiary.copy(alpha = 0.5f),
                start       = Offset(x, 0f),
                end         = Offset(x, size.height),
                strokeWidth = 0.5f,
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
            )
        }

        // Wick
        drawLine(color = color, start = Offset(x, highY), end = Offset(x, lowY), strokeWidth = wickW)

        // Body
        drawRect(
            color    = color.copy(alpha = if (isGreen) 0.9f else 0.85f),
            topLeft  = Offset(x - bodyW / 2, bodyTop),
            size     = androidx.compose.ui.geometry.Size(bodyW, bodyHeight),
        )
    }
}

private fun DrawScope.drawVolumeBars(klines: List<Kline>, selectedIndex: Int?) {
    if (klines.isEmpty()) return
    val maxVol  = klines.maxOf { it.volume }.coerceAtLeast(0.000001)
    val candleW = size.width / klines.size
    val barW    = candleW * 0.6f

    klines.forEachIndexed { i, candle ->
        val x       = i * candleW + candleW / 2
        val isGreen = candle.close >= candle.open
        val barH    = (candle.volume / maxVol * size.height * 0.85f).toFloat()
        val alpha   = if (selectedIndex == i) 0.9f else 0.5f
        drawRect(
            color    = (if (isGreen) GreenUp else RedDown).copy(alpha = alpha),
            topLeft  = Offset(x - barW / 2, size.height - barH),
            size     = androidx.compose.ui.geometry.Size(barW, barH),
        )
    }
}
