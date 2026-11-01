package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.TestData.set
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetFormatTest {
    private val lb = UnitSystem.IMPERIAL
    private fun lbs(value: Double) = Units.displayToKg(value, lb)

    @Test
    fun `durations format and parse`() {
        assertEquals("0:45", SetFormat.duration(45))
        assertEquals("2:05", SetFormat.duration(125))
        assertEquals("1:02:05", SetFormat.duration(3725))
        assertEquals(90, SetFormat.parseDuration("1:30"))
        assertEquals(90, SetFormat.parseDuration("90"))
        assertEquals(3725, SetFormat.parseDuration("1:02:05"))
        assertNull(SetFormat.parseDuration(""))
        assertNull(SetFormat.parseDuration("1:"))
        assertNull(SetFormat.parseDuration("abc"))
    }

    @Test
    fun `decimals accept a comma`() {
        assertEquals(2.5, SetFormat.parseDecimal("2,5")!!, 0.0)
        assertNull(SetFormat.parseDecimal(""))
    }

    @Test
    fun `same weight sets share one label`() {
        val sets = (0..2).map { set(it, weightKg = lbs(145.0), reps = 8) }
        assertEquals("145 lb × 8, 8, 8", SetFormat.summary(TrackingType.WEIGHT_REPS, sets, lb))
    }

    @Test
    fun `changing weights start a new group and incomplete sets are skipped`() {
        val sets = listOf(
            set(0, weightKg = lbs(135.0), reps = 10),
            set(1, weightKg = lbs(145.0), reps = 8),
            set(2, weightKg = lbs(145.0), reps = 7),
            set(3, weightKg = lbs(155.0), reps = 5, completed = false),
        )
        assertEquals("135 lb × 10 / 145 lb × 8, 7", SetFormat.summary(TrackingType.WEIGHT_REPS, sets, lb))
    }

    @Test
    fun `bodyweight sets show added or assisted weight`() {
        val sets = listOf(set(0, reps = 10), set(1, reps = 6, addedWeightKg = lbs(25.0)))
        assertEquals("BW × 10 / BW+25 lb × 6", SetFormat.summary(TrackingType.BODYWEIGHT_REPS, sets, lb))
        assertEquals("BW−20 lb", SetFormat.bodyweightLabel(lbs(-20.0), lb))
    }

    @Test
    fun `cardio shows distance and time`() {
        val sets = listOf(set(0, distanceM = 5000.0, durationSec = 1680))
        assertEquals("3.11 mi in 28:00", SetFormat.summary(TrackingType.DISTANCE_DURATION, sets, lb))
    }
}
