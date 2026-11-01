package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.records.Records
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A tappable "try this next" chip. [sets] fill the workout's unchecked
 * working sets in order; when there are more rows than drafts the last one
 * repeats.
 */
data class Suggestion(val kind: Kind, val label: String, val sets: List<SetDraft>) {
    enum class Kind { HEAVIER, MORE_REPS, REPEAT, ADD_WEIGHT, LONGER, FARTHER, FASTER }
}

/**
 * Progressive overload suggestions (SPEC §8), built from last time's working
 * sets. Always optional. Weights round to the exercise's increment in the
 * unit you're using, so a suggestion is a plate jump you can actually load.
 */
object Overload {
    fun suggest(exercise: ExerciseEntity, lastTime: List<SetEntryEntity>, units: UnitSystem): List<Suggestion> {
        val working = lastTime.filter { it.isCompleted && !it.isWarmup }.sortedBy { it.position }
        if (working.isEmpty()) return emptyList()
        return when (exercise.trackingType) {
            TrackingType.WEIGHT_REPS -> weightReps(exercise, working, units)
            TrackingType.BODYWEIGHT_REPS -> bodyweightReps(exercise, working, units)
            TrackingType.DURATION -> duration(working)
            TrackingType.DISTANCE_DURATION -> distance(working, units)
            TrackingType.WEIGHT_DISTANCE -> weightDistance(exercise, working, units)
        }
    }

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

    private fun weightReps(exercise: ExerciseEntity, working: List<SetEntryEntity>, units: UnitSystem): List<Suggestion> {
        val topKg = working.mapNotNull { it.weightKg }.maxOrNull() ?: return emptyList()
        val top = working.filter { it.weightKg == topKg && (it.reps ?: 0) > 0 }
        if (top.isEmpty()) return emptyList()
        val reps = top.first().reps!!
        val topDisplay = display(topKg, units)
        val inc = incrementDisplay(exercise, units)

        // Reps dropped off (8, 7, 6): own this weight before adding more.
        if (top.any { it.reps!! < reps }) {
            return listOf(
                Suggestion(Suggestion.Kind.REPEAT, "Same again: ${w(topDisplay, units)} × $reps", listOf(draft(topDisplay, reps, units))),
            )
        }

        val targetReps = (reps - 2).coerceAtLeast(3)
        val bestE1rm = working.mapNotNull { s -> s.weightKg?.let { kg -> s.reps?.let { Records.e1rm(kg, it) } } }.maxOrNull()
        val (low, high) = if (bestE1rm != null && reps > targetReps) {
            val target = display(bestE1rm, units) / (1 + targetReps / 30.0)
            var lo = floor(target / inc) * inc
            var hi = ceil(target / inc) * inc
            if (lo <= topDisplay) lo = topDisplay + inc
            if (hi < lo) hi = lo
            lo to hi
        } else {
            (topDisplay + inc).let { it to it }
        }
        val heavierReps = if (bestE1rm != null && reps > targetReps) targetReps else reps
        val range = if (high > low) "${num(low)}–${w(high, units)}" else w(low, units)
        return listOf(
            Suggestion(Suggestion.Kind.HEAVIER, "Heavier: $range × $heavierReps", listOf(draft(low, heavierReps, units))),
            Suggestion(Suggestion.Kind.MORE_REPS, "More reps: ${w(topDisplay, units)} × ${reps + 1}", listOf(draft(topDisplay, reps + 1, units))),
        )
    }

    private fun bodyweightReps(exercise: ExerciseEntity, working: List<SetEntryEntity>, units: UnitSystem): List<Suggestion> {
        val withReps = working.filter { (it.reps ?: 0) > 0 }
        if (withReps.isEmpty()) return emptyList()
        val plus = { n: Int -> withReps.map { SetDraft(reps = it.reps!! + n, addedWeightKg = it.addedWeightKg) } }
        val suggestions = mutableListOf(
            Suggestion(Suggestion.Kind.MORE_REPS, "+1 rep per set", plus(1)),
        )
        val maxRange = exercise.recommendedRepsMax
        val bestReps = withReps.maxOf { it.reps!! }
        if (maxRange != null && bestReps >= maxRange) {
            val addedDisplay = withReps.maxOf { it.addedWeightKg ?: 0.0 }.let { display(it, units) }
            val next = addedDisplay.coerceAtLeast(0.0) + incrementDisplay(exercise, units)
            val reps = exercise.recommendedRepsMin ?: (bestReps - 2).coerceAtLeast(1)
            suggestions += Suggestion(
                Suggestion.Kind.ADD_WEIGHT,
                "Add weight: BW+${w(next, units)} × $reps",
                listOf(SetDraft(reps = reps, addedWeightKg = Units.displayToKg(next, units))),
            )
        } else {
            suggestions += Suggestion(Suggestion.Kind.MORE_REPS, "+2 reps per set", plus(2))
        }
        return suggestions
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

    private fun draft(weightDisplay: Double, reps: Int, units: UnitSystem) =
        SetDraft(weightKg = Units.displayToKg(weightDisplay, units), reps = reps)

    private fun num(value: Double) = Units.formatNumber(value, 1)

    private fun w(value: Double, units: UnitSystem) = "${num(value)} ${Units.weightUnit(units)}"
}
