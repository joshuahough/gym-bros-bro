package com.gymbrosbro.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.ProgressRepository
import com.gymbrosbro.app.data.ScheduleRepository
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.StreakSummary
import com.gymbrosbro.app.data.TemplateRepository
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.schedule.DayStatus
import com.gymbrosbro.app.domain.schedule.Occurrence
import com.gymbrosbro.app.domain.schedule.Schedule
import com.gymbrosbro.app.ui.localDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDay(val date: LocalDate, val inMonth: Boolean, val status: DayStatus)

/** A planned workout on the selected day, with its name resolved. */
data class PlannedWorkout(val occurrence: Occurrence, val name: String)

data class SelectedDay(
    val date: LocalDate,
    val planned: List<PlannedWorkout>,
    val workouts: List<WorkoutSessionEntity>,
    val paused: Boolean,
)

data class CalendarUiState(
    val month: YearMonth,
    val weekStart: DayOfWeek,
    /** Whole weeks covering [month]. */
    val days: List<CalendarDay>,
    val selected: SelectedDay,
    val streaks: StreakSummary,
    val hasSchedule: Boolean,
)

class CalendarViewModel(
    private val schedule: ScheduleRepository,
    private val sessions: SessionRepository,
    progress: ProgressRepository,
    templates: TemplateRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val state: StateFlow<CalendarUiState?> = combine(
        combine(schedule.observe(), progress.completed, templates.observeAll(), ::Triple),
        progress.streaks,
        settings.weekStart,
        month,
        selectedDate,
    ) { (data, completed, allTemplates), streaks, weekStart, month, selected ->
        val today = LocalDate.now()
        val gridStart = Schedule.weekStart(month.atDay(1), weekStart)
        val gridEnd = Schedule.weekStart(month.atEndOfMonth(), weekStart).plusDays(6)
        val occurrences = Schedule.occurrences(data, minOf(gridStart, selected), maxOf(gridEnd, selected)).groupBy { it.date }
        val workouts = completed.map { it.session }.groupBy { it.startedAt.localDate() }
        val names = allTemplates.associate { it.id to it.name }

        val days = generateSequence(gridStart) { it.plusDays(1) }.takeWhile { it <= gridEnd }.map { date ->
            val status = DayStatus.of(date, today, occurrences[date].orEmpty(), workouts.containsKey(date), data.isPaused(date))
            CalendarDay(date, YearMonth.from(date) == month, status)
        }.toList()

        CalendarUiState(
            month = month,
            weekStart = weekStart,
            days = days,
            selected = SelectedDay(
                date = selected,
                planned = occurrences[selected].orEmpty().map { o ->
                    PlannedWorkout(o, o.templateId?.let { names[it] } ?: "Any workout")
                },
                workouts = workouts[selected].orEmpty(),
                paused = data.isPaused(selected),
            ),
            streaks = streaks,
            hasSchedule = data.rules.isNotEmpty(),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun showMonth(delta: Int) {
        month.value = month.value.plusMonths(delta.toLong())
    }

    fun select(date: LocalDate) {
        selectedDate.value = date
        if (YearMonth.from(date) != month.value) month.value = YearMonth.from(date)
    }

    fun skip(occurrence: Occurrence) = viewModelScope.launch { schedule.skip(occurrence) }

    fun restore(occurrence: Occurrence) = viewModelScope.launch { schedule.restore(occurrence) }

    fun move(occurrence: Occurrence, date: LocalDate) = viewModelScope.launch {
        schedule.move(occurrence, date)
        select(date)
    }

    /** A past day with nothing logged: record what you did. */
    fun logPastWorkout(date: LocalDate, onCreated: (Long) -> Unit) = viewModelScope.launch {
        onCreated(sessions.createBackfill(date))
    }
}
