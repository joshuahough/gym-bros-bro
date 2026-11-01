package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.sprites.SpriteImage
import com.gymbrosbro.app.ui.navigation.TopLevelDestination
import com.gymbrosbro.app.ui.theme.GbbColors
import com.gymbrosbro.app.ui.theme.PixelDimens

@Composable
fun PixelBottomBar(
    selected: (TopLevelDestination) -> Boolean,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GbbColors.Night)
            .drawBehind {
                drawRect(GbbColors.Steel, Offset.Zero, Size(size.width, PixelDimens.unit.toPx()))
            }
            .navigationBarsPadding()
            .height(68.dp),
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val isSelected = selected(destination)
            val tint = if (isSelected) GbbColors.Gold else GbbColors.Fog
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(role = Role.Tab) { onSelect(destination) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SpriteImage(
                    sprite = destination.icon.recolor('f' to tint),
                    contentDescription = null,
                    size = 28.dp,
                )
                Spacer(Modifier.height(4.dp))
                Text(destination.label, style = MaterialTheme.typography.labelMedium, color = tint)
            }
        }
    }
}
