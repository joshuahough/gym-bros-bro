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
import com.gymbrosbro.app.data.ProgressRepository
import com.gymbrosbro.app.data.ScheduleRepository
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.StreakSummary
import com.gymbrosbro.app.data.TemplateRepository
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.domain.schedule.Schedule
import com.gymbrosbro.app.sprites.SpriteImage
import com.gymbrosbro.app.sprites.Sprites
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.StatTile
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.localDate
import com.gymbrosbro.app.ui.screens.workouts.StartWorkoutDialog
import com.gymbrosbro.app.ui.screens.workouts.WorkoutsViewModel
import com.gymbrosbro.app.ui.screens.workouts.rememberStartWorkoutController
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Today's plan for Home: what's scheduled, whether it's done, and what's next. */
data class TodayPlan(
    /** Workouts planned today; null template = any workout. */
    val planned: List<Pair<Long?, String>> = emptyList(),
    val trainedToday: Boolean = false,
    val next: Pair<LocalDate, String>? = null,
)

class HomeViewModel(
    sessions: SessionRepository,
    schedule: ScheduleRepository,
    templates: TemplateRepository,
    progress: ProgressRepository,
) : ViewModel() {
    val inProgress: StateFlow<WorkoutSessionEntity?> = sessions.observeInProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastWorkout: StateFlow<WorkoutSessionEntity?> = sessions.observeLatestCompleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val streaks: StateFlow<StreakSummary> = progress.streaks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakSummary())

    val today: StateFlow<TodayPlan> = combine(schedule.observe(), templates.observeAll(), progress.workoutsByDay) { data, all, byDay ->
        val today = LocalDate.now()
        val names = all.associate { it.id to it.name }
        fun name(id: Long?) = id?.let { names[it] } ?: "Any workout"
        val upcoming = Schedule.occurrences(data, today, today.plusDays(14)).filter { it.active }
        TodayPlan(
            planned = upcoming.filter { it.date == today }.map { it.templateId to name(it.templateId) },
            trainedToday = byDay.containsKey(today),
            next = upcoming.firstOrNull { it.date > today }?.let { it.date to name(it.templateId) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayPlan())
}

@Composable
fun HomeScreen(onPressStart: () -> Unit, onOpenSession: (Long) -> Unit) {
    val viewModel = appViewModel { c, _ ->
        HomeViewModel(c.sessionRepository, c.scheduleRepository, c.templateRepository, c.progressRepository)
    }
    val workouts = appViewModel { c, _ -> WorkoutsViewModel(c.templateRepository, c.sessionRepository) }
    val starter = rememberStartWorkoutController(workouts, onOpenSession)
    val inProgress by viewModel.inProgress.collectAsStateWithLifecycle()
    val lastWorkout by viewModel.lastWorkout.collectAsStateWithLifecycle()
    val streaks by viewModel.streaks.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()

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
                onClick = { onOpenSession(active.id) },
                color = GbbColors.Green,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // One planned workout with a template: PRESS START goes straight into it.
            val startTemplate = today.planned.singleOrNull()?.first?.takeIf { !today.trainedToday }
            PixelPanel(Modifier.fillMaxWidth()) {
                Column {
                    Text("TODAY", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            today.trainedToday -> "Done for today. Nice work."
                            today.planned.isNotEmpty() -> today.planned.joinToString(" + ") { it.second }
                            else -> "Pick a workout and get after it."
                        },
                        style = if (today.planned.isNotEmpty() && !today.trainedToday) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = GbbColors.Chalk,
                    )
                    streaks.schedule.atRisk?.takeIf { !today.trainedToday }?.let { missed ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Missed ${missed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} — train today to keep your COMBO.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GbbColors.Gold,
                        )
                    }
                    today.next?.let { (date, name) ->
                        Spacer(Modifier.height(6.dp))
                        Text("Next: $name · ${date.display()}", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                    }
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
                onClick = { if (startTemplate != null) starter.request(startTemplate) else onPressStart() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                if (streaks.hasSchedule) "COMBO" else "WEEKLY",
                "×${if (streaks.hasSchedule) streaks.schedule.current else streaks.weekly.current}",
                GbbColors.Pink,
                Modifier.weight(1f),
            )
            StatTile("LEVEL", "1", GbbColors.Green, Modifier.weight(1f))
        }
    }

    StartWorkoutDialog(starter)
}
