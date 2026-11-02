package com.gymbrosbro.app.data

import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.progress.Lifetime
import com.gymbrosbro.app.domain.progress.Xp
import com.gymbrosbro.app.domain.progress.XpSummary
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

data class StreakSummary(
    val schedule: ScheduleStreak = ScheduleStreak(),
    val weekly: WeeklyStreak = WeeklyStreak(),
    val hasSchedule: Boolean = false,
)

/** Everything derived from the whole history at once. */
data class Progress(
    val records: List<ExerciseRecord> = emptyList(),
    val streaks: StreakSummary = StreakSummary(),
    val xp: XpSummary = XpSummary(),
    val lifetime: Lifetime = Lifetime(),
)

/**
 * Everything derived from the whole history — streaks, records, XP, per-day
 * counts — computed once and shared by Home, Calendar, Stats and the locker.
 */
class ProgressRepository(
    private val sessions: SessionRepository,
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

    val progress: Flow<Progress> = combine(
        completed,
        schedule.observe(),
        settings.weekStart,
        settings.weeklyGoal,
    ) { done, data, weekStart, goal -> compute(done, data, weekStart, goal, LocalDate.now()) }
        .flowOn(Dispatchers.Default)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val records: Flow<List<ExerciseRecord>> = progress.map { it.records }
    val streaks: Flow<StreakSummary> = progress.map { it.streaks }
    val xp: Flow<XpSummary> = progress.map { it.xp }

    /** Fresh from the database, bypassing the shared flows that may lag a just-finished workout. */
    suspend fun current(): Progress {
        val done = sessions.observeCompleted().first()
        val data = schedule.get()
        val weekStart = settings.weekStart.first()
        val goal = settings.weeklyGoal.first()
        return withContext(Dispatchers.Default) { compute(done, data, weekStart, goal, LocalDate.now()) }
    }

    companion object {
        fun compute(
            completed: List<SessionWithExercises>,
            data: ScheduleData,
            weekStart: DayOfWeek,
            goal: Int,
            today: LocalDate,
        ): Progress {
            val streaks = streaks(Stats.workoutsByDay(completed), data, weekStart, goal, today)
            val records = Records.allEvents(completed)
            val first = data.firstDate
            val scheduledDays = if (first == null || first > today) {
                emptySet()
            } else {
                Schedule.occurrences(data, first, today).filter { it.active }.map { it.date }.toSet()
            }
            return Progress(
                records = records,
                streaks = streaks,
                xp = Xp.perSession(completed, records.map { it.event }, scheduledDays, goal, weekStart),
                lifetime = Lifetime.of(completed, records.size, streaks.weekly.best, streaks.schedule.best),
            )
        }

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
