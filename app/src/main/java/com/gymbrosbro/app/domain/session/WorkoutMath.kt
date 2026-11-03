package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.domain.model.TrackingType

data class WorkoutTotals(
    val sets: Int = 0,
    val reps: Int = 0,
    val volumeKg: Double = 0.0,
    val durationSec: Int = 0,
    val distanceM: Double = 0.0,
) {
    operator fun plus(other: WorkoutTotals) = WorkoutTotals(
        sets = sets + other.sets,
        reps = reps + other.reps,
        volumeKg = volumeKg + other.volumeKg,
        durationSec = durationSec + other.durationSec,
        distanceM = distanceM + other.distanceM,
    )
}

/**
 * How sets turn into totals (SPEC §3, §4.2). Only completed sets count;
 * warm-ups count like any other set.
 */
object WorkoutMath {
    /** Whether a set has the numbers its tracking type needs to be checked off. */
    fun canComplete(type: TrackingType, set: SetEntryEntity): Boolean = when (type) {
        TrackingType.WEIGHT_REPS -> set.weightKg != null && (set.reps ?: 0) > 0
        TrackingType.BODYWEIGHT_REPS, TrackingType.REPS -> (set.reps ?: 0) > 0
        TrackingType.DURATION -> (set.durationSec ?: 0) > 0
        TrackingType.DISTANCE_DURATION -> (set.distanceM ?: 0.0) > 0.0 || (set.durationSec ?: 0) > 0
        TrackingType.WEIGHT_DISTANCE -> set.weightKg != null && (set.distanceM ?: 0.0) > 0.0
    }

    /**
     * Weight moved by one set. Dumbbell weights are per implement, so a pair
     * of 15s counts as 15 (SPEC §3). Bodyweight sets need a bodyweight to count.
     */
    fun setVolumeKg(exercise: ExerciseEntity, set: SetEntryEntity, bodyweightKg: Double?): Double {
        val reps = set.reps ?: return 0.0
        return when (exercise.trackingType) {
            TrackingType.WEIGHT_REPS -> (set.weightKg ?: 0.0) * reps
            TrackingType.BODYWEIGHT_REPS -> {
                val base = if (exercise.bodyweightFactor != null && bodyweightKg != null) {
                    bodyweightKg * exercise.bodyweightFactor
                } else {
                    0.0
                }
                ((base + (set.addedWeightKg ?: 0.0)) * reps).coerceAtLeast(0.0)
            }
            else -> 0.0
        }
    }

    fun totals(exercise: ExerciseEntity, sets: List<SetEntryEntity>, bodyweightKg: Double?): WorkoutTotals {
        val done = sets.filter { it.isCompleted }
        val countsReps = exercise.trackingType == TrackingType.WEIGHT_REPS ||
            exercise.trackingType == TrackingType.BODYWEIGHT_REPS ||
            exercise.trackingType == TrackingType.REPS
        return WorkoutTotals(
            sets = done.size,
            reps = if (countsReps) done.sumOf { it.reps ?: 0 } else 0,
            volumeKg = done.sumOf { setVolumeKg(exercise, it, bodyweightKg) },
            durationSec = done.sumOf { it.durationSec ?: 0 },
            distanceM = done.sumOf { it.distanceM ?: 0.0 },
        )
    }

    fun totals(items: List<Pair<ExerciseEntity, List<SetEntryEntity>>>, bodyweightKg: Double?): WorkoutTotals =
        items.fold(WorkoutTotals()) { acc, (exercise, sets) -> acc + totals(exercise, sets, bodyweightKg) }
}
