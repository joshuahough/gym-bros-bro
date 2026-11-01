package com.gymbrosbro.app.domain.schedule

import com.gymbrosbro.app.data.db.entity.ScheduleExceptionEntity
import com.gymbrosbro.app.data.db.entity.SchedulePauseEntity
import com.gymbrosbro.app.domain.model.ScheduleAction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate

class StreaksTest {
    private val f = ScheduleFixtures

    private fun streak(data: ScheduleData, workouts: Set<LocalDate>, today: LocalDate) =
        Streaks.schedule(Schedule.occurrences(data, data.firstDate!!, today), workouts, today, MONDAY)

    /** Every Mon/Wed/Fri from MON for [weeks] weeks. */
    private fun mwfDays(weeks: Int) = (0 until weeks).flatMap { w -> listOf(0L, 2L, 4L).map { f.day(w * 7 + it) } }.toSet()

    @Test
    fun `counts scheduled days done`() {
        val s = streak(f.data(f.mwf()), setOf(f.MON, f.day(2), f.day(4)), today = f.day(5))
        assertEquals(3, s.current)
        assertEquals(3, s.best)
    }

    @Test
    fun `today not done yet doesn't break it`() {
        val s = streak(f.data(f.mwf()), setOf(f.MON), today = f.day(2))
        assertEquals(1, s.current)
    }

    @Test
    fun `unscheduled workouts don't extend it`() {
        val s = streak(f.data(f.mwf()), setOf(f.MON, f.day(1)), today = f.day(1))
        assertEquals(1, s.current)
    }

    @Test
    fun `a miss resets the streak, best is kept`() {
        val s = streak(f.data(f.mwf()), setOf(f.MON, f.day(2), f.day(7)), today = f.day(8))
        assertEquals(1, s.current)
        assertEquals(2, s.best)
    }

    @Test
    fun `grace day makes up a miss`() {
        // Missed Monday, trained Tuesday.
        val s = streak(f.data(f.mwf()), setOf(f.day(1), f.day(2)), today = f.day(2))
        assertEquals(2, s.current)
    }

    @Test
    fun `grace day still in progress is at risk, not broken`() {
        val s = streak(f.data(f.mwf()), emptySet(), today = f.day(1))
        assertEquals(0, s.current)
        assertEquals(f.MON, s.atRisk)
    }

    @Test
    fun `no grace when the next day is scheduled too`() {
        // Daily schedule: missing Monday can't be made up Tuesday, Tuesday's workout is Tuesday's.
        val daily = f.mwf().copy(daysOfWeek = 0b1111111)
        val s = streak(f.data(daily), setOf(f.day(1), f.day(2)), today = f.day(2))
        assertEquals(2, s.current)
        assertEquals(2, s.best)
        val broken = streak(f.data(daily), setOf(f.day(1)), today = f.day(1))
        assertEquals(1, broken.current)
    }

    @Test
    fun `moved occurrences count on their new day`() {
        val move = ScheduleExceptionEntity(ruleId = 1, originalDate = f.MON, action = ScheduleAction.MOVE, newDate = f.day(1))
        val s = streak(f.data(f.mwf(), exceptions = listOf(move)), setOf(f.day(1), f.day(2)), today = f.day(3))
        assertEquals(2, s.current)
    }

    @Test
    fun `paused days are frozen`() {
        val pause = SchedulePauseEntity(startDate = f.day(7), endDate = f.day(13))
        val s = streak(f.data(f.mwf(), pauses = listOf(pause)), mwfDays(1) + f.day(14), today = f.day(14))
        assertEquals(4, s.current)
    }

    @Test
    fun `four perfect weeks earn a freeze that covers a miss`() {
        val done = mwfDays(4)
        val earned = streak(f.data(f.mwf()), done, today = f.day(28))
        assertEquals(1, earned.freezes)

        // Week 5: miss Monday (and Tuesday, no make-up), train Wednesday.
        val covered = streak(f.data(f.mwf()), done + f.day(30), today = f.day(30))
        assertEquals(13, covered.current)
        assertEquals(0, covered.freezes)
    }

    @Test
    fun `skips use a freeze, or reset without one`() {
        val skip = ScheduleExceptionEntity(ruleId = 1, originalDate = f.day(2), action = ScheduleAction.SKIP)
        val s = streak(f.data(f.mwf(), exceptions = listOf(skip)), setOf(f.MON, f.day(4)), today = f.day(4))
        assertEquals(1, s.current)
    }

    @Test
    fun `freezes cap at two`() {
        val s = streak(f.data(f.mwf()), mwfDays(16), today = f.day(16 * 7))
        assertEquals(Streaks.MAX_FREEZES, s.freezes)
    }

    @Test
    fun `weekly streak counts weeks that hit the goal`() {
        val workouts = mwfDays(3).toList() + f.day(21)
        val s = Streaks.weekly(workouts, goal = 3, today = f.day(22), firstDayOfWeek = MONDAY)
        assertEquals(3, s.current) // this week (1 so far) doesn't count yet, but doesn't break it
        assertEquals(1, s.thisWeek)
    }

    @Test
    fun `weekly streak breaks on a short week, skips paused ones`() {
        val workouts = mwfDays(1).toList() + listOf(f.day(7)) + mwfDays(3).filter { it >= f.day(14) }
        val broken = Streaks.weekly(workouts, goal = 3, today = f.day(25), firstDayOfWeek = MONDAY)
        assertEquals(1, broken.current)
        assertEquals(1, broken.best)

        val paused = Streaks.weekly(workouts, 3, f.day(25), MONDAY) { it == f.day(9) }
        assertEquals(2, paused.current)
    }
}
