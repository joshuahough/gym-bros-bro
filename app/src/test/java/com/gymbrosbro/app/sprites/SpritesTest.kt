package com.gymbrosbro.app.sprites

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SpritesTest {
    @Test
    fun `all sprites are 12x12`() {
        // Constructing Sprites validates row widths and palette coverage.
        Sprites.all.forEach { sprite ->
            assertEquals(12, sprite.width)
            assertEquals(12, sprite.height)
        }
    }

    @Test
    fun `recolor swaps only the given palette entry`() {
        val sprite = Sprite(listOf("fo"), mapOf('f' to Color.Red, 'o' to Color.Black))
        val swapped = sprite.recolor('f' to Color.Green)
        assertEquals(Color.Green, swapped.colorAt(0, 0))
        assertEquals(Color.Black, swapped.colorAt(1, 0))
    }

    @Test
    fun `dot is transparent`() {
        assertEquals(Color.Transparent, Sprite(listOf("."), emptyMap()).colorAt(0, 0))
    }

    @Test
    fun `ragged rows are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Sprite(listOf("..", "..."), emptyMap())
        }
    }

    @Test
    fun `unknown palette chars are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Sprite(listOf("x"), emptyMap())
        }
    }
}
