package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.TestData.exercise
import com.gymbrosbro.app.TestData.set
import com.gymbrosbro.app.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutMathTest {
    private val bench = exercise(trackingType = TrackingType.WEIGHT_REPS)
    private val pushUp = exercise(trackingType = TrackingType.BODYWEIGHT_REPS, bodyweightFactor = 0.65)
    private val run = exercise(trackingType = TrackingType.DISTANCE_DURATION)

    @Test
    fun `only completed sets count and warm-ups are included`() {
        val sets = listOf(
            set(0, weightKg = 20.0, reps = 10, warmup = true),
            set(1, weightKg = 60.0, reps = 8),
            set(2, weightKg = 60.0, reps = 8, completed = false),
        )
        val totals = WorkoutMath.totals(bench, sets, bodyweightKg = null)
        assertEquals(2, totals.sets)
        assertEquals(18, totals.reps)
        assertEquals(20.0 * 10 + 60.0 * 8, totals.volumeKg, 1e-9)
    }

    @Test
    fun `bodyweight volume uses the factor plus added weight`() {
        val sets = listOf(set(0, reps = 10), set(1, reps = 5, addedWeightKg = 10.0))
        val totals = WorkoutMath.totals(pushUp, sets, bodyweightKg = 80.0)
        assertEquals(80.0 * 0.65 * 10 + (80.0 * 0.65 + 10.0) * 5, totals.volumeKg, 1e-9)
        assertEquals(15, totals.reps)
    }

    @Test
    fun `bodyweight sets without a bodyweight still count reps but no volume`() {
        val totals = WorkoutMath.totals(pushUp, listOf(set(0, reps = 10)), bodyweightKg = null)
        assertEquals(10, totals.reps)
        assertEquals(0.0, totals.volumeKg, 0.0)
    }

    @Test
    fun `assistance never makes volume negative`() {
        val volume = WorkoutMath.setVolumeKg(pushUp, set(0, reps = 5, addedWeightKg = -100.0), bodyweightKg = 80.0)
        assertEquals(0.0, volume, 0.0)
    }

    @Test
    fun `cardio adds distance and time but no reps or volume`() {
        val totals = WorkoutMath.totals(run, listOf(set(0, distanceM = 5000.0, durationSec = 1500)), null)
        assertEquals(5000.0, totals.distanceM, 0.0)
        assertEquals(1500, totals.durationSec)
        assertEquals(0, totals.reps)
        assertEquals(0.0, totals.volumeKg, 0.0)
    }

    @Test
    fun `sets need their required numbers to be checked off`() {
        assertFalse(WorkoutMath.canComplete(TrackingType.WEIGHT_REPS, set(reps = 8)))
        assertTrue(WorkoutMath.canComplete(TrackingType.WEIGHT_REPS, set(weightKg = 0.0, reps = 8)))
        assertTrue(WorkoutMath.canComplete(TrackingType.BODYWEIGHT_REPS, set(reps = 8)))
        assertFalse(WorkoutMath.canComplete(TrackingType.DURATION, set()))
        assertTrue(WorkoutMath.canComplete(TrackingType.DISTANCE_DURATION, set(durationSec = 600)))
    }
}
