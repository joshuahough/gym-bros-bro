package com.gymbrosbro.app.avatar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.gymbrosbro.app.sprites.Sprite
import com.gymbrosbro.app.ui.theme.GbbColors

/** Builds the avatar sprite from an [AvatarConfig] by stacking its layers. */
object AvatarRenderer {
    /** Two idle frames: standing, then the upper body bobbed down a pixel. */
    fun frames(config: AvatarConfig): List<Sprite> {
        val (rows, palette) = compose(config)
        val drop = AvatarArt.BOB_ROW + AvatarArt.HEADROOM
        val bobbed = listOf(".".repeat(AvatarArt.WIDTH)) + rows.take(drop) + rows.drop(drop + 1)
        return listOf(Sprite(rows, palette), Sprite(bobbed, palette))
    }

    fun sprite(config: AvatarConfig): Sprite = compose(config).let { (rows, palette) -> Sprite(rows, palette) }

    private fun compose(config: AvatarConfig): Pair<List<String>, Map<Char, Color>> {
        val defaults = AvatarConfig()
        fun pick(id: String, fallback: String) = Locker.item(id) ?: Locker.item(fallback)!!

        val skin = pick(config.skin, defaults.skin)
        val hairColor = pick(config.hairColor, defaults.hairColor)
        val topColor = pick(config.topColor, defaults.topColor).swatch
        val bottomColor = pick(config.bottomColor, defaults.bottomColor).swatch
        val headwear = pick(config.headwear, defaults.headwear)
        val accessory = pick(config.accessory, defaults.accessory)

        val grid = Array(AvatarArt.HEIGHT) { CharArray(AvatarArt.WIDTH) { Sprite.TRANSPARENT } }
        fun draw(layer: Layer?, minRow: Int = Int.MIN_VALUE) {
            layer ?: return
            layer.rows.forEachIndexed { i, row ->
                val bodyRow = layer.top + i
                if (bodyRow < minRow) return@forEachIndexed
                val y = bodyRow + AvatarArt.HEADROOM
                row.forEachIndexed { x, c -> if (c != Sprite.TRANSPARENT) grid[y][x] = c }
            }
        }
        draw(AvatarArt.Body)
        draw(AvatarArt.Sneakers)
        draw(pick(config.bottom, defaults.bottom).layer)
        draw(pick(config.top, defaults.top).layer)
        draw(pick(config.hair, defaults.hair).layer, minRow = headwear.layer?.takeIf { headwear.coversHair }?.top ?: Int.MIN_VALUE)
        draw(accessory.layer)
        draw(headwear.layer)

        val palette = mapOf(
            'o' to GbbColors.Void,
            'k' to GbbColors.Void,
            'w' to GbbColors.Chalk,
            's' to skin.swatch,
            'd' to (skin.shade ?: shade(skin.swatch)),
            'h' to hairColor.swatch,
            'f' to topColor,
            'g' to shade(topColor),
            'p' to bottomColor,
            'q' to shade(bottomColor),
            'b' to pick(config.shoes, defaults.shoes).swatch,
        ) + accessory.palette + headwear.palette
        return grid.map { String(it) } to palette
    }

    private fun shade(color: Color): Color = lerp(color, GbbColors.Void, 0.3f)
}
