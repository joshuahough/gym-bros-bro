package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.TestData.exercise
import com.gymbrosbro.app.TestData.set
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.TemplateExerciseEntity
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

    private val hinge = RepTarget(8, 10, sets = 3)
    private val press = RepTarget(6, 8, sets = 3)

    /** One workout: every set at [weight] lb. */
    private fun lifted(weight: Double, vararg reps: Int) = reps.mapIndexed { i, r -> set(i, weightKg = lb(weight), reps = r) }

    private fun reps(vararg reps: Int, added: Double? = null) =
        reps.mapIndexed { i, r -> set(i, reps = r, addedWeightKg = added?.let(::lb)) }

    /** The chips after these workouts, newest first. */
    private fun labels(target: RepTarget, vararg newestFirst: List<SetEntryEntity>, exercise: ExerciseEntity = bench) =
        Overload.suggest(exercise, newestFirst.toList(), UnitSystem.IMPERIAL, target).map { it.label }

    @Test
    fun `spec example - top of the range twice goes up one increment`() {
        val top = lifted(145.0, 10, 10, 10)
        val heavier = Overload.suggest(bench, listOf(top, top), UnitSystem.IMPERIAL, hinge).single()
        assertEquals(Suggestion.Kind.HEAVIER, heavier.kind)
        assertEquals("Heavier: 150 lb × 8", heavier.label)
        assertEquals(150.0, asLb(heavier.sets.single().weightKg), 1e-9)
        assertEquals(8, heavier.sets.single().reps)
    }

    @Test
    fun `top of the range once asks for one more`() {
        assertEquals(listOf("Once more: 40 lb × 10"), labels(hinge, lifted(40.0, 10, 10, 10), lifted(40.0, 9, 9, 9)))
        // At a different weight last time is also a first.
        assertEquals(listOf("Once more: 40 lb × 10"), labels(hinge, lifted(40.0, 10, 10, 10), lifted(35.0, 10, 10, 10)))
        assertEquals(listOf("Once more: 40 lb × 10"), labels(hinge, lifted(40.0, 10, 10, 10)))
    }

    @Test
    fun `below the top only reps go up, however long it takes`() {
        assertEquals(listOf("More reps: 115 lb × 9"), labels(hinge, lifted(115.0, 8, 8, 8)))
        assertEquals(listOf("More reps: 155 lb × 7"), labels(press, lifted(155.0, 6, 6, 6), lifted(155.0, 6, 6, 5)))
        assertEquals(listOf("More reps: 115 lb × 10"), labels(hinge, lifted(115.0, 9, 9, 9), lifted(115.0, 9, 9, 9)))
    }

    @Test
    fun `past the top still counts as the top`() {
        assertEquals(listOf("Heavier: 80 lb × 6"), labels(press, lifted(75.0, 10, 9, 9), lifted(75.0, 8, 8, 8)))
    }

    @Test
    fun `reps dropping off suggests repeating`() {
        val suggestions = Overload.suggest(bench, listOf(lifted(145.0, 8, 7, 6)), UnitSystem.IMPERIAL, press)
        assertEquals(listOf(Suggestion.Kind.REPEAT), suggestions.map { it.kind })
        assertEquals("Same again: 145 lb × 8", suggestions.single().label)
    }

    @Test
    fun `missing planned sets hold the weight`() {
        val short = lifted(145.0, 8, 8)
        assertEquals(listOf("Same again: 145 lb × 8"), labels(press, short, short))
        // Only the sets at the new weight count toward the plan.
        val jumped = listOf(set(0, weightKg = lb(75.0), reps = 9)) + lifted(80.0, 6, 6).map { it.copy(position = it.position + 1) }
        assertEquals(listOf("Same again: 80 lb × 6"), labels(press, jumped))
        // Without a plan there's no set count to miss.
        assertEquals(listOf("Heavier: 150 lb × 6"), labels(RepTarget(6, 8), short, short))
    }

    @Test
    fun `short of the range twice backs off about ten percent`() {
        assertEquals(listOf("Lighter: 110 lb × 8"), labels(hinge, lifted(125.0, 4, 4, 4), lifted(120.0, 6, 6, 6)))
        assertEquals(listOf("Lighter: 110 lb × 8"), labels(hinge, lifted(125.0, 5, 3), lifted(125.0, 4, 4, 4)))
        // Once isn't enough, and one good set keeps the weight.
        assertEquals(listOf("More reps: 120 lb × 7"), labels(hinge, lifted(120.0, 6, 6, 6), lifted(115.0, 8, 8, 8)))
        assertEquals(listOf("Same again: 115 lb × 8"), labels(hinge, lifted(115.0, 8, 7, 7), lifted(115.0, 7, 7, 7)))
    }

    @Test
    fun `a lighter weight gets its own two workouts`() {
        assertEquals(listOf("More reps: 110 lb × 8"), labels(hinge, lifted(110.0, 7, 7, 7), lifted(125.0, 4, 4, 4)))
        assertEquals(listOf("Lighter: 100 lb × 8"), labels(hinge, lifted(110.0, 7, 7, 7), lifted(110.0, 7, 7, 7)))
    }

    @Test
    fun `lighter never reaches zero`() {
        assertEquals(listOf("More reps: 5 lb × 5"), labels(hinge, lifted(5.0, 4, 4, 4), lifted(5.0, 4, 4, 4)))
        assertEquals(listOf("Lighter: 5 lb × 8"), labels(hinge, lifted(10.0, 4, 4, 4), lifted(10.0, 4, 4, 4)))
    }

    @Test
    fun `warm-ups are ignored`() {
        val last = listOf(set(0, weightKg = lb(95.0), reps = 5, warmup = true)) +
            lifted(145.0, 8, 8, 8).map { it.copy(position = it.position + 1) }
        assertEquals(listOf("Heavier: 150 lb × 6"), labels(press, last, last))
        // A workout that was all warm-ups isn't one of the two.
        val warmupOnly = listOf(set(0, weightKg = lb(45.0), reps = 10, warmup = true))
        assertEquals(listOf("Heavier: 150 lb × 6"), labels(press, warmupOnly, last, last))
    }

    @Test
    fun `odd weights snap to the next plate jump, but not by a sliver`() {
        val dumbbell = exercise(equipment = Equipment.DUMBBELL)
        fun next(weight: Double) = lifted(weight, 12, 12, 12).let { labels(RepTarget(10, 12, 3), it, it, exercise = dumbbell).single() }
        assertEquals("Heavier: 25 lb × 10", next(22.5))
        assertEquals("Heavier: 20 lb × 10", next(17.5))
        assertEquals("Heavier: 50 lb × 10", next(44.1))
        assertEquals("Heavier: 5 lb × 10", next(0.0))
    }

    @Test
    fun `metric rounds to kilogram plates`() {
        val top = List(3) { set(it, weightKg = 60.0, reps = 8) }
        val heavier = Overload.suggest(bench, listOf(top, top), UnitSystem.METRIC, press).single()
        assertEquals("Heavier: 62.5 kg × 6", heavier.label)
    }

    @Test
    fun `the workout's range wins, then the exercise's, then a default`() {
        fun planned(min: Int?, max: Int?) =
            TemplateExerciseEntity(templateId = 1, exerciseId = "ex", position = 0, targetSets = 4, targetRepsMin = min, targetRepsMax = max)
        assertEquals(RepTarget(6, 8, 4), RepTarget.of(bench, planned(6, 8)))
        assertEquals(RepTarget(6, 8, 4), RepTarget.of(bench, planned(8, 6)))
        assertEquals(RepTarget(5, 5, 4), RepTarget.of(bench, planned(null, 5)))
        assertEquals(RepTarget(8, 12, 4), RepTarget.of(bench.copy(recommendedRepsMin = 8), planned(null, null)))
        assertEquals(RepTarget(3, 5), RepTarget.of(bench.copy(recommendedRepsMin = 3, recommendedRepsMax = 5)))
        assertEquals(RepTarget(8, 12), RepTarget.of(bench.copy(recommendedRepsMin = null, recommendedRepsMax = null)))
    }

    @Test
    fun `machines jump in bigger steps`() {
        assertEquals(10.0, Overload.incrementDisplay(exercise(equipment = Equipment.MACHINE), UnitSystem.IMPERIAL), 1e-9)
        assertEquals(2.0, Overload.incrementDisplay(exercise(equipment = Equipment.DUMBBELL), UnitSystem.METRIC), 1e-9)
    }

    private val pullUp = exercise(trackingType = TrackingType.BODYWEIGHT_REPS, equipment = Equipment.BODYWEIGHT)

    private fun bodyweight(vararg newestFirst: List<SetEntryEntity>) =
        Overload.suggest(pullUp, newestFirst.toList(), UnitSystem.IMPERIAL, press)

    @Test
    fun `bodyweight adds reps up to the top of the range`() {
        val mid = bodyweight(reps(6, 5, 5))
        assertEquals(listOf("+1 rep per set", "+2 reps per set"), mid.map { it.label })
        assertEquals(listOf(7, 6, 6), mid[0].sets.map { it.reps })
        assertEquals(listOf(8, 7, 7), mid[1].sets.map { it.reps })

        assertEquals(listOf("+1 rep per set"), bodyweight(reps(7, 7, 7)).map { it.label })
        val shorter = bodyweight(reps(8, 8, 7)).single()
        assertEquals("+1 rep on shorter sets", shorter.label)
        assertEquals(listOf(8, 8, 8), shorter.sets.map { it.reps })
    }

    @Test
    fun `bodyweight adds weight after two workouts at the top`() {
        val top = reps(8, 8, 8)
        assertEquals(listOf("Once more: BW × 8"), bodyweight(top, reps(8, 8, 7)).map { it.label })
        assertEquals(null, bodyweight(top).single().sets.single().addedWeightKg)

        val ready = bodyweight(top, top)
        assertEquals(listOf("Add weight: BW+5 lb × 6", "+1 rep per set"), ready.map { it.label })
        assertEquals(listOf(9, 9, 9), ready[1].sets.map { it.reps })

        val loaded = reps(8, 8, 8, added = 10.0)
        assertEquals("Once more: BW+10 lb × 8", bodyweight(loaded, top).single().label)
        assertEquals("Add weight: BW+15 lb × 6", bodyweight(loaded, loaded).first().label)
    }

    @Test
    fun `bodyweight holds when planned sets are missing`() {
        assertEquals("Same again: BW × 8", bodyweight(reps(8, 8), reps(8, 8)).single().label)
    }

    @Test
    fun `assisted reps lose help a step at a time`() {
        fun next(added: Double) = reps(8, 8, 8, added = added).let { bodyweight(it, it).first() }
        assertEquals("Less help: BW−15 lb × 6", next(-20.0).label)
        assertEquals("Less help: BW × 6", next(-5.0).label)
        assertEquals(null, next(-5.0).sets.single().addedWeightKg)
    }

    @Test
    fun `added weight comes off after falling short twice`() {
        val short = reps(5, 4, 4, added = 10.0)
        assertEquals("Lighter: BW+5 lb × 6", bodyweight(short, short).single().label)
        val shortBw = reps(5, 4, 4)
        assertEquals("+1 rep per set", bodyweight(shortBw, shortBw).first().label)
    }

    @Test
    fun `duration adds seconds`() {
        val plank = exercise(trackingType = TrackingType.DURATION)
        val s = Overload.suggest(plank, listOf(listOf(set(0, durationSec = 45))), UnitSystem.IMPERIAL).single()
        assertEquals(50, s.sets.single().durationSec)
    }

    @Test
    fun `reps only steps by bigger jumps for big counts`() {
        val jumpRope = exercise(trackingType = TrackingType.REPS, equipment = Equipment.OTHER)
        val s = Overload.suggest(jumpRope, listOf(listOf(set(0, reps = 100), set(1, reps = 80))), UnitSystem.IMPERIAL)
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
