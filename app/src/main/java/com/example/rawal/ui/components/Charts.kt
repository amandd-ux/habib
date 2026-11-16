package com.example.rawal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rawal.domain.analytics.CategoryDurationStat
import com.example.rawal.domain.i18n.AppLanguage
import com.example.rawal.domain.i18n.AppStringsProvider
import kotlin.math.max

data class LineChartPoint(
    val label: String,
    val value: Float
)

@Composable
fun RawalLineChart(
    points: List<LineChartPoint>,
    lineColor: Color,
    maxValue: Float = 100f,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val topPad = 10f
                val bottomPad = 14f
                val chartH = (h - topPad - bottomPad).coerceAtLeast(10f)

                // Horizontal grid lines at 0%, 50%, 100%
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = topPad + chartH * (1f - frac)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.5f
                    )
                }

                val safeMax = max(1f, maxValue)
                val stepX = if (points.size > 1) w / (points.size - 1) else 0f
                val coords = points.mapIndexed { idx, p ->
                    val x = if (points.size > 1) idx * stepX else w / 2f
                    val y = topPad + chartH * (1f - (p.value / safeMax).coerceIn(0f, 1f))
                    Offset(x, y)
                }

                if (coords.size > 1) {
                    val fillPath = Path().apply {
                        moveTo(coords.first().x, topPad + chartH)
                        coords.forEach { lineTo(it.x, it.y) }
                        lineTo(coords.last().x, topPad + chartH)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = topPad,
                            endY = topPad + chartH
                        )
                    )

                    val linePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            val prev = coords[i - 1]
                            val cur = coords[i]
                            val midX = (prev.x + cur.x) / 2f
                            cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }

                coords.forEach { c ->
                    drawCircle(color = Color.White, radius = 7f, center = c)
                    drawCircle(color = lineColor, radius = 4.5f, center = c)
                }
            }
        }
        // X-axis labels (sampled so they never overlap)
        val displayLabels = if (points.size <= 7) {
            points
        } else {
            listOf(points.first(), points[points.size / 2], points.last())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            displayLabels.forEach { pt ->
                Text(
                    text = AppStringsProvider.toLocalizedDigits(pt.label, language),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        }
    }
}

data class BarChartEntry(
    val label: String,
    val primaryValue: Float,
    val secondaryValue: Float = 0f
)

@Composable
fun RawalBarChart(
    entries: List<BarChartEntry>,
    primaryColor: Color,
    secondaryColor: Color,
    primaryLegend: String,
    secondaryLegend: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxVal = entries.maxOfOrNull { max(it.primaryValue, it.secondaryValue) }?.coerceAtLeast(10f) ?: 10f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(primaryColor))
                Spacer(Modifier.width(6.dp))
                Text(primaryLegend, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(secondaryColor))
                Spacer(Modifier.width(6.dp))
                Text(secondaryLegend, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val slotW = w / entries.size.coerceAtLeast(1)
                val barW = (slotW * 0.28f).coerceIn(6f, 26f)

                drawLine(
                    color = gridColor,
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 2f
                )
                drawLine(
                    color = gridColor.copy(alpha = 0.5f),
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = 1f
                )

                entries.forEachIndexed { idx, item ->
                    val centerX = slotW * idx + slotW / 2f
                    val pH = ((item.primaryValue / maxVal) * (h - 10f)).coerceAtLeast(2f)
                    val sH = ((item.secondaryValue / maxVal) * (h - 10f)).coerceAtLeast(2f)

                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(centerX - barW - 2f, h - pH),
                        size = Size(barW, pH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(centerX + 2f, h - sH),
                        size = Size(barW, sH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }
        val labels = if (entries.size <= 7) entries else listOf(entries.first(), entries[entries.size / 2], entries.last())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach {
                Text(
                    text = AppStringsProvider.toLocalizedDigits(it.label, language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RawalDonutChart(
    items: List<CategoryDurationStat>,
    language: AppLanguage,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    val totalMinutes = items.sumOf { it.totalMinutes }
    if (totalMinutes <= 0) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(136.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                var startAngle = -90f
                val strokeWidth = 28f
                items.forEach { stat ->
                    val sweep = (stat.totalMinutes.toFloat() / totalMinutes.toFloat()) * 360f
                    drawArc(
                        color = Color(stat.colorHex),
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = AppStringsProvider.formatDuration(language, totalMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(5).forEach { stat ->
                val catName = if (language == AppLanguage.FA) stat.nameFa else stat.nameEn
                val pct = ((stat.totalMinutes.toFloat() / totalMinutes.toFloat()) * 100f).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(stat.colorHex))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = catName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${AppStringsProvider.formatDuration(language, stat.totalMinutes)} (${AppStringsProvider.toLocalizedDigits(pct.toString(), language)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
