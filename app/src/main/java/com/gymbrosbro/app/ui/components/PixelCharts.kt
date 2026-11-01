package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.domain.schedule.Schedule
import com.gymbrosbro.app.domain.stats.ChartPoint
import com.gymbrosbro.app.ui.theme.GbbColors
import com.gymbrosbro.app.ui.theme.PixelDimens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/** Snaps to the pixel grid so lines and dots stay chunky. */
private fun DrawScope.snap(v: Float): Float {
    val u = PixelDimens.unit.toPx()
    return (v / u).roundToInt() * u
}

/**
 * Line chart of values over time, drawn on the pixel grid. An optional
 * [secondary] series (e.g. e1RM behind top weight) shares the y axis.
 */
@Composable
fun PixelLineChart(
    points: List<ChartPoint>,
    color: Color,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
    secondary: List<ChartPoint> = emptyList(),
    secondaryColor: Color = GbbColors.Purple,
    height: Dp = 140.dp,
) {
    val all = points + secondary
    if (all.isEmpty()) return
    val minY = all.minOf { it.value }
    val maxY = all.maxOf { it.value }
    val minX = all.minOf { it.date.toEpochDay() }
    val maxX = all.maxOf { it.date.toEpochDay() }

    Column(modifier) {
        Text(formatValue(maxY), style = MaterialTheme.typography.labelSmall, color = GbbColors.Fog)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = "Chart from ${formatValue(minY)} to ${formatValue(maxY)}" },
        ) {
            val u = PixelDimens.unit.toPx()
            val dot = u * 3
            fun x(p: ChartPoint) = if (maxX == minX) size.width / 2 else
                dot / 2 + (p.date.toEpochDay() - minX).toFloat() / (maxX - minX) * (size.width - dot)
            fun y(p: ChartPoint) = if (maxY == minY) size.height / 2 else
                size.height - dot / 2 - ((p.value - minY) / (maxY - minY)).toFloat() * (size.height - dot)

            for (i in 0..4) {
                val gy = snap(size.height * i / 4f)
                drawRect(GbbColors.Dusk, Offset(0f, gy), Size(size.width, u / 2))
            }
            for (series in listOf(secondary to secondaryColor, points to color)) {
                val (list, c) = series
                val sorted = list.sortedBy { it.date }
                sorted.zipWithNext().forEach { (a, b) ->
                    drawLine(c, Offset(x(a), y(a)), Offset(x(b), y(b)), strokeWidth = u)
                }
                sorted.forEach { p ->
                    drawRect(c, Offset(snap(x(p) - dot / 2), snap(y(p) - dot / 2)), Size(dot, dot))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatValue(minY), style = MaterialTheme.typography.labelSmall, color = GbbColors.Fog)
            Text(
                listOf(LocalDate.ofEpochDay(minX), LocalDate.ofEpochDay(maxX)).distinct().joinToString(" – ") { it.format(shortDate) },
                style = MaterialTheme.typography.labelSmall,
                color = GbbColors.Fog,
            )
        }
    }
}

/** Bars over time, oldest left, e.g. weekly volume. */
@Composable
fun PixelBarChart(
    points: List<ChartPoint>,
    color: Color,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
) {
    if (points.isEmpty()) return
    val maxY = points.maxOf { it.value }
    Column(modifier) {
        Text(formatValue(maxY), style = MaterialTheme.typography.labelSmall, color = GbbColors.Fog)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = "Bar chart, highest ${formatValue(maxY)}" },
        ) {
            val u = PixelDimens.unit.toPx()
            val slot = size.width / points.size
            val barWidth = snap((slot - u * 2).coerceAtLeast(u))
            drawRect(GbbColors.Dusk, Offset(0f, size.height - u / 2), Size(size.width, u / 2))
            points.forEachIndexed { i, p ->
                if (p.value <= 0 || maxY <= 0) return@forEachIndexed
                val h = snap(((p.value / maxY) * size.height).toFloat().coerceAtLeast(u))
                drawRect(color, Offset(snap(i * slot + u), size.height - h), Size(barWidth, h))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(points.first().date.format(shortDate), style = MaterialTheme.typography.labelSmall, color = GbbColors.Fog)
            Text(points.last().date.format(shortDate), style = MaterialTheme.typography.labelSmall, color = GbbColors.Fog)
        }
    }
}

/**
 * Workout-day heatmap: one column per week (oldest left), one row per
 * weekday, brighter for more workouts that day.
 */
@Composable
fun PixelHeatmap(
    counts: Map<LocalDate, Int>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    modifier: Modifier = Modifier,
    weeks: Int = 17,
) {
    val lastWeek = Schedule.weekStart(today, firstDayOfWeek)
    val firstWeek = lastWeek.minusWeeks(weeks - 1L)
    val total = (0 until weeks * 7).sumOf { counts[firstWeek.plusDays(it.toLong())] ?: 0 }
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(weeks / 7f)
            .semantics { contentDescription = "$total workouts in the last $weeks weeks" },
    ) {
        val gap = PixelDimens.unit.toPx()
        val cell = minOf((size.width - gap * (weeks - 1)) / weeks, (size.height - gap * 6) / 7)
        for (w in 0 until weeks) {
            for (d in 0 until 7) {
                val date = firstWeek.plusWeeks(w.toLong()).plusDays(d.toLong())
                if (date > today) continue
                val n = counts[date] ?: 0
                val c = when {
                    n == 0 -> GbbColors.Dusk
                    n == 1 -> GbbColors.Forest
                    else -> GbbColors.Green
                }
                drawRect(c, Offset(w * (cell + gap), d * (cell + gap)), Size(cell, cell))
            }
        }
    }
}

/** Labeled horizontal bars, e.g. sets per muscle group. */
@Composable
fun PixelBarList(rows: List<Pair<String, Int>>, color: Color, modifier: Modifier = Modifier) {
    val max = rows.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { (label, value) ->
            Row {
                Text(label, style = MaterialTheme.typography.bodySmall, color = GbbColors.Chalk, modifier = Modifier.width(96.dp))
                Box(Modifier.weight(1f).height(16.dp)) {
                    Canvas(Modifier.fillMaxWidth(value.toFloat() / max).height(16.dp)) {
                        drawRect(color, Offset.Zero, Size(snap(size.width).coerceAtLeast(PixelDimens.unit.toPx()), size.height))
                    }
                }
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = GbbColors.Fog,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
