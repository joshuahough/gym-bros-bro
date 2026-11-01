package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.ui.theme.GbbColors

/** Label over a big pixel-font number. */
@Composable
fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    PixelPanel(modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, maxLines = 1)
        }
    }
}
