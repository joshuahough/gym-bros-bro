package com.gymbrosbro.app.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gymbrosbro.app.data.AwardsRepository
import com.gymbrosbro.app.data.BodyweightRepository
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.model.SessionStatus
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.records.Performance
import com.gymbrosbro.app.domain.records.RecordHit
import com.gymbrosbro.app.domain.records.RecordType
import com.gymbrosbro.app.domain.records.Records
import com.gymbrosbro.app.domain.session.Overload
import com.gymbrosbro.app.domain.session.Suggestion
import com.gymbrosbro.app.domain.session.WorkoutMath
import com.gymbrosbro.app.domain.session.WorkoutTotals
import com.gymbrosbro.app.feedback.Cue
import com.gymbrosbro.app.feedback.Feedback
import com.gymbrosbro.app.ui.navigation.SessionRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class ExerciseBlock(
    val sessionExerciseId: Long,
    val exercise: ExerciseEntity,
    val sets: List<SetEntryEntity>,
    /** Completed sets from the previous time this exercise was done; empty when editing history. */
    val lastTime: List<SetEntryEntity>,
    /** Overload chips (SPEC §8); empty when editing history or with nothing left to fill. */
    val suggestions: List<Suggestion> = emptyList(),
    /** The other way to log this exercise (time ↔ reps), if it has one. Live workouts only. */
    val alternate: ExerciseEntity? = null,
)

/** What earlier workouts say about one exercise: last time's sets and the records to beat. */
private data class ExerciseContext(
    val lastTime: List<SetEntryEntity>,
    val bests: Map<RecordType, Double>,
    val alternate: ExerciseEntity?,
)

/** The NEW HIGH SCORE pop-up. */
data class HighScore(val exercise: ExerciseEntity, val hits: List<RecordHit>)

data class SessionUiState(
    val session: WorkoutSessionEntity,
    val blocks: List<ExerciseBlock>,
    val totals: WorkoutTotals,
    val uncheckedSets: Int,
    val needsBodyweight: Boolean,
) {
    val isCompleted get() = session.status == SessionStatus.COMPLETED
}

class SessionViewModel(
    private val sessions: SessionRepository,
    private val bodyweight: BodyweightRepository,
    settings: SettingsRepository,
    private val awards: AwardsRepository,
    private val feedback: Feedback,
    private val appScope: CoroutineScope,
    savedState: SavedStateHandle,
) : ViewModel() {
    val sessionId = savedState.toRoute<SessionRoute>().sessionId

    private val detail = sessions.observeSession(sessionId)
    private val bodyweightPromptDismissed = MutableStateFlow(false)

    /** Last time + records to beat, re-fetched only when the set of exercises changes. Live workouts only. */
    private val contexts = detail
        .map { d ->
            if (d == null || d.session.status == SessionStatus.COMPLETED) null
            else d.session to d.exercises.map { it.exercise }.distinctBy { it.id }
        }
        .distinctUntilChanged()
        .map { pair ->
            val (session, exercises) = pair ?: return@map emptyMap()
            exercises.associate { exercise ->
                val prior = sessions.priorPerformances(exercise.id, session)
                val bests = Records.history(
                    exercise.trackingType,
                    prior.map { Performance(it.session.id, it.session.startedAt, it.sets) },
                ).bests.mapValues { it.value.value }
                val alternate = exercise.alternateId?.let { sessions.getExercise(it) }
                exercise.id to ExerciseContext(prior.firstOrNull()?.sortedSets.orEmpty(), bests, alternate)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _highScore = MutableStateFlow<HighScore?>(null)
    val highScore: StateFlow<HighScore?> = _highScore.asStateFlow()

    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val state: StateFlow<SessionUiState?> = combine(
        detail,
        contexts,
        bodyweight.observeLatest(),
        bodyweightPromptDismissed,
        settings.unitSystem,
    ) { d, contexts, latestBodyweight, dismissed, units ->
        d ?: return@combine null
        val blocks = d.sortedExercises.map { se ->
            val context = contexts[se.exercise.id]
            val last = context?.lastTime.orEmpty()
            val open = se.sets.any { !it.isCompleted && !it.isWarmup }
            ExerciseBlock(
                sessionExerciseId = se.sessionExercise.id,
                exercise = se.exercise,
                sets = se.sortedSets,
                lastTime = last,
                suggestions = if (open) Overload.suggest(se.exercise, last, units) else emptyList(),
                alternate = context?.alternate,
            )
        }
        val bw = d.session.bodyweightKgSnapshot ?: latestBodyweight?.weightKg
        SessionUiState(
            session = d.session,
            blocks = blocks,
            totals = WorkoutMath.totals(blocks.map { it.exercise to it.sets }, bw),
            uncheckedSets = blocks.sumOf { b -> b.sets.count { !it.isCompleted } },
            needsBodyweight = !dismissed && bw == null && blocks.any {
                it.exercise.trackingType == TrackingType.BODYWEIGHT_REPS && it.exercise.bodyweightFactor != null
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    // Sets

    fun updateSet(setId: Long, transform: (SetEntryEntity) -> SetEntryEntity) = launch { sessions.updateSet(setId, transform) }

    fun toggleComplete(setId: Long, exercise: ExerciseEntity) = launch {
        val updated = sessions.updateSet(setId) { s ->
            when {
                s.isCompleted -> s.copy(isCompleted = false, completedAt = null)
                WorkoutMath.canComplete(exercise.trackingType, s) -> s.copy(isCompleted = true, completedAt = Instant.now())
                else -> s
            }
        } ?: return@launch
        sessions.carryForward(setId)
        if (!checkHighScore(updated, exercise) && updated.isCompleted) feedback.play(Cue.SET_DONE)
    }

    /** NEW HIGH SCORE when a just-checked set beats every earlier workout and every earlier set today. */
    private suspend fun checkHighScore(set: SetEntryEntity, exercise: ExerciseEntity): Boolean {
        if (!set.isCompleted || state.value?.isCompleted != false) return false
        val bests = contexts.value[exercise.id]?.bests ?: return false
        val hits = Records.brokenBy(exercise.trackingType, set, bests, sessions.getSets(set.sessionExerciseId))
        if (hits.isEmpty()) return false
        _highScore.value = HighScore(exercise, hits)
        feedback.play(Cue.HIGH_SCORE)
        return true
    }

    fun dismissHighScore() {
        _highScore.value = null
    }

    fun applySuggestion(sessionExerciseId: Long, suggestion: Suggestion) =
        launch { sessions.applyDrafts(sessionExerciseId, suggestion.sets) }

    fun toggleWarmup(setId: Long) = updateSet(setId) { it.copy(isWarmup = !it.isWarmup) }

    fun deleteSet(setId: Long) = launch { sessions.deleteSet(setId) }

    fun addSet(sessionExerciseId: Long) = launch { sessions.addSet(sessionExerciseId) }

    // Exercises

    fun addExercises(ids: List<String>) = launch { sessions.addExercises(sessionId, ids) }

    fun removeExercise(sessionExerciseId: Long) = launch { sessions.removeExercise(sessionExerciseId) }

    fun setExerciseWarmup(sessionExerciseId: Long, warmup: Boolean) =
        launch { sessions.setExerciseWarmup(sessionExerciseId, warmup) }

    /** Time ↔ reps: swaps in the alternate exercise with fresh rows. */
    fun switchMode(sessionExerciseId: Long, alternateId: String) =
        launch { sessions.switchExercise(sessionExerciseId, alternateId) }

    fun moveExercise(sessionExerciseId: Long, delta: Int) = launch { sessions.moveExercise(sessionId, sessionExerciseId, delta) }

    // Bodyweight prompt

    fun saveBodyweight(weightKg: Double) = launch {
        bodyweight.record(weightKg)
        sessions.setBodyweightSnapshot(sessionId, weightKg)
    }

    fun dismissBodyweightPrompt() {
        bodyweightPromptDismissed.value = true
    }

    // Whole session

    fun finish(onDone: () -> Unit) = launch {
        sessions.finish(sessionId)
        awards.sync(sessionId)
        onDone()
    }

    fun delete(onDone: () -> Unit) = launch {
        sessions.delete(sessionId)
        onDone()
    }

    fun changeDate(date: LocalDate) = launch { sessions.changeDate(sessionId, date) }

    /** Leaving a past workout: drop rows that were added but never checked off. */
    override fun onCleared() {
        if (state.value?.isCompleted == true) {
            appScope.launch { sessions.pruneIncomplete(sessionId) }
        }
    }
}
