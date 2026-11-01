package com.gymbrosbro.app.domain.stats

import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.domain.model.MuscleGroup
import com.gymbrosbro.app.domain.records.RecordEvent
import com.gymbrosbro.app.domain.schedule.Schedule
import com.gymbrosbro.app.domain.session.WorkoutMath
import com.gymbrosbro.app.domain.session.WorkoutTotals
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class StatsPeriod { WEEK, MONTH, YEAR, LIFETIME }

/** Inclusive date range. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate) = date >= start && date <= end
}

data class PeriodStats(
    val workouts: Int = 0,
    val totals: WorkoutTotals = WorkoutTotals(),
    val timeTrainedSec: Long = 0,
    val records: Int = 0,
    /** Working sets by primary muscle, most first. */
    val setsByMuscle: List<Pair<MuscleGroup, Int>> = emptyList(),
    /** Working sets by exercise, most first. */
    val exercises: List<Pair<ExerciseEntity, Int>> = emptyList(),
)

/** Totals for the Stats screen (SPEC §7.7). Everything is derived from history. */
object Stats {
    /** The [period] containing today, shifted back [offset] periods. null = lifetime. */
    fun range(period: StatsPeriod, offset: Int, today: LocalDate, firstDayOfWeek: DayOfWeek): DateRange? = when (period) {
        StatsPeriod.WEEK -> Schedule.weekStart(today, firstDayOfWeek).minusWeeks(offset.toLong())
            .let { DateRange(it, it.plusDays(6)) }
        StatsPeriod.MONTH -> today.withDayOfMonth(1).minusMonths(offset.toLong())
            .let { DateRange(it, it.with(TemporalAdjusters.lastDayOfMonth())) }
        StatsPeriod.YEAR -> today.withDayOfYear(1).minusYears(offset.toLong())
            .let { DateRange(it, it.with(TemporalAdjusters.lastDayOfYear())) }
        StatsPeriod.LIFETIME -> null
    }

    fun period(
        sessions: List<SessionWithExercises>,
        records: List<RecordEvent>,
        range: DateRange?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): PeriodStats {
        fun Instant.inRange() = range == null || atZone(zone).toLocalDate() in range
        val inRange = sessions.filter { it.session.endedAt != null && it.session.startedAt.inRange() }
        val muscles = mutableMapOf<MuscleGroup, Int>()
        val exercises = mutableMapOf<String, Pair<ExerciseEntity, Int>>()
        var totals = WorkoutTotals()
        var time = 0L
        for (s in inRange) {
            totals += WorkoutMath.totals(s.exercises.map { it.exercise to it.sets }, s.session.bodyweightKgSnapshot)
            time += Duration.between(s.session.startedAt, s.session.endedAt).seconds.coerceAtLeast(0)
            for (e in s.exercises) {
                val working = e.sets.count { it.isCompleted && !it.isWarmup }
                if (working > 0) {
                    muscles.merge(e.exercise.primaryMuscle, working, Int::plus)
                    exercises[e.exercise.id] = e.exercise to (exercises[e.exercise.id]?.second ?: 0) + working
                }
            }
        }
        return PeriodStats(
            workouts = inRange.size,
            totals = totals,
            timeTrainedSec = time,
            records = records.count { it.at.inRange() },
            setsByMuscle = muscles.entries.sortedByDescending { it.value }.map { it.key to it.value },
            exercises = exercises.values.sortedByDescending { it.second },
        )
    }

    /** Workouts per day, for the heatmap and calendar. */
    fun workoutsByDay(sessions: List<SessionWithExercises>, zone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, Int> =
        sessions.groupingBy { it.session.startedAt.atZone(zone).toLocalDate() }.eachCount()
}
