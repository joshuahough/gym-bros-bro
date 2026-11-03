package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.TestData.exercise
import com.gymbrosbro.app.TestData.set
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverloadTest {
    private val bench = exercise(equipment = Equipment.BARBELL)
    private fun lb(value: Double) = Units.displayToKg(value, UnitSystem.IMPERIAL)
    private fun asLb(kg: Double?) = Math.round(Units.kgToDisplay(kg!!, UnitSystem.IMPERIAL) * 100) / 100.0

    @Test
    fun `spec example - 3x8 at 145 suggests 150-155 x 6`() {
        val last = List(3) { set(it, weightKg = lb(145.0), reps = 8) }
        val suggestions = Overload.suggest(bench, last, UnitSystem.IMPERIAL)
        val heavier = suggestions.first { it.kind == Suggestion.Kind.HEAVIER }
        assertEquals("Heavier: 150–155 lb × 6", heavier.label)
        assertEquals(150.0, asLb(heavier.sets.single().weightKg), 1e-9)
        assertEquals(6, heavier.sets.single().reps)

        val more = suggestions.first { it.kind == Suggestion.Kind.MORE_REPS }
        assertEquals("More reps: 145 lb × 9", more.label)
    }

    @Test
    fun `reps dropping off suggests repeating`() {
        val last = listOf(set(0, weightKg = lb(145.0), reps = 8), set(1, weightKg = lb(145.0), reps = 7), set(2, weightKg = lb(145.0), reps = 6))
        val suggestions = Overload.suggest(bench, last, UnitSystem.IMPERIAL)
        assertEquals(listOf(Suggestion.Kind.REPEAT), suggestions.map { it.kind })
        assertEquals("Same again: 145 lb × 8", suggestions.single().label)
    }

    @Test
    fun `warm-ups are ignored`() {
        val last = listOf(set(0, weightKg = lb(95.0), reps = 5, warmup = true), set(1, weightKg = lb(145.0), reps = 8))
        val heavier = Overload.suggest(bench, last, UnitSystem.IMPERIAL).first()
        assertEquals("Heavier: 150–155 lb × 6", heavier.label)
    }

    @Test
    fun `heavier is always above the current weight`() {
        // 3 reps can't drop further, so it's just the next increment.
        val last = List(3) { set(it, weightKg = lb(225.0), reps = 3) }
        val heavier = Overload.suggest(bench, last, UnitSystem.IMPERIAL).first()
        assertEquals("Heavier: 230 lb × 3", heavier.label)
    }

    @Test
    fun `metric rounds to kilogram plates`() {
        val last = List(3) { set(it, weightKg = 60.0, reps = 8) }
        val heavier = Overload.suggest(bench, last, UnitSystem.METRIC).first()
        // e1RM 76 kg → 63.3 kg for 6 → 62.5–65 kg.
        assertEquals("Heavier: 62.5–65 kg × 6", heavier.label)
    }

    @Test
    fun `machines jump in bigger steps`() {
        assertEquals(10.0, Overload.incrementDisplay(exercise(equipment = Equipment.MACHINE), UnitSystem.IMPERIAL), 1e-9)
        assertEquals(2.0, Overload.incrementDisplay(exercise(equipment = Equipment.DUMBBELL), UnitSystem.METRIC), 1e-9)
    }

    @Test
    fun `bodyweight adds reps, then weight at the top of the range`() {
        val pushUp = exercise(trackingType = TrackingType.BODYWEIGHT_REPS, equipment = Equipment.BODYWEIGHT)
        val mid = Overload.suggest(pushUp, listOf(set(0, reps = 10), set(1, reps = 9)), UnitSystem.IMPERIAL)
        assertEquals(listOf(11, 10), mid.first().sets.map { it.reps })
        assertEquals("+2 reps per set", mid[1].label)

        val top = Overload.suggest(pushUp, listOf(set(0, reps = 12)), UnitSystem.IMPERIAL)
        assertEquals(Suggestion.Kind.ADD_WEIGHT, top[1].kind)
        assertEquals("Add weight: BW+5 lb × 8", top[1].label)
    }

    @Test
    fun `duration adds seconds`() {
        val plank = exercise(trackingType = TrackingType.DURATION)
        val s = Overload.suggest(plank, listOf(set(0, durationSec = 45)), UnitSystem.IMPERIAL).single()
        assertEquals(50, s.sets.single().durationSec)
    }

    @Test
    fun `reps only steps by bigger jumps for big counts`() {
        val jumpRope = exercise(trackingType = TrackingType.REPS, equipment = Equipment.OTHER)
        val s = Overload.suggest(jumpRope, listOf(set(0, reps = 100), set(1, reps = 80)), UnitSystem.IMPERIAL)
        assertEquals("+10 reps per set", s[0].label)
        assertEquals(listOf(110, 90), s[0].sets.map { it.reps })
        assertEquals("+20 reps per set", s[1].label)
        assertTrue(s.flatMap { it.sets }.all { it.addedWeightKg == null })
    }

    @Test
    fun `first time has no suggestions`() {
        assertTrue(Overload.suggest(bench, emptyList(), UnitSystem.IMPERIAL).isEmpty())
    }
}
