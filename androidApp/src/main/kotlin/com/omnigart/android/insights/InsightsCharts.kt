package com.omnigart.android.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.omnigart.shared.insights.ChartPoint
import com.omnigart.shared.insights.HeatmapCell
import kotlin.math.max

@Composable
fun DarkInsightCard(title: String, sourceName: String? = null, content: @Composable () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(InsightsChartPalette.card, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = InsightsChartPalette.title, style = MaterialTheme.typography.titleMedium)
            if (!sourceName.isNullOrBlank()) {
                Text(sourceName, color = InsightsChartPalette.subtitle, style = MaterialTheme.typography.labelSmall)
            }
        }
        content()
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
fun CompareLineChart(
    current: List<ChartPoint>,
    comparison: List<ChartPoint>,
    nowFraction: Double,
    showBand: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier =
        modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val pts = current.ifEmpty { comparison }
        if (pts.isEmpty()) return@Canvas
        val maxX = max(pts.maxOf { it.dayOffset }.toFloat(), 1f)
        val values =
            buildList {
                addAll(current.map { it.value })
                addAll(comparison.map { it.value })
                if (showBand) {
                    current.forEach {
                        it.low?.let { v -> add(v) }
                        it.high?.let { v -> add(v) }
                    }
                }
            }
        val minY = (values.minOrNull() ?: 0.0).toFloat()
        val maxY = max((values.maxOrNull() ?: 1.0).toFloat(), minY + 1f)
        fun x(day: Int): Float = (day / maxX) * size.width
        fun y(v: Double): Float = size.height - ((v.toFloat() - minY) / (maxY - minY)) * size.height

        if (showBand && current.isNotEmpty()) {
            val band = Path()
            current.forEachIndexed { i, p ->
                val high = p.high ?: p.value
                val pt = Offset(x(p.dayOffset), y(high))
                if (i == 0) band.moveTo(pt.x, pt.y) else band.lineTo(pt.x, pt.y)
            }
            for (i in current.indices.reversed()) {
                val p = current[i]
                val low = p.low ?: p.value
                band.lineTo(x(p.dayOffset), y(low))
            }
            band.close()
            drawPath(band, InsightsChartPalette.band)
        }

        fun strokeSeries(points: List<ChartPoint>, color: androidx.compose.ui.graphics.Color, dashed: Boolean) {
            if (points.size < 2) return
            val path = Path()
            points.forEachIndexed { i, p ->
                val pt = Offset(x(p.dayOffset), y(p.value))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(
                path,
                color,
                style =
                Stroke(
                    width = if (dashed) 2.5f else 4f,
                    cap = StrokeCap.Round,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
                )
            )
        }
        strokeSeries(comparison, InsightsChartPalette.comparison, dashed = true)
        strokeSeries(current, InsightsChartPalette.current, dashed = false)

        val nowX = (nowFraction.coerceIn(0.0, 1.0).toFloat()) * size.width
        drawLine(
            color = InsightsChartPalette.now,
            start = Offset(nowX, 0f),
            end = Offset(nowX, size.height),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    }
}

@Composable
fun HeatmapGrid(cells: List<HeatmapCell>) {
    val rows = cells.chunked(12)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    Box(
                        modifier =
                        Modifier
                            .weight(1f)
                            .height(12.dp)
                            .background(
                                InsightsChartPalette.current.copy(
                                    alpha = (0.12f + cell.intensity.toFloat() * 0.88f).coerceIn(0.12f, 1f)
                                ),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
                repeat(12 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
