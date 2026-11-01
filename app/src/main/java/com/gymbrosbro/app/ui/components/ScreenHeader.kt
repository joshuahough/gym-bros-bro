package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.ui.theme.GbbColors

@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = GbbColors.Gold,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
        }
    }
}

/** Placeholder card for screens whose features land in a later phase. */
@Composable
fun ComingSoonPanel(title: String, body: String, modifier: Modifier = Modifier) {
    PixelPanel(modifier) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
        }
    }
}
