package com.gymbrosbro.app.domain.progress

import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.domain.records.RecordEvent
import com.gymbrosbro.app.domain.schedule.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** What one workout earned (SPEC §7.9). */
data class SessionXp(
    val sessionId: Long,
    val workout: Int = Xp.WORKOUT,
    val sets: Int = 0,
    val records: Int = 0,
    /** The first workout on a scheduled day. */
    val scheduled: Boolean = false,
    /** The workout that reached the weekly goal. */
    val weeklyGoal: Boolean = false,
) {
    val total: Int
        get() = workout + sets * Xp.SET + records * Xp.RECORD +
            (if (scheduled) Xp.SCHEDULED else 0) + (if (weeklyGoal) Xp.WEEKLY_GOAL else 0)
}

data class Level(val level: Int, val xpIntoLevel: Int, val xpForNext: Int) {
    val progress: Float get() = xpIntoLevel.toFloat() / xpForNext
}

data class XpSummary(val sessions: Map<Long, SessionXp> = emptyMap()) {
    val total: Int = sessions.values.sumOf { it.total }
    val level: Level = Xp.level(total)

    /** The level before [sessionId] was logged, for LEVEL UP! on the finish screen. */
    fun levelBefore(sessionId: Long): Level = Xp.level(total - (sessions[sessionId]?.total ?: 0))
}

/**
 * XP is derived from history like everything else, so editing an old workout
 * corrects it. Unlocks and trophies earned along the way are kept regardless.
 */
object Xp {
    const val WORKOUT = 100
    const val SET = 10
    const val RECORD = 50
    const val SCHEDULED = 25
    const val WEEKLY_GOAL = 100

    /** Level n needs 250 × (n − 1) more XP than level n − 1, so reaching level n takes 125·n·(n − 1). */
    fun xpToReach(level: Int): Int = 125 * level * (level - 1)

    fun level(xp: Int): Level {
        var level = 1
        while (xpToReach(level + 1) <= xp) level++
        val floor = xpToReach(level)
        return Level(level, xp - floor, xpToReach(level + 1) - floor)
    }

    /**
     * @param scheduledDays days with an active scheduled workout
     * @param weeklyGoal workouts per week for the weekly-goal bonus
     */
    fun perSession(
        sessions: List<SessionWithExercises>,
        records: List<RecordEvent>,
        scheduledDays: Set<LocalDate>,
        weeklyGoal: Int,
        weekStart: DayOfWeek,
        zone: ZoneId = ZoneId.systemDefault(),
    ): XpSummary {
        val recordsBySession = records.groupingBy { it.sessionId }.eachCount()
        val daysCredited = mutableSetOf<LocalDate>()
        val perWeek = mutableMapOf<LocalDate, Int>()
        val result = linkedMapOf<Long, SessionXp>()
        for (s in sessions.filter { it.session.endedAt != null }.sortedBy { it.session.startedAt }) {
            val day = s.session.startedAt.atZone(zone).toLocalDate()
            val week = Schedule.weekStart(day, weekStart)
            val count = perWeek.merge(week, 1, Int::plus)!!
            result[s.session.id] = SessionXp(
                sessionId = s.session.id,
                sets = s.exercises.sumOf { e -> e.sets.count { it.isCompleted && !it.isWarmup } },
                records = recordsBySession[s.session.id] ?: 0,
                scheduled = day in scheduledDays && daysCredited.add(day),
                weeklyGoal = count == weeklyGoal,
            )
        }
        return XpSummary(result)
    }
}
