package com.gymbrosbro.app.sprites

import androidx.compose.ui.graphics.Color

/**
 * A pixel-art image authored as text: each string is one row, each char one
 * pixel, and [palette] maps chars to colors. `.` is always transparent.
 *
 * Keeping art as data (instead of PNGs) makes palette swaps — skin tones,
 * selected-tab highlights, muscle-group colors — a one-line [recolor].
 */
class Sprite(
    val rows: List<String>,
    val palette: Map<Char, Color>,
) {
    val width: Int = rows.firstOrNull()?.length ?: 0
    val height: Int = rows.size

    init {
        require(rows.isNotEmpty()) { "Sprite has no rows" }
        rows.forEachIndexed { i, row ->
            require(row.length == width) { "Row $i is ${row.length} wide, expected $width" }
            row.forEach { c ->
                require(c == TRANSPARENT || c in palette) { "Row $i uses '$c' which has no palette color" }
            }
        }
    }

    /** Returns a copy with some palette entries replaced. */
    fun recolor(vararg swaps: Pair<Char, Color>): Sprite = Sprite(rows, palette + swaps)

    /** Color at (x, y), or [Color.Transparent]. */
    fun colorAt(x: Int, y: Int): Color {
        val c = rows[y][x]
        return if (c == TRANSPARENT) Color.Transparent else palette.getValue(c)
    }

    companion object {
        const val TRANSPARENT = '.'
    }
}
