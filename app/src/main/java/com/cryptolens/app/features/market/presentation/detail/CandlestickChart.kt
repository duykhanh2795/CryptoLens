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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
    markPriceKlines: List<Kline> = emptyList(),
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
    val selectedMarkPrice = selectedIndex?.let { markPriceKlines.getOrNull(it)?.close }

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
                    if (selectedMarkPrice != null) {
                        CandleInfoChip("M", selectedMarkPrice, BinanceYellow)
                    }
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
                markPriceKlines = markPriceKlines,
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
fun LinePriceChart(
    klines: List<Kline>,
    markPriceKlines: List<Kline> = emptyList(),
    modifier: Modifier = Modifier,
) {
    if (klines.isEmpty()) {
        Box(modifier.background(SurfaceDark), contentAlignment = Alignment.Center) {
            Text("No chart data", color = TextTertiary, fontSize = 13.sp)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val selected = selectedIndex?.let { klines.getOrNull(it) }
    val selectedMarkPrice = selectedIndex?.let { markPriceKlines.getOrNull(it)?.close }

    Column(modifier = modifier.background(SurfaceDark)) {
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
                    CandleInfoChip("Price", selected.close, if (selected.close >= selected.open) GreenUp else RedDown)
                    if (selectedMarkPrice != null) {
                        CandleInfoChip("Mark", selectedMarkPrice, BinanceYellow)
                    }
                    Text(
                        text = dateFormat.format(Date(selected.openTime)),
                        color = TextTertiary,
                        fontSize = 11.sp,
                    )
                }
            } else {
                Text("Tap line for details", color = TextTertiary, fontSize = 11.sp)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(klines) {
                    detectTapGestures { offset ->
                        val step = if (klines.size <= 1) size.width.toFloat() else size.width.toFloat() / (klines.size - 1)
                        val idx = (offset.x / step).toInt().coerceIn(0, klines.lastIndex)
                        selectedIndex = if (selectedIndex == idx) null else idx
                    }
                }
                .pointerInput(klines) {
                    detectHorizontalDragGestures { change, _ ->
                        val step = if (klines.size <= 1) size.width.toFloat() else size.width.toFloat() / (klines.size - 1)
                        selectedIndex = (change.position.x / step).toInt().coerceIn(0, klines.lastIndex)
                    }
                },
        ) {
            drawLinePriceChart(
                klines = klines,
                markPriceKlines = markPriceKlines,
                selectedIndex = selectedIndex,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
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
    markPriceKlines: List<Kline>,
    selectedIndex: Int?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    if (klines.isEmpty()) return

    val paddingH  = 8.dp.toPx()
    val paddingV  = 24.dp.toPx()
    val chartW    = size.width - paddingH * 2
    val chartH    = size.height - paddingV * 2

    val markCloses = markPriceKlines.map { it.close }.filter { it > 0.0 }
    val highMax = maxOf(klines.maxOf { it.high }, markCloses.maxOrNull() ?: 0.0)
    val lowMin  = minOf(klines.minOf { it.low }, markCloses.minOrNull() ?: Double.MAX_VALUE)
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

    if (markPriceKlines.size >= 2) {
        val markPath = Path()
        markPriceKlines.take(klines.size).forEachIndexed { index, candle ->
            val x = paddingH + index * candleW + candleW / 2
            val y = priceToY(candle.close)
            if (index == 0) {
                markPath.moveTo(x, y)
            } else {
                markPath.lineTo(x, y)
            }
        }
        drawPath(
            path = markPath,
            color = BinanceYellow.copy(alpha = 0.9f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            ),
        )
    }
}

private fun DrawScope.drawLinePriceChart(
    klines: List<Kline>,
    markPriceKlines: List<Kline>,
    selectedIndex: Int?,
) {
    if (klines.isEmpty()) return

    val paddingH = 12.dp.toPx()
    val paddingV = 24.dp.toPx()
    val chartW = size.width - paddingH * 2
    val chartH = size.height - paddingV * 2
    val prices = klines.map { it.close }.filter { it > 0.0 }
    val markPrices = markPriceKlines.map { it.close }.filter { it > 0.0 }
    val highMax = maxOf(prices.maxOrNull() ?: 0.0, markPrices.maxOrNull() ?: 0.0)
    val lowMin = minOf(prices.minOrNull() ?: 0.0, markPrices.minOrNull() ?: Double.MAX_VALUE)
    val range = (highMax - lowMin).coerceAtLeast(0.000001)

    fun xFor(index: Int): Float {
        if (klines.size <= 1) return size.width / 2f
        return paddingH + (index.toFloat() / klines.lastIndex.toFloat()) * chartW
    }

    fun yFor(price: Double): Float =
        (paddingV + (1.0 - (price - lowMin) / range) * chartH).toFloat()

    repeat(3) { line ->
        val y = paddingV + chartH * line / 2f
        drawLine(
            color = DividerColor,
            start = Offset(paddingH, y),
            end = Offset(size.width - paddingH, y),
            strokeWidth = 0.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
        )
    }

    val isUp = klines.last().close >= klines.first().close
    val lineColor = if (isUp) GreenUp else RedDown
    val path = Path()
    klines.forEachIndexed { index, candle ->
        val x = xFor(index)
        val y = yFor(candle.close)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = lineColor, style = Stroke(width = 2.2.dp.toPx()))

    if (markPriceKlines.size >= 2) {
        val markPath = Path()
        markPriceKlines.take(klines.size).forEachIndexed { index, candle ->
            val x = xFor(index)
            val y = yFor(candle.close)
            if (index == 0) markPath.moveTo(x, y) else markPath.lineTo(x, y)
        }
        drawPath(
            path = markPath,
            color = BinanceYellow.copy(alpha = 0.9f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            ),
        )
    }

    selectedIndex?.let { index ->
        val candle = klines.getOrNull(index) ?: return@let
        val x = xFor(index)
        val y = yFor(candle.close)
        drawLine(
            color = TextTertiary.copy(alpha = 0.5f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
        )
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
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
