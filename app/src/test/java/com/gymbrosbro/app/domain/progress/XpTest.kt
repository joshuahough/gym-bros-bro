package com.gymbrosbro.app.domain.progress

import com.gymbrosbro.app.TestData
import com.gymbrosbro.app.TestData.set
import com.gymbrosbro.app.data.db.SessionExerciseWithSets
import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SessionExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.domain.model.SessionStatus
import com.gymbrosbro.app.domain.records.RecordEvent
import com.gymbrosbro.app.domain.records.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

/** Builds finished workouts for progress tests. */
object Workouts {
    val MON: LocalDate = LocalDate.of(2026, 11, 2)

    fun workout(
        id: Long,
        day: LocalDate,
        vararg sets: SetEntryEntity,
        exercise: ExerciseEntity = TestData.exercise(),
        finished: Boolean = true,
    ): SessionWithExercises {
        val start = day.atTime(7, 0).toInstant(ZoneOffset.UTC)
        val session = WorkoutSessionEntity(
            id = id,
            templateId = null,
            name = "W$id",
            startedAt = start,
            endedAt = if (finished) start.plusSeconds(3600) else null,
            status = if (finished) SessionStatus.COMPLETED else SessionStatus.IN_PROGRESS,
        )
        val se = SessionExerciseEntity(id = id, sessionId = id, exerciseId = exercise.id, position = 0)
        return SessionWithExercises(session, listOf(SessionExerciseWithSets(se, exercise, sets.toList())))
    }
}

class XpTest {
    private val mon = Workouts.MON

    private fun perSession(
        sessions: List<SessionWithExercises>,
        records: List<RecordEvent> = emptyList(),
        scheduled: Set<LocalDate> = emptySet(),
        goal: Int = 3,
    ) = Xp.perSession(sessions, records, scheduled, goal, DayOfWeek.MONDAY, ZoneOffset.UTC)

    @Test
    fun `levels cost 250 more XP each`() {
        assertEquals(0, Xp.xpToReach(1))
        assertEquals(250, Xp.xpToReach(2))
        assertEquals(750, Xp.xpToReach(3))
        assertEquals(Level(1, 249, 250), Xp.level(249))
        assertEquals(Level(2, 0, 500), Xp.level(250))
        assertEquals(Level(3, 50, 750), Xp.level(800))
    }

    @Test
    fun `workout and working sets earn XP, warm-ups and unchecked sets don't`() {
        val xp = perSession(
            listOf(
                Workouts.workout(
                    1, mon,
                    set(0, weightKg = 20.0, reps = 10, warmup = true),
                    set(1, weightKg = 60.0, reps = 8),
                    set(2, weightKg = 60.0, reps = 8),
                    set(3, weightKg = 60.0, reps = 8, completed = false),
                ),
            ),
        )
        assertEquals(Xp.WORKOUT + 2 * Xp.SET, xp.total)
    }

    @Test
    fun `unfinished workouts earn nothing`() {
        assertEquals(0, perSession(listOf(Workouts.workout(1, mon, set(0, weightKg = 60.0, reps = 8), finished = false))).total)
    }

    @Test
    fun `records earn XP for their own workout`() {
        val records = listOf(
            RecordEvent(RecordType.MAX_WEIGHT, 100.0, 90.0, sessionId = 2, at = mon.atStartOfDay().toInstant(ZoneOffset.UTC)),
            RecordEvent(RecordType.BEST_E1RM, 110.0, 100.0, sessionId = 2, at = mon.atStartOfDay().toInstant(ZoneOffset.UTC)),
        )
        val xp = perSession(listOf(Workouts.workout(1, mon), Workouts.workout(2, mon.plusDays(2))), records)
        assertEquals(0, xp.sessions.getValue(1).records)
        assertEquals(2, xp.sessions.getValue(2).records)
        assertEquals(Xp.WORKOUT + 2 * Xp.RECORD, xp.sessions.getValue(2).total)
    }

    @Test
    fun `only the first workout on a scheduled day gets the schedule bonus`() {
        val xp = perSession(
            listOf(Workouts.workout(1, mon), Workouts.workout(2, mon), Workouts.workout(3, mon.plusDays(1))),
            scheduled = setOf(mon),
        )
        assertTrue(xp.sessions.getValue(1).scheduled)
        assertFalse(xp.sessions.getValue(2).scheduled)
        assertFalse(xp.sessions.getValue(3).scheduled)
    }

    @Test
    fun `the workout that reaches the weekly goal gets the bonus, once per week`() {
        val days = listOf(0L, 2, 4, 5, 7, 9, 11)
        val xp = perSession(days.mapIndexed { i, d -> Workouts.workout(i + 1L, mon.plusDays(d)) }, goal = 3)
        assertEquals(listOf(3L, 7L), xp.sessions.values.filter { it.weeklyGoal }.map { it.sessionId })
    }

    @Test
    fun `level before a workout leaves out its XP`() {
        val days = listOf(mon, mon.plusDays(1), mon.plusDays(2))
        val xp = perSession(days.mapIndexed { i, d -> Workouts.workout(i + 1L, d) }, goal = 5)
        assertEquals(300, xp.total)
        assertEquals(2, xp.level.level)
        assertEquals(1, xp.levelBefore(3).level)
    }
}
