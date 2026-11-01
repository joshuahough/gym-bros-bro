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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.sprites.SpriteImage
import com.gymbrosbro.app.sprites.Sprites
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.StatTile
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.localDate
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(sessions: SessionRepository) : ViewModel() {
    val inProgress: StateFlow<WorkoutSessionEntity?> = sessions.observeInProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastWorkout: StateFlow<WorkoutSessionEntity?> = sessions.observeLatestCompleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun HomeScreen(onPressStart: () -> Unit, onResume: (Long) -> Unit) {
    val viewModel = appViewModel { c, _ -> HomeViewModel(c.sessionRepository) }
    val inProgress by viewModel.inProgress.collectAsStateWithLifecycle()
    val lastWorkout by viewModel.lastWorkout.collectAsStateWithLifecycle()

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

        val active = inProgress
        if (active != null) {
            PixelPanel(Modifier.fillMaxWidth(), border = GbbColors.Green) {
                Column {
                    Text("IN PROGRESS", style = MaterialTheme.typography.titleLarge, color = GbbColors.Green)
                    Spacer(Modifier.height(8.dp))
                    Text(active.name, style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)
                }
            }
            PixelButton(
                text = "CONTINUE",
                onClick = { onResume(active.id) },
                color = GbbColors.Green,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PixelPanel(Modifier.fillMaxWidth()) {
                Column {
                    Text("TODAY", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Pick a workout and get after it.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GbbColors.Chalk,
                    )
                    lastWorkout?.let { last ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Last: ${last.name} · ${last.startedAt.localDate().display()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GbbColors.Fog,
                        )
                    }
                }
            }
            PixelButton(
                text = "PRESS START",
                onClick = onPressStart,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile("COMBO", "x0", GbbColors.Pink, Modifier.weight(1f))
            StatTile("LEVEL", "1", GbbColors.Green, Modifier.weight(1f))
        }
    }
}
