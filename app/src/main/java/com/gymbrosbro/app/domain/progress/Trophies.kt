package com.gymbrosbro.app.domain.progress

import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.domain.session.WorkoutMath

/** Lifetime numbers that trophies are measured against. */
data class Lifetime(
    val workouts: Int = 0,
    val records: Int = 0,
    val volumeKg: Double = 0.0,
    val distanceM: Double = 0.0,
    val pullUps: Int = 0,
    val bestWeeklyStreak: Int = 0,
    val bestCombo: Int = 0,
) {
    fun value(metric: TrophyMetric): Double = when (metric) {
        TrophyMetric.WORKOUTS -> workouts.toDouble()
        TrophyMetric.RECORDS -> records.toDouble()
        TrophyMetric.VOLUME_KG -> volumeKg
        TrophyMetric.DISTANCE_M -> distanceM
        TrophyMetric.PULL_UPS -> pullUps.toDouble()
        TrophyMetric.WEEKLY_STREAK -> bestWeeklyStreak.toDouble()
        TrophyMetric.COMBO -> bestCombo.toDouble()
    }

    companion object {
        /** Pull-up and chin-up variations that count toward the pull-up trophy. */
        val PULL_UP_IDS = setOf(
            "pullups", "chin-up", "weighted-pull-ups", "band-assisted-pull-up", "wide-grip-rear-pull-up",
            "v-bar-pullup", "one-arm-chin-up", "mixed-grip-chin",
        )

        fun of(
            sessions: List<SessionWithExercises>,
            records: Int,
            bestWeeklyStreak: Int,
            bestCombo: Int,
        ): Lifetime {
            val done = sessions.filter { it.session.endedAt != null }
            var volume = 0.0
            var distance = 0.0
            var pullUps = 0
            for (s in done) {
                val totals = WorkoutMath.totals(s.exercises.map { it.exercise to it.sets }, s.session.bodyweightKgSnapshot)
                volume += totals.volumeKg
                distance += totals.distanceM
                pullUps += s.exercises.filter { it.exercise.id in PULL_UP_IDS }
                    .sumOf { e -> e.sets.filter { it.isCompleted && !it.isWarmup }.sumOf { it.reps ?: 0 } }
            }
            return Lifetime(done.size, records, volume, distance, pullUps, bestWeeklyStreak, bestCombo)
        }
    }
}

enum class TrophyMetric { WORKOUTS, RECORDS, VOLUME_KG, DISTANCE_M, PULL_UPS, WEEKLY_STREAK, COMBO }

/** An achievement (SPEC §7.9). Ids are stored, so never change one. */
data class Trophy(val id: String, val name: String, val metric: TrophyMetric, val target: Double)

object Trophies {
    private const val LB = 0.45359237

    val all: List<Trophy> = listOf(
        Trophy("workouts-1", "PLAYER 1 READY", TrophyMetric.WORKOUTS, 1.0),
        Trophy("workouts-10", "REGULAR", TrophyMetric.WORKOUTS, 10.0),
        Trophy("workouts-50", "GYM RAT", TrophyMetric.WORKOUTS, 50.0),
        Trophy("workouts-100", "CENTURION", TrophyMetric.WORKOUTS, 100.0),
        Trophy("workouts-250", "IRON VETERAN", TrophyMetric.WORKOUTS, 250.0),
        Trophy("workouts-500", "LEGEND", TrophyMetric.WORKOUTS, 500.0),
        Trophy("weekly-4", "ON A ROLL", TrophyMetric.WEEKLY_STREAK, 4.0),
        Trophy("weekly-12", "QUARTER CHAMP", TrophyMetric.WEEKLY_STREAK, 12.0),
        Trophy("weekly-26", "HALF-YEAR HERO", TrophyMetric.WEEKLY_STREAK, 26.0),
        Trophy("weekly-52", "YEAR OF IRON", TrophyMetric.WEEKLY_STREAK, 52.0),
        Trophy("combo-10", "COMBO ×10", TrophyMetric.COMBO, 10.0),
        Trophy("combo-25", "COMBO ×25", TrophyMetric.COMBO, 25.0),
        Trophy("combo-50", "COMBO ×50", TrophyMetric.COMBO, 50.0),
        Trophy("records-1", "HIGH SCORE", TrophyMetric.RECORDS, 1.0),
        Trophy("records-25", "SCORE CHASER", TrophyMetric.RECORDS, 25.0),
        Trophy("records-100", "TOP OF THE BOARD", TrophyMetric.RECORDS, 100.0),
        Trophy("volume-100k", "100K CLUB", TrophyMetric.VOLUME_KG, 100_000 * LB),
        Trophy("volume-1m", "MILLION CLUB", TrophyMetric.VOLUME_KG, 1_000_000 * LB),
        Trophy("pull-ups-1000", "BAR BOSS", TrophyMetric.PULL_UPS, 1000.0),
        Trophy("distance-marathon", "MARATHONER", TrophyMetric.DISTANCE_M, 42_195.0),
    )

    fun byId(id: String): Trophy? = all.firstOrNull { it.id == id }

    fun reached(lifetime: Lifetime): List<Trophy> = all.filter { lifetime.value(it.metric) >= it.target - 1e-6 }
}
