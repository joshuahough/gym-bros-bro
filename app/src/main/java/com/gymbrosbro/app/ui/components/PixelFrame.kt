package com.gymbrosbro.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Fills a rectangle with one-pixel notched corners — the classic 8-bit
 * "rounded" box. Drawn as two overlapping rects (a plus shape) so the four
 * corner pixels are left empty.
 */
internal fun DrawScope.drawNotchedRect(
    color: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    notch: Float,
) {
    val width = right - left
    val height = bottom - top
    if (width <= 0f || height <= 0f) return
    drawRect(color, Offset(left, top + notch), Size(width, height - 2 * notch))
    drawRect(color, Offset(left + notch, top), Size(width - 2 * notch, height))
}
