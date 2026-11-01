package com.gymbrosbro.app.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gymbrosbro.app.data.BodyweightRepository
import com.gymbrosbro.app.data.ProgressRepository
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import com.gymbrosbro.app.domain.records.ExerciseRecord
import com.gymbrosbro.app.domain.session.SetFormat
import com.gymbrosbro.app.domain.session.WorkoutMath
import com.gymbrosbro.app.domain.units.Units
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.NumberInputDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.StatTile
import com.gymbrosbro.app.ui.format
import com.gymbrosbro.app.ui.formatVolume
import com.gymbrosbro.app.ui.label
import com.gymbrosbro.app.ui.navigation.SessionSummaryRoute
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SessionSummaryViewModel(
    private val sessions: SessionRepository,
    private val bodyweight: BodyweightRepository,
    progress: ProgressRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val sessionId = savedState.toRoute<SessionSummaryRoute>().sessionId

    /** Records set in this workout, including whole-workout ones like volume. */
    val records: StateFlow<List<ExerciseRecord>> = progress.records
        .map { all -> all.filter { it.event.sessionId == sessionId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val session: StateFlow<SessionWithExercises?> = sessions.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestBodyweight: StateFlow<BodyweightEntryEntity?> = bodyweight.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Offer "save changes to workout" only when the session diverged from its workout. */
    private val _canUpdateTemplate = MutableStateFlow(false)
    val canUpdateTemplate: StateFlow<Boolean> = _canUpdateTemplate.asStateFlow()

    private val _templateUpdated = MutableStateFlow(false)
    val templateUpdated: StateFlow<Boolean> = _templateUpdated.asStateFlow()

    init {
        viewModelScope.launch {
            val detail = sessions.getSession(sessionId) ?: return@launch
            _canUpdateTemplate.value = sessions.differsFromTemplate(detail)
        }
    }

    fun updateTemplate() = viewModelScope.launch {
        sessions.saveToTemplate(sessionId)
        _canUpdateTemplate.value = false
        _templateUpdated.value = true
    }

    /** "Still 180 lb?" → yes: re-record today so the reminder goes quiet for another month. */
    fun confirmBodyweight() = viewModelScope.launch {
        latestBodyweight.value?.let { bodyweight.record(it.weightKg) }
    }

    fun updateBodyweight(weightKg: Double) = viewModelScope.launch { bodyweight.record(weightKg) }
}

/** Shown right after finishing (SPEC §7.3, "STAGE CLEAR"). */
@Composable
fun SessionSummaryScreen(onDone: () -> Unit) {
    val viewModel = appViewModel { c, s ->
        SessionSummaryViewModel(c.sessionRepository, c.bodyweightRepository, c.progressRepository, s)
    }
    val records by viewModel.records.collectAsStateWithLifecycle()
    val detail by viewModel.session.collectAsStateWithLifecycle()
    val latestBodyweight by viewModel.latestBodyweight.collectAsStateWithLifecycle()
    val canUpdateTemplate by viewModel.canUpdateTemplate.collectAsStateWithLifecycle()
    val templateUpdated by viewModel.templateUpdated.collectAsStateWithLifecycle()
    var bodyweightHandled by remember { mutableStateOf(false) }
    var editBodyweight by remember { mutableStateOf(false) }
    val units = LocalUnitSystem.current

    val d = detail ?: return
    val bw = d.session.bodyweightKgSnapshot ?: latestBodyweight?.weightKg
    val exercises = d.sortedExercises
    val totals = WorkoutMath.totals(exercises.map { it.exercise to it.sets }, bw)
    val minutes = d.session.endedAt?.let { Duration.between(d.session.startedAt, it).toMinutes() } ?: 0
    val staleBodyweight = latestBodyweight?.takeIf { ChronoUnit.DAYS.between(it.date, LocalDate.now()) > 30 }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Text("STAGE CLEAR!", style = MaterialTheme.typography.displayMedium, color = GbbColors.Gold, textAlign = TextAlign.Center)
            Text(d.session.name, style = MaterialTheme.typography.titleMedium, color = GbbColors.Fog)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("TIME", "${minutes}m", GbbColors.Cyan, Modifier.weight(1f))
                StatTile("SETS", totals.sets.toString(), GbbColors.Green, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("REPS", totals.reps.toString(), GbbColors.Pink, Modifier.weight(1f))
                if (totals.distanceM > 0) {
                    StatTile("DISTANCE", Units.formatDistance(totals.distanceM, units), GbbColors.Orange, Modifier.weight(1f))
                } else {
                    StatTile("LIFTED", formatVolume(totals.volumeKg, units), GbbColors.Orange, Modifier.weight(1f))
                }
            }

            if (records.isNotEmpty()) {
                PixelPanel(Modifier.fillMaxWidth(), border = GbbColors.Gold) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (records.size == 1) "NEW HIGH SCORE!" else "${records.size} NEW HIGH SCORES!",
                            style = MaterialTheme.typography.titleLarge,
                            color = GbbColors.Gold,
                        )
                        records.forEach { r ->
                            Text(r.exercise.name, style = MaterialTheme.typography.titleSmall, color = GbbColors.Chalk)
                            Text(
                                "${r.event.type.label}: ${r.event.type.format(r.event.value, units)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GbbColors.Green,
                            )
                        }
                    }
                }
            }

            PixelPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RECAP", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    exercises.forEach { se ->
                        Text(se.exercise.name, style = MaterialTheme.typography.titleSmall, color = GbbColors.Chalk)
                        Text(
                            SetFormat.summary(se.exercise.trackingType, se.sortedSets, units),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GbbColors.Fog,
                        )
                    }
                }
            }

            if (canUpdateTemplate) {
                PixelPanel(Modifier.fillMaxWidth(), border = GbbColors.Cyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "You changed things up from the plan. Save today's exercises and set counts to the workout?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GbbColors.Chalk,
                        )
                        PixelButton("SAVE TO WORKOUT", onClick = { viewModel.updateTemplate() }, color = GbbColors.Cyan, small = true)
                    }
                }
            }
            if (templateUpdated) {
                Text("Workout updated.", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Green)
            }

            if (staleBodyweight != null && !bodyweightHandled) {
                PixelPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Still ${Units.formatWeight(staleBodyweight.weightKg, units)}?",
                            style = MaterialTheme.typography.titleMedium,
                            color = GbbColors.Chalk,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PixelButton(
                                "YES",
                                onClick = {
                                    viewModel.confirmBodyweight()
                                    bodyweightHandled = true
                                },
                                small = true,
                            )
                            PixelButton(
                                "UPDATE",
                                onClick = { editBodyweight = true },
                                small = true,
                                color = GbbColors.Steel,
                                contentColor = GbbColors.Chalk,
                            )
                            PixelButton(
                                "LATER",
                                onClick = { bodyweightHandled = true },
                                small = true,
                                color = GbbColors.Steel,
                                contentColor = GbbColors.Chalk,
                            )
                        }
                    }
                }
            }
        }
        PixelButton("DONE", onClick = onDone, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }

    if (editBodyweight) {
        NumberInputDialog(
            title = "BODYWEIGHT",
            unitLabel = Units.weightUnit(units),
            initial = staleBodyweight?.let { Units.formatNumber(Units.kgToDisplay(it.weightKg, units), 1) }.orEmpty(),
            onConfirm = { value ->
                viewModel.updateBodyweight(Units.displayToKg(value, units))
                editBodyweight = false
                bodyweightHandled = true
            },
            onDismiss = { editBodyweight = false },
        )
    }
}
