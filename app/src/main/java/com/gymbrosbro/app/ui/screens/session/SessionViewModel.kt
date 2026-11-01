package com.gymbrosbro.app.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gymbrosbro.app.data.BodyweightRepository
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.model.SessionStatus
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.session.WorkoutMath
import com.gymbrosbro.app.domain.session.WorkoutTotals
import com.gymbrosbro.app.ui.navigation.SessionRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
)

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
    private val appScope: CoroutineScope,
    savedState: SavedStateHandle,
) : ViewModel() {
    val sessionId = savedState.toRoute<SessionRoute>().sessionId

    private val detail = sessions.observeSession(sessionId)
    private val bodyweightPromptDismissed = MutableStateFlow(false)

    /** "Last time" sets, re-fetched only when the set of exercises changes. */
    private val lastTimes = detail
        .map { d ->
            if (d == null || d.session.status == SessionStatus.COMPLETED) emptySet()
            else d.exercises.map { it.exercise.id }.toSet()
        }
        .distinctUntilChanged()
        .map { ids -> ids.associateWith { sessions.lastSets(it, excludeSessionId = sessionId) } }

    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val state: StateFlow<SessionUiState?> = combine(
        detail,
        lastTimes,
        bodyweight.observeLatest(),
        bodyweightPromptDismissed,
    ) { d, last, latestBodyweight, dismissed ->
        d ?: return@combine null
        val blocks = d.sortedExercises.map { se ->
            ExerciseBlock(se.sessionExercise.id, se.exercise, se.sortedSets, last[se.exercise.id].orEmpty())
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

    fun toggleComplete(setId: Long, type: TrackingType) = launch {
        sessions.updateSet(setId) { s ->
            when {
                s.isCompleted -> s.copy(isCompleted = false, completedAt = null)
                WorkoutMath.canComplete(type, s) -> s.copy(isCompleted = true, completedAt = Instant.now())
                else -> s
            }
        }
        sessions.carryForward(setId)
    }

    fun toggleWarmup(setId: Long) = updateSet(setId) { it.copy(isWarmup = !it.isWarmup) }

    fun deleteSet(setId: Long) = launch { sessions.deleteSet(setId) }

    fun addSet(sessionExerciseId: Long) = launch { sessions.addSet(sessionExerciseId) }

    // Exercises

    fun addExercises(ids: List<String>) = launch { sessions.addExercises(sessionId, ids) }

    fun removeExercise(sessionExerciseId: Long) = launch { sessions.removeExercise(sessionExerciseId) }

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
