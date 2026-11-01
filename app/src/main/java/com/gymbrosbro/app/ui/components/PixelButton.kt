package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.ui.theme.GbbColors
import com.gymbrosbro.app.ui.theme.PixelDimens

/**
 * Arcade-style button: a raised face over a darker "depth" layer. Pressing
 * drops the face down onto the depth layer, like a physical cabinet button.
 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = GbbColors.Gold,
    contentColor: Color = GbbColors.Void,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val face = if (enabled) color else GbbColors.Steel
    val depthColor = lerp(face, Color.Black, 0.45f)
    val depth = PixelDimens.unit * 2
    val sink = if (pressed) depth else 0.dp

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .drawBehind {
                val u = PixelDimens.unit.toPx()
                val d = depth.toPx()
                val s = sink.toPx()
                drawNotchedRect(depthColor, 0f, d, size.width, size.height, u)
                drawNotchedRect(face, 0f, s, size.width, size.height - d + s, u)
            }
            .padding(bottom = depth)
            .offset(y = sink)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) contentColor else GbbColors.Fog,
        )
    }
}
