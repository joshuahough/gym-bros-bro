package com.gymbrosbro.app.ui.screens.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.ProgressRepository
import com.gymbrosbro.app.data.StreakSummary
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.stats.DateRange
import com.gymbrosbro.app.domain.stats.PeriodStats
import com.gymbrosbro.app.domain.stats.Stats
import com.gymbrosbro.app.domain.stats.StatsPeriod
import com.gymbrosbro.app.domain.units.Units
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.PixelBarList
import com.gymbrosbro.app.ui.components.PixelChip
import com.gymbrosbro.app.ui.components.PixelHeatmap
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.ScreenHeader
import com.gymbrosbro.app.ui.components.StatTile
import com.gymbrosbro.app.ui.formatVolume
import com.gymbrosbro.app.ui.label
import com.gymbrosbro.app.ui.screens.library.MuscleIcon
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class StatsUiState(
    val period: StatsPeriod,
    val offset: Int,
    val range: DateRange?,
    val stats: PeriodStats,
    val streaks: StreakSummary,
    val workoutsByDay: Map<LocalDate, Int>,
    val weekStart: DayOfWeek,
)

class StatsViewModel(progress: ProgressRepository, settings: SettingsRepository) : ViewModel() {
    private val selection = MutableStateFlow(StatsPeriod.WEEK to 0)

    val state: StateFlow<StatsUiState?> = combine(
        combine(progress.completed, progress.records, ::Pair),
        progress.streaks,
        progress.workoutsByDay,
        settings.weekStart,
        selection,
    ) { (sessions, records), streaks, byDay, weekStart, (period, offset) ->
        val range = Stats.range(period, offset, LocalDate.now(), weekStart)
        StatsUiState(period, offset, range, Stats.period(sessions, records.map { it.event }, range), streaks, byDay, weekStart)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectPeriod(period: StatsPeriod) {
        selection.value = period to 0
    }

    /** -1 = further back, +1 = toward now. */
    fun shift(delta: Int) {
        val (period, offset) = selection.value
        selection.value = period to (offset - delta).coerceAtLeast(0)
    }
}

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val dayFormat = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/** The Stats tab (SPEC §7.7). */
@Composable
fun StatsScreen(onOpenExercise: (String) -> Unit) {
    val viewModel = appViewModel { c, _ -> StatsViewModel(c.progressRepository, c.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val units = LocalUnitSystem.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("STATS", subtitle = "Every rep counts. Literally.")
        val s = state ?: return@Column

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatsPeriod.entries.forEach { p ->
                PixelChip(p.label, selected = s.period == p, onClick = { viewModel.selectPeriod(p) })
            }
        }
        s.range?.let { range ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArrowButton("<", enabled = true) { viewModel.shift(-1) }
                Text(
                    rangeLabel(s.period, s.offset, range),
                    style = MaterialTheme.typography.titleMedium,
                    color = GbbColors.Chalk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                ArrowButton(">", enabled = s.offset > 0) { viewModel.shift(1) }
            }
        }

        val stats = s.stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("WORKOUTS", stats.workouts.toString(), GbbColors.Green, Modifier.weight(1f))
            StatTile("HIGH SCORES", stats.records.toString(), GbbColors.Gold, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("REPS", "%,d".format(stats.totals.reps), GbbColors.Pink, Modifier.weight(1f))
            StatTile("TIME", trainedTime(stats.timeTrainedSec), GbbColors.Cyan, Modifier.weight(1f))
        }
        StatTile("WEIGHT LIFTED", formatVolume(stats.totals.volumeKg, units), GbbColors.Orange, Modifier.fillMaxWidth())
        if (stats.totals.distanceM > 0) {
            StatTile("CARDIO DISTANCE", Units.formatDistance(stats.totals.distanceM, units), GbbColors.Purple, Modifier.fillMaxWidth())
        }

        StreakPanel(s.streaks)

        PixelPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("WORKOUT DAYS", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                PixelHeatmap(s.workoutsByDay, LocalDate.now(), s.weekStart)
                Text("Last 17 weeks", style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
            }
        }

        if (stats.setsByMuscle.isNotEmpty()) {
            PixelPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("SETS BY MUSCLE", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    PixelBarList(stats.setsByMuscle.map { (m, n) -> m.label to n }, GbbColors.Green)
                    Text("Working sets, by each exercise's main muscle.", style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
                }
            }
        }

        if (stats.exercises.isNotEmpty()) {
            PixelPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("EXERCISES", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    stats.exercises.take(15).forEach { (exercise, sets) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onOpenExercise(exercise.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MuscleIcon(exercise.primaryMuscle, size = 24)
                            Spacer(Modifier.width(10.dp))
                            Text(exercise.name, style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk, modifier = Modifier.weight(1f))
                            Text("$sets ${if (sets == 1) "set" else "sets"}  >", style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
                        }
                    }
                }
            }
        }
        if (stats.workouts == 0) {
            Text(
                "No workouts in this stretch yet. Go make some numbers.",
                style = MaterialTheme.typography.bodyMedium,
                color = GbbColors.Fog,
            )
        }
    }
}

@Composable
private fun StreakPanel(streaks: StreakSummary) {
    PixelPanel(Modifier.fillMaxWidth(), border = GbbColors.Pink) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("STREAKS", style = MaterialTheme.typography.titleLarge, color = GbbColors.Pink)
            if (streaks.hasSchedule) {
                StreakRow("COMBO ×${streaks.schedule.current}", "Best ×${streaks.schedule.best}")
                Text("Scheduled workouts in a row.", style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
                if (streaks.schedule.freezes > 0) {
                    Text(
                        "${streaks.schedule.freezes} streak ${if (streaks.schedule.freezes == 1) "freeze" else "freezes"} banked",
                        style = MaterialTheme.typography.bodySmall,
                        color = GbbColors.Cyan,
                    )
                }
            } else {
                Text("Set a schedule in Calendar to start a COMBO.", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
            }
            StreakRow("WEEKLY ×${streaks.weekly.current}", "Best ×${streaks.weekly.best}")
            Text(
                "Weeks with ${streaks.weekly.goal}+ workouts. This week: ${streaks.weekly.thisWeek} of ${streaks.weekly.goal}.",
                style = MaterialTheme.typography.bodySmall,
                color = GbbColors.Fog,
            )
        }
    }
}

@Composable
private fun StreakRow(current: String, best: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(current, style = MaterialTheme.typography.headlineSmall, color = GbbColors.Chalk, modifier = Modifier.weight(1f))
        Text(best, style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
    }
}

@Composable
private fun ArrowButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = if (enabled) GbbColors.Gold else GbbColors.Steel)
    }
}

private val StatsPeriod.label
    get() = when (this) {
        StatsPeriod.WEEK -> "Week"
        StatsPeriod.MONTH -> "Month"
        StatsPeriod.YEAR -> "Year"
        StatsPeriod.LIFETIME -> "Lifetime"
    }

private fun rangeLabel(period: StatsPeriod, offset: Int, range: DateRange): String = when (period) {
    StatsPeriod.WEEK -> when (offset) {
        0 -> "This week"
        1 -> "Last week"
        else -> "${range.start.format(dayFormat)} – ${range.end.format(dayFormat)}"
    }
    StatsPeriod.MONTH -> range.start.format(monthFormat)
    StatsPeriod.YEAR -> range.start.year.toString()
    StatsPeriod.LIFETIME -> ""
}

/** "45m", "3h 20m", "112h". */
private fun trainedTime(seconds: Long): String {
    val minutes = seconds / 60
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "${m}m"
        h >= 100 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}
