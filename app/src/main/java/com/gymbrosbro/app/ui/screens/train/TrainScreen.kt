package com.gymbrosbro.app.ui.screens.train

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.ui.components.ComingSoonPanel
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.ScreenHeader
import com.gymbrosbro.app.ui.screens.library.LibraryScreen
import com.gymbrosbro.app.ui.theme.GbbColors

private val tabs = listOf("WORKOUTS", "LIBRARY")

@Composable
fun TrainScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenHeader("TRAIN", Modifier.padding(top = 16.dp, bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedTab
                PixelPanel(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { selected = isSelected }
                        .clickable(role = Role.Tab) { selectedTab = index },
                    fill = if (isSelected) GbbColors.Dusk else GbbColors.Night,
                    border = if (isSelected) GbbColors.Gold else GbbColors.Steel,
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) GbbColors.Gold else GbbColors.Fog,
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize().padding(top = 12.dp)) {
            when (selectedTab) {
                0 -> ComingSoonPanel(
                    title = "NO WORKOUTS YET",
                    body = "The workout builder is coming next. You'll combine library exercises into your own routines here.",
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> LibraryScreen()
            }
        }
    }
}
