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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.gymbrosbro.app.avatar.AvatarConfig
import com.gymbrosbro.app.avatar.Locker
import com.gymbrosbro.app.avatar.LockerItem
import com.gymbrosbro.app.data.AwardsRepository
import com.gymbrosbro.app.data.BodyweightRepository
import com.gymbrosbro.app.data.ProgressRepository
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import com.gymbrosbro.app.domain.progress.Level
import com.gymbrosbro.app.domain.progress.SessionXp
import com.gymbrosbro.app.domain.progress.Trophies
import com.gymbrosbro.app.domain.progress.Trophy
import com.gymbrosbro.app.domain.progress.Xp
import com.gymbrosbro.app.domain.records.ExerciseRecord
import com.gymbrosbro.app.domain.session.SetFormat
import com.gymbrosbro.app.domain.session.WorkoutMath
import com.gymbrosbro.app.domain.units.Units
import com.gymbrosbro.app.feedback.Cue
import com.gymbrosbro.app.feedback.Feedback
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.AvatarView
import com.gymbrosbro.app.ui.components.LevelBar
import com.gymbrosbro.app.ui.components.NumberInputDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.StatTile
import com.gymbrosbro.app.ui.description
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

/** What this workout earned: its XP, and the level before and after it. */
data class SummaryXp(val earned: SessionXp, val before: Level, val after: Level) {
    val leveledUp: Boolean get() = after.level > before.level
}

class SessionSummaryViewModel(
    private val sessions: SessionRepository,
    private val bodyweight: BodyweightRepository,
    progress: ProgressRepository,
    awards: AwardsRepository,
    private val feedback: Feedback,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val sessionId = savedState.toRoute<SessionSummaryRoute>().sessionId

    val xp: StateFlow<SummaryXp?> = progress.xp
        .map { xp -> xp.sessions[sessionId]?.let { SummaryXp(it, xp.levelBefore(sessionId), xp.level) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Trophies and gear this workout unlocked. */
    val newTrophies: StateFlow<List<Trophy>> = awards.trophies
        .map { list -> list.filter { it.sessionId == sessionId }.mapNotNull { Trophies.byId(it.trophyId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val newGear: StateFlow<List<LockerItem>> = awards.unlocks
        .map { list -> list.filter { it.sessionId == sessionId }.mapNotNull { Locker.item(it.itemId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val avatar: StateFlow<AvatarConfig?> = awards.avatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun celebrate() = feedback.play(Cue.LEVEL_UP)

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
        SessionSummaryViewModel(
            c.sessionRepository, c.bodyweightRepository, c.progressRepository, c.awardsRepository, c.feedback, s,
        )
    }
    val xp by viewModel.xp.collectAsStateWithLifecycle()
    val newTrophies by viewModel.newTrophies.collectAsStateWithLifecycle()
    val newGear by viewModel.newGear.collectAsStateWithLifecycle()
    val avatar by viewModel.avatar.collectAsStateWithLifecycle()
    var cheered by rememberSaveable { mutableStateOf(false) }
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
    val seconds = d.session.endedAt?.let { Duration.between(d.session.startedAt, it).seconds.toInt() } ?: 0
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

            val leveledUp = xp?.leveledUp == true
            LaunchedEffect(leveledUp, newTrophies.isNotEmpty()) {
                if ((leveledUp || newTrophies.isNotEmpty()) && !cheered) {
                    cheered = true
                    viewModel.celebrate()
                }
            }
            avatar?.let {
                AvatarView(it, 111.dp, celebrate = leveledUp || records.isNotEmpty() || newTrophies.isNotEmpty())
            }
            xp?.let { XpPanel(it) }
            if (newTrophies.isNotEmpty() || newGear.isNotEmpty()) UnlocksPanel(newTrophies, newGear)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("TIME", SetFormat.duration(seconds), GbbColors.Cyan, Modifier.weight(1f))
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

@Composable
private fun XpPanel(xp: SummaryXp) {
    val e = xp.earned
    PixelPanel(Modifier.fillMaxWidth(), border = if (xp.leveledUp) GbbColors.Gold else GbbColors.Green) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (xp.leveledUp) {
                Text("LEVEL UP!", style = MaterialTheme.typography.headlineSmall, color = GbbColors.Gold)
                Text("You're level ${xp.after.level} now.", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Chalk)
                Spacer(Modifier.height(4.dp))
            }
            Text("+${e.total} XP", style = MaterialTheme.typography.titleLarge, color = GbbColors.Green)
            XpLine("Workout", Xp.WORKOUT)
            if (e.sets > 0) XpLine("${e.sets} ${if (e.sets == 1) "set" else "sets"}", e.sets * Xp.SET)
            if (e.records > 0) XpLine("${e.records} high ${if (e.records == 1) "score" else "scores"}", e.records * Xp.RECORD)
            if (e.scheduled) XpLine("On schedule", Xp.SCHEDULED)
            if (e.weeklyGoal) XpLine("Weekly goal hit", Xp.WEEKLY_GOAL)
            Spacer(Modifier.height(4.dp))
            LevelBar(xp.after, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun XpLine(label: String, xp: Int) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog, modifier = Modifier.weight(1f))
        Text("+$xp", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Chalk)
    }
}

@Composable
private fun UnlocksPanel(trophies: List<Trophy>, gear: List<LockerItem>) {
    val units = LocalUnitSystem.current
    PixelPanel(Modifier.fillMaxWidth(), border = GbbColors.Cyan) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (trophies.isNotEmpty()) {
                Text(
                    if (trophies.size == 1) "TROPHY EARNED!" else "${trophies.size} TROPHIES EARNED!",
                    style = MaterialTheme.typography.titleLarge,
                    color = GbbColors.Gold,
                )
                trophies.forEach { t ->
                    Text(t.name, style = MaterialTheme.typography.titleSmall, color = GbbColors.Chalk)
                    Text(t.description(units), style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                }
            }
            if (gear.isNotEmpty()) {
                if (trophies.isNotEmpty()) Spacer(Modifier.height(4.dp))
                Text("NEW GEAR!", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                Text(gear.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium, color = GbbColors.Chalk)
                Text("Try it on in the Locker (Me tab).", style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
            }
        }
    }
}
