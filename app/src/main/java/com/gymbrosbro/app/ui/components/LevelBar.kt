package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.domain.progress.Level
import com.gymbrosbro.app.ui.theme.GbbColors
import java.text.NumberFormat

/** Chunky pixel progress bar, 0–1. */
@Composable
fun PixelProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(14.dp)
            .border(2.dp, GbbColors.Steel)
            .background(GbbColors.Void)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color),
        )
    }
}

/** "LV 3" with the XP bar toward the next level. */
@Composable
fun LevelBar(level: Level, modifier: Modifier = Modifier) {
    val n = NumberFormat.getIntegerInstance()
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LV ${level.level}", style = MaterialTheme.typography.titleMedium, color = GbbColors.Green)
            Spacer(Modifier.weight(1f))
            Text(
                "${n.format(level.xpIntoLevel)} / ${n.format(level.xpForNext)} XP",
                style = MaterialTheme.typography.bodySmall,
                color = GbbColors.Fog,
            )
        }
        Spacer(Modifier.height(6.dp))
        PixelProgressBar(level.progress, GbbColors.Green, Modifier.fillMaxWidth())
    }
}
