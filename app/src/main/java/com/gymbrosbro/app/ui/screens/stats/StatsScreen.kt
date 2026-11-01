package com.gymbrosbro.app.ui.screens.stats

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
fun StatsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("STATS", subtitle = "Every rep counts. Literally.")
        ComingSoonPanel(
            title = "HIGH SCORES",
            body = "Workouts, total reps, weight lifted, streaks and personal records — by week, month and lifetime.",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
