package com.perfectapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * A minimal, dependency-free line chart for trend visualization (weight over time, etc).
 * Draws a smoothed-looking polyline + point markers on a Canvas, normalized to [0,1].
 * Intentionally simple — swap for a full charting lib later if richer interaction is needed.
 */
@Composable
fun TrendLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.secondary,
    pointColor: Color = MaterialTheme.colorScheme.primary
) {
    if (values.size < 2) {
        return
    }

    val min = values.min()
    val max = values.max()
    val range = (max - min).takeIf { it > 0f } ?: 1f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val normalized = (value - min) / range
            val x = index * stepX
            val y = size.height - (normalized * size.height)
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }

        points.forEach { point ->
            drawCircle(color = pointColor, radius = 6f, center = point)
        }
    }
}
