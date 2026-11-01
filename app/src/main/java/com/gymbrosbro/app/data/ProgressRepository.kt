package com.gymbrosbro.app.data

import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.records.ExerciseRecord
import com.gymbrosbro.app.domain.records.Records
import com.gymbrosbro.app.domain.schedule.Schedule
import com.gymbrosbro.app.domain.schedule.ScheduleData
import com.gymbrosbro.app.domain.schedule.ScheduleStreak
import com.gymbrosbro.app.domain.schedule.Streaks
import com.gymbrosbro.app.domain.schedule.WeeklyStreak
import com.gymbrosbro.app.domain.stats.Stats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.time.DayOfWeek
import java.time.LocalDate

data class StreakSummary(
    val schedule: ScheduleStreak = ScheduleStreak(),
    val weekly: WeeklyStreak = WeeklyStreak(),
    val hasSchedule: Boolean = false,
)

/**
 * Everything derived from the whole history — streaks, records, per-day
 * counts — computed once and shared by Home, Calendar and Stats.
 */
class ProgressRepository(
    sessions: SessionRepository,
    private val schedule: ScheduleRepository,
    private val settings: SettingsRepository,
    scope: CoroutineScope,
) {
    val completed: Flow<List<SessionWithExercises>> = sessions.observeCompleted()
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val workoutsByDay: Flow<Map<LocalDate, Int>> = completed
        .map { Stats.workoutsByDay(it) }
        .flowOn(Dispatchers.Default)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val records: Flow<List<ExerciseRecord>> = completed
        .map { Records.allEvents(it) }
        .flowOn(Dispatchers.Default)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val streaks: Flow<StreakSummary> = combine(
        workoutsByDay,
        schedule.observe(),
        settings.weekStart,
        settings.weeklyGoal,
    ) { days, data, weekStart, goal -> streaks(days, data, weekStart, goal, LocalDate.now()) }
        .flowOn(Dispatchers.Default)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    companion object {
        fun streaks(
            workoutsByDay: Map<LocalDate, Int>,
            data: ScheduleData,
            weekStart: DayOfWeek,
            goal: Int,
            today: LocalDate,
        ): StreakSummary {
            val first = data.firstDate
            val scheduleStreak = if (first == null || first > today) {
                ScheduleStreak()
            } else {
                Streaks.schedule(Schedule.occurrences(data, first, today), workoutsByDay.keys, today, weekStart)
            }
            val dates = workoutsByDay.flatMap { (day, n) -> List(n) { day } }
            return StreakSummary(
                schedule = scheduleStreak,
                weekly = Streaks.weekly(dates, goal, today, weekStart, data::isPaused),
                hasSchedule = data.rules.isNotEmpty(),
            )
        }
    }
}
