package com.gymbrosbro.app.sprites

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

/** Draws a [Sprite] scaled with nearest-neighbor filtering so pixels stay crisp. */
@Composable
fun SpriteImage(
    sprite: Sprite,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(sprite) { sprite.toImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )
}

private fun Sprite.toImageBitmap(): ImageBitmap {
    val pixels = IntArray(width * height) { i -> colorAt(i % width, i / width).toArgb() }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
