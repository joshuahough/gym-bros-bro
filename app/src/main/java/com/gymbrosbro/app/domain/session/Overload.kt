package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.TemplateExerciseEntity
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * A tappable "try this next" chip. [sets] fill the workout's unchecked
 * working sets in order; when there are more rows than drafts the last one
 * repeats.
 */
data class Suggestion(val kind: Kind, val label: String, val sets: List<SetDraft>) {
    enum class Kind { HEAVIER, LIGHTER, MORE_REPS, REPEAT, ADD_WEIGHT, LONGER, FARTHER, FASTER }
}

/** The reps to aim for, and how many sets make a full workout ([sets] null = no plan to hold you to). */
data class RepTarget(val min: Int, val max: Int, val sets: Int? = null) {
    companion object {
        private const val DEFAULT_MIN = 8
        private const val DEFAULT_MAX = 12

        /** The workout's own range, else the exercise's recommended one, else 8–12. */
        fun of(exercise: ExerciseEntity, planned: TemplateExerciseEntity? = null): RepTarget {
            val (a, b) = range(planned?.targetRepsMin, planned?.targetRepsMax)
                ?: range(exercise.recommendedRepsMin, exercise.recommendedRepsMax)
                ?: (DEFAULT_MIN to DEFAULT_MAX)
            return RepTarget(minOf(a, b), maxOf(a, b), planned?.targetSets)
        }

        /** Either end alone is an exact target. */
        private fun range(min: Int?, max: Int?): Pair<Int, Int>? {
            val low = min ?: max ?: return null
            return low to (max ?: low)
        }
    }
}

/**
 * Progressive overload suggestions (SPEC §8). Reps climb through the target
 * range first; weight only goes up after [WORKOUTS_TO_CONFIRM] workouts in a
 * row at the top of it, and comes down after as many below the bottom. Always
 * optional. Weights round to the exercise's increment in the unit you're
 * using, so a suggestion is a plate jump you can actually load.
 */
object Overload {
    const val WORKOUTS_TO_CONFIRM = 2

    /** [history] is each earlier workout's sets for this exercise, newest first. */
    fun suggest(
        exercise: ExerciseEntity,
        history: List<List<SetEntryEntity>>,
        units: UnitSystem,
        target: RepTarget = RepTarget.of(exercise),
    ): List<Suggestion> {
        val workouts = history.map(::working).filter { it.isNotEmpty() }
        val last = workouts.firstOrNull() ?: return emptyList()
        return when (exercise.trackingType) {
            TrackingType.WEIGHT_REPS -> weightReps(exercise, workouts, units, target)
            TrackingType.BODYWEIGHT_REPS -> bodyweightReps(exercise, workouts, units, target)
            TrackingType.DURATION -> duration(last)
            TrackingType.DISTANCE_DURATION -> distance(last, units)
            TrackingType.WEIGHT_DISTANCE -> weightDistance(exercise, last, units)
            TrackingType.REPS -> repsOnly(last)
        }
    }

    fun working(sets: List<SetEntryEntity>): List<SetEntryEntity> =
        sets.filter { it.isCompleted && !it.isWarmup }.sortedBy { it.position }

    /** Smallest sensible weight jump, in display units. */
    fun incrementDisplay(exercise: ExerciseEntity, units: UnitSystem): Double {
        exercise.weightIncrementKg?.let { return Units.kgToDisplay(it, units) }
        val imperial = units == UnitSystem.IMPERIAL
        return when (exercise.equipment) {
            Equipment.MACHINE, Equipment.CABLE -> if (imperial) 10.0 else 5.0
            Equipment.DUMBBELL -> if (imperial) 5.0 else 2.0
            Equipment.KETTLEBELL -> if (imperial) 5.0 else 4.0
            else -> if (imperial) 5.0 else 2.5
        }
    }

    /** The sets done at a workout's heaviest load; lighter ramp-up and back-off sets don't count. */
    private data class TopSets(val loadKg: Double, val reps: List<Int>) {
        fun full(target: RepTarget) = reps.size >= (target.sets ?: 1)

        fun atTop(target: RepTarget) = full(target) && reps.all { it >= target.max }

        fun belowRange(target: RepTarget) = reps.all { it < target.min }
    }

    private fun topSets(sets: List<SetEntryEntity>, load: (SetEntryEntity) -> Double?): TopSets? {
        val done = sets.filter { (it.reps ?: 0) > 0 && load(it) != null }
        val top = done.maxOfOrNull { load(it)!! } ?: return null
        return TopSets(top, done.filter { sameLoad(load(it)!!, top) }.map { it.reps!! })
    }

    /** Enough workouts in a row at the top of the range, all at the same load. */
    private fun readyForMore(recent: List<TopSets>, target: RepTarget): Boolean {
        val streak = recent.take(WORKOUTS_TO_CONFIRM)
        return streak.size == WORKOUTS_TO_CONFIRM &&
            streak.all { it.atTop(target) && sameLoad(it.loadKg, streak.first().loadKg) }
    }

    /** Short of the range too many workouts in a row. Having just gone lighter starts the count over. */
    private fun needsLess(recent: List<TopSets>, target: RepTarget): Boolean {
        val streak = recent.take(WORKOUTS_TO_CONFIRM)
        return streak.size == WORKOUTS_TO_CONFIRM &&
            streak.all { it.belowRange(target) } &&
            streak.zipWithNext().none { (newer, older) -> newer.loadKg < older.loadKg && !sameLoad(newer.loadKg, older.loadKg) }
    }

    private fun sameLoad(a: Double, b: Double) = abs(a - b) < 0.05

    private fun weightReps(
        exercise: ExerciseEntity,
        workouts: List<List<SetEntryEntity>>,
        units: UnitSystem,
        target: RepTarget,
    ): List<Suggestion> {
        val recent = workouts.take(WORKOUTS_TO_CONFIRM).mapNotNull { sets -> topSets(sets) { it.weightKg } }
        val last = recent.firstOrNull() ?: return emptyList()
        val weight = display(last.loadKg, units)
        val inc = incrementDisplay(exercise, units)
        val reps = last.reps.first()

        fun one(kind: Suggestion.Kind, verb: String, weight: Double, reps: Int) =
            listOf(Suggestion(kind, "$verb: ${w(weight, units)} × $reps", listOf(draft(weight, reps, units))))

        if (needsLess(recent, target)) {
            val lighter = stepDown(weight, inc)
            if (lighter > 0) return one(Suggestion.Kind.LIGHTER, "Lighter", lighter, target.min)
        }
        return when {
            readyForMore(recent, target) -> one(Suggestion.Kind.HEAVIER, "Heavier", stepUp(weight, inc), target.min)
            last.atTop(target) -> one(Suggestion.Kind.REPEAT, "Once more", weight, last.reps.min())
            // Reps dropped off (8, 7, 6) or sets are missing: own this weight first.
            last.reps.any { it < reps } || !last.full(target) -> one(Suggestion.Kind.REPEAT, "Same again", weight, reps)
            else -> one(Suggestion.Kind.MORE_REPS, "More reps", weight, reps + 1)
        }
    }

    private fun bodyweightReps(
        exercise: ExerciseEntity,
        workouts: List<List<SetEntryEntity>>,
        units: UnitSystem,
        target: RepTarget,
    ): List<Suggestion> {
        val sets = workouts.first().filter { (it.reps ?: 0) > 0 }
        if (sets.isEmpty()) return emptyList()
        val recent = workouts.take(WORKOUTS_TO_CONFIRM).mapNotNull { w -> topSets(w) { it.addedWeightKg ?: 0.0 } }
        val last = recent.first()
        val added = display(last.loadKg, units)
        val inc = incrementDisplay(exercise, units)

        fun label(added: Double) = SetFormat.bodyweightLabel(Units.displayToKg(added, units), units)
        fun load(added: Double, reps: Int) =
            listOf(SetDraft(reps = reps, addedWeightKg = Units.displayToKg(added, units).takeIf { added != 0.0 }))
        fun plus(n: Int, cap: Int) =
            sets.map { SetDraft(reps = maxOf(it.reps!!, minOf(it.reps + n, cap)), addedWeightKg = it.addedWeightKg) }

        if (added > 0 && needsLess(recent, target)) {
            val lighter = (added - inc).coerceAtLeast(0.0)
            return listOf(
                Suggestion(Suggestion.Kind.LIGHTER, "Lighter: ${label(lighter)} × ${target.min}", load(lighter, target.min)),
            )
        }
        if (readyForMore(recent, target)) {
            val heavier = stepUp(added, inc)
            val verb = if (added < 0) "Less help" else "Add weight"
            return listOf(
                Suggestion(Suggestion.Kind.ADD_WEIGHT, "$verb: ${label(heavier)} × ${target.min}", load(heavier, target.min)),
                // Not everything can be loaded, so more reps stays on offer.
                Suggestion(Suggestion.Kind.MORE_REPS, "+1 rep per set", plus(1, Int.MAX_VALUE)),
            )
        }
        if (last.atTop(target)) {
            val reps = last.reps.min()
            return listOf(Suggestion(Suggestion.Kind.REPEAT, "Once more: ${label(added)} × $reps", load(added, reps)))
        }
        val room = target.max - sets.maxOf { it.reps!! }
        return when {
            // The best set is there already: bring the others up to it.
            room <= 0 && sets.any { it.reps!! < target.max } ->
                listOf(Suggestion(Suggestion.Kind.MORE_REPS, "+1 rep on shorter sets", plus(1, target.max)))
            // Reps are there, but sets are missing or some were easier.
            room <= 0 -> listOf(
                Suggestion(Suggestion.Kind.REPEAT, "Same again: ${label(added)} × ${last.reps.first()}", load(added, last.reps.first())),
            )
            else -> listOfNotNull(
                Suggestion(Suggestion.Kind.MORE_REPS, "+1 rep per set", plus(1, target.max)),
                Suggestion(Suggestion.Kind.MORE_REPS, "+2 reps per set", plus(2, target.max)).takeIf { room >= 2 },
            )
        }
    }

    /** No weight to add, so just more reps; big counts (100 skips) step by 5 or 10. */
    private fun repsOnly(working: List<SetEntryEntity>): List<Suggestion> {
        val withReps = working.filter { (it.reps ?: 0) > 0 }
        if (withReps.isEmpty()) return emptyList()
        val best = withReps.maxOf { it.reps!! }
        val step = when {
            best >= 50 -> 10
            best >= 20 -> 5
            else -> 1
        }
        val plus = { n: Int -> withReps.map { SetDraft(reps = it.reps!! + n) } }
        return listOf(
            Suggestion(Suggestion.Kind.MORE_REPS, "+$step ${if (step == 1) "rep" else "reps"} per set", plus(step)),
            Suggestion(Suggestion.Kind.MORE_REPS, "+${step * 2} reps per set", plus(step * 2)),
        )
    }

    private fun duration(working: List<SetEntryEntity>): List<Suggestion> {
        val timed = working.filter { (it.durationSec ?: 0) > 0 }
        if (timed.isEmpty()) return emptyList()
        val step = if (timed.maxOf { it.durationSec!! } >= 60) 10 else 5
        return listOf(
            Suggestion(
                Suggestion.Kind.LONGER,
                "+$step sec per set",
                timed.map { SetDraft(durationSec = it.durationSec!! + step, addedWeightKg = it.addedWeightKg) },
            ),
        )
    }

    private fun distance(working: List<SetEntryEntity>, units: UnitSystem): List<Suggestion> {
        val best = working.filter { (it.distanceM ?: 0.0) > 0 }.maxByOrNull { it.distanceM!! } ?: return emptyList()
        val meters = best.distanceM!!
        val seconds = best.durationSec?.takeIf { it > 0 }
        val farther = Units.displayToMeters(round2(Units.metersToDisplay(meters * 1.05, units)), units)
        val suggestions = mutableListOf<Suggestion>()
        if (seconds != null) {
            val faster = (seconds * 0.95).roundToInt()
            suggestions += Suggestion(
                Suggestion.Kind.FASTER,
                "Faster: ${Units.formatDistance(meters, units)} in ${SetFormat.duration(faster)}",
                listOf(SetDraft(distanceM = meters, durationSec = faster)),
            )
        }
        suggestions += Suggestion(
            Suggestion.Kind.FARTHER,
            "Farther: ${Units.formatDistance(farther, units)}",
            listOf(SetDraft(distanceM = farther, durationSec = seconds?.let { (it * 1.05).roundToInt() })),
        )
        return suggestions
    }

    private fun weightDistance(exercise: ExerciseEntity, working: List<SetEntryEntity>, units: UnitSystem): List<Suggestion> {
        val top = working.filter { it.weightKg != null }.maxByOrNull { it.weightKg!! } ?: return emptyList()
        val next = display(top.weightKg!!, units) + incrementDisplay(exercise, units)
        return listOf(
            Suggestion(
                Suggestion.Kind.HEAVIER,
                "Heavier: ${w(next, units)}",
                listOf(SetDraft(weightKg = Units.displayToKg(next, units), distanceM = top.distanceM)),
            ),
        )
    }

    /** Display value with float noise removed, so 145 lb stays 145 after a kg round trip. */
    private fun display(kg: Double, units: UnitSystem) = round2(Units.kgToDisplay(kg, units))

    private fun round2(value: Double) = Math.round(value * 100) / 100.0

    /** The next loadable weight up. Odd weights snap (22.5 → 25), but never by a sliver (44.1 → 50, not 45). */
    private fun stepUp(weight: Double, inc: Double) = ceil((weight + inc / 4) / inc - EPSILON) * inc + 0.0

    /** About 10% off, to the nearest loadable weight below. */
    private fun stepDown(weight: Double, inc: Double): Double {
        val nearest = ceil(weight * 0.9 / inc - 0.5 - EPSILON) * inc
        return if (nearest < weight) nearest else nearest - inc
    }

    private const val EPSILON = 1e-6

    private fun draft(weightDisplay: Double, reps: Int, units: UnitSystem) =
        SetDraft(weightKg = Units.displayToKg(weightDisplay, units), reps = reps)

    private fun num(value: Double) = Units.formatNumber(value, 1)

    private fun w(value: Double, units: UnitSystem) = "${num(value)} ${Units.weightUnit(units)}"
}
