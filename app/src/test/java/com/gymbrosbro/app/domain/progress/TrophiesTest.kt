package com.gymbrosbro.app.domain.progress

import com.gymbrosbro.app.TestData
import com.gymbrosbro.app.TestData.set
import com.gymbrosbro.app.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrophiesTest {
    private val mon = Workouts.MON

    @Test
    fun `ids are unique`() {
        assertEquals(Trophies.all.size, Trophies.all.map { it.id }.toSet().size)
    }

    @Test
    fun `lifetime counts finished workouts, volume and working pull-up reps`() {
        val pullUps = TestData.exercise(id = "pullups", trackingType = TrackingType.BODYWEIGHT_REPS)
        val lifetime = Lifetime.of(
            listOf(
                Workouts.workout(1, mon, set(0, weightKg = 100.0, reps = 5), set(1, weightKg = 100.0, reps = 5)),
                Workouts.workout(
                    2, mon.plusDays(2),
                    set(0, reps = 3, warmup = true), set(1, reps = 10), set(2, reps = 8),
                    exercise = pullUps,
                ),
                Workouts.workout(3, mon.plusDays(4), set(0, weightKg = 100.0, reps = 5), finished = false),
            ),
            records = 4,
            bestWeeklyStreak = 2,
            bestCombo = 7,
        )
        assertEquals(2, lifetime.workouts)
        assertEquals(1000.0, lifetime.volumeKg, 1e-9)
        assertEquals(18, lifetime.pullUps)
        assertEquals(4, lifetime.records)
    }

    @Test
    fun `trophies are reached at their target, not before`() {
        val reached = Trophies.reached(Lifetime(workouts = 10, records = 1, bestCombo = 9)).map { it.id }
        assertEquals(listOf("workouts-1", "workouts-10", "records-1"), reached)
        assertTrue(Trophies.reached(Lifetime()).isEmpty())
    }

    @Test
    fun `volume trophies are 100k and 1M pounds`() {
        val reached = Trophies.reached(Lifetime(volumeKg = 100_000 * 0.45359237)).map { it.id }
        assertEquals(listOf("volume-100k"), reached)
    }
}
