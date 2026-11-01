package com.gymbrosbro.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.sprites.SpriteImage
import com.gymbrosbro.app.sprites.Sprites
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.theme.GbbColors

@Composable
fun HomeScreen(onPressStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "GYM BROS BRO",
            style = MaterialTheme.typography.displaySmall,
            color = GbbColors.Gold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "The gym bro's bro.",
            style = MaterialTheme.typography.bodyMedium,
            color = GbbColors.Fog,
        )

        PixelPanel(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpriteImage(
                    sprite = Sprites.Player.recolor('f' to GbbColors.Cyan),
                    contentDescription = "Your avatar",
                    size = 96.dp,
                )
                Spacer(Modifier.height(12.dp))
                Text("PLAYER 1", style = MaterialTheme.typography.titleLarge, color = GbbColors.Chalk)
            }
        }

        PixelPanel(Modifier.fillMaxWidth()) {
            Column {
                Text("TODAY", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No workout scheduled. Pick one and get after it.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GbbColors.Chalk,
                )
            }
        }

        PixelButton(
            text = "PRESS START",
            onClick = onPressStart,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile("COMBO", "x0", GbbColors.Pink, Modifier.weight(1f))
            StatTile("LEVEL", "1", GbbColors.Green, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    PixelPanel(modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineLarge, color = accent)
        }
    }
}
