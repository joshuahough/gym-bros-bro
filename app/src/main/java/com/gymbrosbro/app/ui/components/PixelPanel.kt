package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.ui.theme.GbbColors
import com.gymbrosbro.app.ui.theme.PixelDimens

/** A bordered box with stepped pixel corners. The building block for cards and tiles. */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    fill: Color = GbbColors.Night,
    border: Color = GbbColors.Steel,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .drawBehind {
                val u = PixelDimens.unit.toPx()
                drawNotchedRect(border, 0f, 0f, size.width, size.height, u)
                drawNotchedRect(fill, u, u, size.width - u, size.height - u, u)
            }
            .padding(PixelDimens.unit)
            .padding(contentPadding),
        content = content,
    )
}
