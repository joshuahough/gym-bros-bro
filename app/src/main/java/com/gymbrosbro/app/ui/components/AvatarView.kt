package com.gymbrosbro.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.gymbrosbro.app.avatar.AvatarConfig
import com.gymbrosbro.app.avatar.AvatarRenderer
import com.gymbrosbro.app.sprites.SpriteImage
import kotlinx.coroutines.delay

/**
 * The player's avatar with its 2-frame idle bob (SPEC §7.10). [celebrate]
 * makes it hop a few times, for PRs and level-ups.
 */
@Composable
fun AvatarView(
    config: AvatarConfig,
    height: Dp,
    modifier: Modifier = Modifier,
    celebrate: Boolean = false,
) {
    val frames = remember(config) { AvatarRenderer.frames(config) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(if (frame == 0) 900 else 500)
            frame = 1 - frame
        }
    }
    val hop = remember { Animatable(0f) }
    LaunchedEffect(celebrate) {
        if (!celebrate) return@LaunchedEffect
        delay(300)
        repeat(3) {
            hop.animateTo(-1f, tween(160))
            hop.animateTo(0f, tween(160))
        }
    }
    SpriteImage(
        sprite = frames[frame],
        contentDescription = "Your avatar",
        size = height,
        modifier = modifier.offset(y = height * 0.08f * hop.value),
    )
}
