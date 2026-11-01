package com.gymbrosbro.app.domain.schedule

import java.time.DayOfWeek
import java.time.LocalDate

data class ScheduleStreak(
    val current: Int = 0,
    val best: Int = 0,
    /** Banked streak freezes, 0–[Streaks.MAX_FREEZES]. */
    val freezes: Int = 0,
    /** A missed day whose grace day is today: train today to keep the streak. */
    val atRisk: LocalDate? = null,
)

data class WeeklyStreak(
    val current: Int = 0,
    val best: Int = 0,
    val thisWeek: Int = 0,
    val goal: Int = 3,
)

/**
 * Streaks (SPEC §7.6), recomputed from scratch from the schedule and logged
 * workouts every time, so edits and backfills just work. Scheduled days are
 * the unit: a day counts if any workout is logged on it.
 */
object Streaks {
    const val WEEKS_PER_FREEZE = 4
    const val MAX_FREEZES = 2

    /**
     * The schedule streak ("COMBO"). [occurrences] should cover every day from
     * the first rule up to [today].
     *
     * - Done: a workout on the day.
     * - Grace: missed, but a workout the next day makes it up — unless the next
     *   day has its own scheduled workout.
     * - Otherwise a banked freeze covers it (skips always use one), or the
     *   streak resets.
     * - Freezes are earned for every 4 weeks in a row with every scheduled day
     *   done, up to 2.
     */
    fun schedule(
        occurrences: List<Occurrence>,
        workoutDays: Set<LocalDate>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
    ): ScheduleStreak {
        val byDay = occurrences.filter { !it.paused && it.date <= today }.groupBy { it.date }
        val scheduled = byDay.filterValues { list -> list.any { it.active } }.keys

        var current = 0
        var best = 0
        var freezes = 0
        var perfectWeeks = 0
        var atRisk: LocalDate? = null
        var week: LocalDate? = null
        var weekPerfect = true

        fun closeWeek() {
            if (week == null) return
            if (weekPerfect) {
                if (++perfectWeeks == WEEKS_PER_FREEZE) {
                    freezes = minOf(MAX_FREEZES, freezes + 1)
                    perfectWeeks = 0
                }
            } else {
                perfectWeeks = 0
            }
        }

        fun done() {
            current++
            best = maxOf(best, current)
        }

        fun missed() {
            weekPerfect = false
            if (freezes > 0) freezes-- else current = 0
        }

        for (day in byDay.keys.sorted()) {
            val dayWeek = Schedule.weekStart(day, firstDayOfWeek)
            if (dayWeek != week) {
                closeWeek()
                week = dayWeek
                weekPerfect = true
            }
            val next = day.plusDays(1)
            val graceAvailable = next !in scheduled
            when {
                day in workoutDays -> done()
                day !in scheduled -> missed() // every occurrence that day was skipped
                day == today -> Unit // still time today
                graceAvailable && next in workoutDays -> done()
                graceAvailable && next == today -> atRisk = day
                else -> missed()
            }
        }
        if (week != null && week != Schedule.weekStart(today, firstDayOfWeek)) closeWeek()
        return ScheduleStreak(current, best, freezes, atRisk)
    }

    /**
     * Weeks in a row with at least [goal] workouts. This week only adds once
     * the goal is hit and never breaks the streak before it ends. A week that
     * overlaps a pause and misses the goal is skipped over, not a break.
     */
    fun weekly(
        workoutDates: List<LocalDate>,
        goal: Int,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
        isPaused: (LocalDate) -> Boolean = { false },
    ): WeeklyStreak {
        val counts = workoutDates.filter { it <= today }.groupingBy { Schedule.weekStart(it, firstDayOfWeek) }.eachCount()
        val thisWeek = Schedule.weekStart(today, firstDayOfWeek)
        val thisWeekCount = counts[thisWeek] ?: 0
        val first = counts.keys.minOrNull() ?: return WeeklyStreak(goal = goal)

        var run = 0
        var best = 0
        var week = first
        while (week < thisWeek) {
            if ((counts[week] ?: 0) >= goal) {
                run++
                best = maxOf(best, run)
            } else if ((0L..6L).none { isPaused(week.plusDays(it)) }) {
                run = 0
            }
            week = week.plusWeeks(1)
        }
        val current = if (thisWeekCount >= goal) run + 1 else run
        return WeeklyStreak(current, maxOf(best, current), thisWeekCount, goal)
    }
}
