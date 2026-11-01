package com.gymbrosbro.app.ui.screens.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.ui.components.ComingSoonPanel
import com.gymbrosbro.app.ui.components.ScreenHeader

@Composable
fun MeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("ME")
        ComingSoonPanel("LOCKER", "Customize your bro and show off the gear you've unlocked.", Modifier.fillMaxWidth())
        ComingSoonPanel("HISTORY", "Every workout you've logged, editable if you fat-finger a set.", Modifier.fillMaxWidth())
        ComingSoonPanel("SETTINGS", "Units (lb / kg), bodyweight, reminders and sound.", Modifier.fillMaxWidth())
    }
}
