package com.gymbrosbro.app.domain.schedule

import com.gymbrosbro.app.data.db.entity.ScheduleExceptionEntity
import com.gymbrosbro.app.data.db.entity.SchedulePauseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleRuleEntity
import com.gymbrosbro.app.domain.model.ScheduleAction
import com.gymbrosbro.app.domain.model.ScheduleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate

/** Shared builders: Monday 2026-11-02 starts the test calendar. */
object ScheduleFixtures {
    val MON: LocalDate = LocalDate.of(2026, 11, 2)

    fun mwf(start: LocalDate = MON, id: Long = 1, templateId: Long? = 10) = ScheduleRuleEntity(
        id = id,
        templateId = templateId,
        kind = ScheduleKind.WEEKLY,
        daysOfWeek = Schedule.dayBit(MONDAY) or Schedule.dayBit(WEDNESDAY) or Schedule.dayBit(FRIDAY),
        startDate = start,
        createdAt = Instant.EPOCH,
    )

    fun data(vararg rules: ScheduleRuleEntity, exceptions: List<ScheduleExceptionEntity> = emptyList(), pauses: List<SchedulePauseEntity> = emptyList()) =
        ScheduleData(rules.toList(), exceptions, pauses)

    fun day(offset: Long): LocalDate = MON.plusDays(offset)
}

class ScheduleTest {
    private val f = ScheduleFixtures

    @Test
    fun `weekly rule lands on its weekdays from its start date`() {
        val dates = Schedule.occurrences(f.data(f.mwf(start = f.day(2))), f.MON, f.day(13)).map { it.date }
        assertEquals(listOf(f.day(2), f.day(4), f.day(7), f.day(9), f.day(11)), dates)
    }

    @Test
    fun `ended rules stop`() {
        val rule = f.mwf().copy(endDate = f.day(3))
        assertEquals(listOf(f.MON, f.day(2)), Schedule.occurrences(f.data(rule), f.MON, f.day(13)).map { it.date })
    }

    @Test
    fun `every N days counts from the start date`() {
        val rule = ScheduleRuleEntity(id = 1, templateId = null, kind = ScheduleKind.EVERY_N_DAYS, intervalDays = 3, startDate = f.day(1), createdAt = Instant.EPOCH)
        assertEquals(listOf(f.day(1), f.day(4), f.day(7)), Schedule.occurrences(f.data(rule), f.MON, f.day(8)).map { it.date })
    }

    @Test
    fun `one-off is a single day`() {
        val rule = ScheduleRuleEntity(id = 1, templateId = null, kind = ScheduleKind.ONE_OFF, startDate = f.day(5), createdAt = Instant.EPOCH)
        assertEquals(listOf(f.day(5)), Schedule.occurrences(f.data(rule), f.MON, f.day(30)).map { it.date })
    }

    @Test
    fun `moves land on the new day, including from outside the range`() {
        val move = ScheduleExceptionEntity(ruleId = 1, originalDate = f.MON, action = ScheduleAction.MOVE, newDate = f.day(1))
        val occ = Schedule.occurrences(f.data(f.mwf(), exceptions = listOf(move)), f.day(1), f.day(1)).single()
        assertEquals(f.MON, occ.originalDate)
        assertTrue(occ.moved)
    }

    @Test
    fun `skips and pauses are flagged`() {
        val skip = ScheduleExceptionEntity(ruleId = 1, originalDate = f.MON, action = ScheduleAction.SKIP)
        val pause = SchedulePauseEntity(startDate = f.day(4), endDate = f.day(4))
        val occ = Schedule.occurrences(f.data(f.mwf(), exceptions = listOf(skip), pauses = listOf(pause)), f.MON, f.day(4))
        assertTrue(occ[0].skipped)
        assertTrue(occ[1].active)
        assertTrue(occ[2].paused)
    }

    @Test
    fun `reminder fires on a scheduled day until you train`() {
        val data = f.data(f.mwf())
        assertTrue(Reminders.decide(f.MON, data, emptySet(), graceEnabled = false) is Reminder.WorkoutDay)
        assertNull(Reminders.decide(f.MON, data, setOf(f.MON), graceEnabled = false))
        assertNull(Reminders.decide(f.day(1), data, emptySet(), graceEnabled = false))
    }

    @Test
    fun `grace reminder only when enabled and yesterday was missed`() {
        val data = f.data(f.mwf())
        assertEquals(Reminder.GraceDay(f.MON), Reminders.decide(f.day(1), data, emptySet(), graceEnabled = true))
        assertNull(Reminders.decide(f.day(1), data, setOf(f.MON), graceEnabled = true))
    }
}
