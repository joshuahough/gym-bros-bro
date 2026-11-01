package com.gymbrosbro.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.BodyweightRepository
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.domain.session.WorkoutMath
import com.gymbrosbro.app.domain.session.WorkoutTotals
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.PastDatePickerDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.localDate
import com.gymbrosbro.app.ui.summaryLine
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate

data class HistoryItem(val session: SessionWithExercises, val totals: WorkoutTotals, val minutes: Long)

class HistoryViewModel(
    private val sessions: SessionRepository,
    bodyweight: BodyweightRepository,
) : ViewModel() {
    val items: StateFlow<List<HistoryItem>?> = combine(sessions.observeCompleted(), bodyweight.observeLatest()) { list, latest ->
        list.map { d ->
            val bw = d.session.bodyweightKgSnapshot ?: latest?.weightKg
            HistoryItem(
                session = d,
                totals = WorkoutMath.totals(d.exercises.map { it.exercise to it.sets }, bw),
                minutes = d.session.endedAt?.let { Duration.between(d.session.startedAt, it).toMinutes() } ?: 0,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logPastWorkout(date: LocalDate, onCreated: (Long) -> Unit) = viewModelScope.launch {
        onCreated(sessions.createBackfill(date))
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenSession: (Long) -> Unit) {
    val viewModel = appViewModel { c, _ -> HistoryViewModel(c.sessionRepository, c.bodyweightRepository) }
    val items by viewModel.items.collectAsStateWithLifecycle()
    var pickDate by remember { mutableStateOf(false) }
    val units = LocalUnitSystem.current

    Column(Modifier.fillMaxSize()) {
        PixelTopBar("HISTORY", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                PixelButton(
                    "+ LOG PAST WORKOUT",
                    onClick = { pickDate = true },
                    color = GbbColors.Cyan,
                    modifier = Modifier.fillMaxWidth(),
                    small = true,
                )
            }
            val list = items
            when {
                list == null -> item { Text("LOADING...", style = MaterialTheme.typography.titleLarge, color = GbbColors.Fog) }
                list.isEmpty() -> item {
                    Text(
                        "No workouts logged yet. Finish one and it'll show up here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GbbColors.Fog,
                    )
                }
                else -> items(list, key = { it.session.session.id }) { item ->
                    PixelPanel(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClickLabel = "Edit workout") { onOpenSession(item.session.session.id) },
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.session.session.startedAt.localDate().display().uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GbbColors.Cyan,
                                    modifier = Modifier.weight(1f),
                                )
                                if (item.minutes > 0) {
                                    Text("${item.minutes} min", style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
                                }
                            }
                            Text(
                                item.session.session.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = GbbColors.Chalk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(item.totals.summaryLine(units), style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                        }
                    }
                }
            }
        }
    }

    if (pickDate) {
        PastDatePickerDialog(
            initial = LocalDate.now().minusDays(1),
            onConfirm = { date ->
                pickDate = false
                viewModel.logPastWorkout(date, onOpenSession)
            },
            onDismiss = { pickDate = false },
        )
    }
}
