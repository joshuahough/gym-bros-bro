package com.gymbrosbro.app.ui.screens.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.TemplateRepository
import com.gymbrosbro.app.data.db.TemplateWithExercises
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Starting a workout, shared by the Workouts list and Home. Only one can be in progress. */
sealed interface StartResult {
    data class Started(val sessionId: Long) : StartResult
    data class Blocked(val inProgress: WorkoutSessionEntity) : StartResult
}

class WorkoutsViewModel(
    templates: TemplateRepository,
    private val sessions: SessionRepository,
) : ViewModel() {
    /** null until loaded. */
    val templates: StateFlow<List<TemplateWithExercises>?> = templates.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val inProgress: StateFlow<WorkoutSessionEntity?> = sessions.observeInProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** [templateId] null = empty quick workout. */
    fun start(templateId: Long?, onResult: (StartResult) -> Unit) {
        viewModelScope.launch {
            val existing = sessions.getInProgress()
            onResult(if (existing != null) StartResult.Blocked(existing) else StartResult.Started(create(templateId)))
        }
    }

    fun discardAndStart(discardId: Long, templateId: Long?, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            sessions.delete(discardId)
            onStarted(create(templateId))
        }
    }

    fun discard(sessionId: Long) {
        viewModelScope.launch { sessions.delete(sessionId) }
    }

    private suspend fun create(templateId: Long?): Long =
        if (templateId == null) sessions.startEmpty() else sessions.startFromTemplate(templateId)
}
