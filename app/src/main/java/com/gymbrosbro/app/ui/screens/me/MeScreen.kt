package com.gymbrosbro.app.ui.screens.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.ui.components.ComingSoonPanel
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.ScreenHeader
import com.gymbrosbro.app.ui.theme.GbbColors

@Composable
fun MeScreen(onOpenHistory: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("ME")
        MenuPanel("HISTORY", "Every workout you've logged. Tap one to fix a set or change the date.", onOpenHistory)
        MenuPanel("SETTINGS", "Units (lb / kg), bodyweight and screen-awake.", onOpenSettings)
        ComingSoonPanel("LOCKER", "Customize your bro and show off the gear you've unlocked.", Modifier.fillMaxWidth())
    }
}

@Composable
private fun MenuPanel(title: String, body: String, onClick: () -> Unit) {
    PixelPanel(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Column {
            Text("$title  >", style = MaterialTheme.typography.titleLarge, color = GbbColors.Gold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
