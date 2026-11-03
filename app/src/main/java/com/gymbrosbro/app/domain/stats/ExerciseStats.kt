package com.gymbrosbro.app.domain.stats

import com.gymbrosbro.app.data.db.ExercisePerformance
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.records.Performance
import com.gymbrosbro.app.domain.records.RecordHistory
import com.gymbrosbro.app.domain.records.RecordType
import com.gymbrosbro.app.domain.records.Records
import com.gymbrosbro.app.domain.schedule.Schedule
import com.gymbrosbro.app.domain.session.WorkoutMath
import com.gymbrosbro.app.domain.session.WorkoutTotals
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ChartPoint(val date: LocalDate, val value: Double)

/** The exercise detail screen's personal panel (SPEC §7.1). */
data class ExerciseSummary(
    val timesPerformed: Int,
    val lastPerformed: Instant?,
    val totals: WorkoutTotals,
    /** Best e1RM set for weighted lifts; most reps / longest / farthest otherwise. */
    val bestSet: SetEntryEntity?,
    /** Most reps in one working set, any weight. */
    val maxReps: Int?,
    val records: RecordHistory,
    /** Per workout: top weight, max reps, longest time or distance, by tracking type. */
    val progress: List<ChartPoint>,
    /** Per workout best e1RM; weighted lifts only. */
    val e1rmProgress: List<ChartPoint>,
    /** The last [ExerciseStats.WEEKS] weeks, oldest first: volume, or time / distance. */
    val weekly: List<ChartPoint>,
) {
    fun best(type: RecordType): Double? = records.bests[type]?.value
}

object ExerciseStats {
    const val WEEKS = 12

    fun summarize(
        exercise: ExerciseEntity,
        history: List<ExercisePerformance>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ExerciseSummary {
        val type = exercise.trackingType
        val performed = history
            .filter { p -> p.sets.any { it.isCompleted } }
            .sortedBy { it.session.startedAt }
        fun ExercisePerformance.date() = session.startedAt.atZone(zone).toLocalDate()

        val totals = performed.fold(WorkoutTotals()) { acc, p ->
            acc + WorkoutMath.totals(exercise, p.sets, p.session.bodyweightKgSnapshot)
        }
        val records = Records.history(type, performed.map { Performance(it.session.id, it.session.startedAt, it.sets) })

        val progressRecord = Records.setTypes(type).first()
        val progress = performed.mapNotNull { p ->
            Records.sessionValue(progressRecord, p.sets)?.let { ChartPoint(p.date(), it) }
        }
        val e1rm = if (type == TrackingType.WEIGHT_REPS) {
            performed.mapNotNull { p -> Records.sessionValue(RecordType.BEST_E1RM, p.sets)?.let { ChartPoint(p.date(), it) } }
        } else {
            emptyList()
        }

        val thisWeek = Schedule.weekStart(today, firstDayOfWeek)
        val weeks = (WEEKS - 1 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }
        val perWeek = performed.groupBy { Schedule.weekStart(it.date(), firstDayOfWeek) }
        val weekly = weeks.map { week ->
            val t = perWeek[week].orEmpty().fold(WorkoutTotals()) { acc, p ->
                acc + WorkoutMath.totals(exercise, p.sets, p.session.bodyweightKgSnapshot)
            }
            ChartPoint(week, weeklyValue(type, t))
        }

        val working = performed.flatMap { p -> p.sets.filter { it.isCompleted && !it.isWarmup } }
        return ExerciseSummary(
            timesPerformed = performed.size,
            lastPerformed = performed.lastOrNull()?.session?.startedAt,
            totals = totals,
            bestSet = bestSet(type, working),
            maxReps = working.mapNotNull { it.reps }.maxOrNull(),
            records = records,
            progress = progress,
            e1rmProgress = e1rm,
            weekly = weekly,
        )
    }

    /** What the weekly chart measures for this tracking type. */
    fun weeklyValue(type: TrackingType, totals: WorkoutTotals): Double = when (type) {
        TrackingType.WEIGHT_REPS -> totals.volumeKg
        TrackingType.BODYWEIGHT_REPS, TrackingType.REPS -> totals.reps.toDouble()
        TrackingType.DURATION -> totals.durationSec.toDouble()
        TrackingType.DISTANCE_DURATION, TrackingType.WEIGHT_DISTANCE -> totals.distanceM
    }

    private fun bestSet(type: TrackingType, sets: List<SetEntryEntity>): SetEntryEntity? = when (type) {
        TrackingType.WEIGHT_REPS ->
            sets.maxByOrNull { s -> s.weightKg?.let { w -> s.reps?.let { Records.e1rm(w, it) } } ?: -1.0 }
                ?.takeIf { s -> s.weightKg?.let { w -> s.reps?.let { Records.e1rm(w, it) } } != null }
                ?: sets.maxByOrNull { it.weightKg ?: 0.0 }
        TrackingType.BODYWEIGHT_REPS, TrackingType.REPS -> sets.maxByOrNull { it.reps ?: 0 }
        TrackingType.DURATION -> sets.maxByOrNull { it.durationSec ?: 0 }
        TrackingType.DISTANCE_DURATION -> sets.maxByOrNull { it.distanceM ?: 0.0 }
        TrackingType.WEIGHT_DISTANCE -> sets.maxByOrNull { it.weightKg ?: 0.0 }
    }
}
