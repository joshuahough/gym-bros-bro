package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.data.db.entity.SetEntryEntity

/** Values for a new, not-yet-completed set. */
data class SetDraft(
    val weightKg: Double? = null,
    val reps: Int? = null,
    val addedWeightKg: Double? = null,
    val durationSec: Int? = null,
    val distanceM: Double? = null,
)

/**
 * Pre-fills new set rows from last time so repeating a workout is one tap per
 * set (SPEC §7.3).
 */
object Prefill {
    /**
     * [count] rows copied from last time's completed working sets, position by
     * position. If last time had fewer sets, the last one repeats; with no
     * history the rows start empty.
     */
    fun sets(count: Int, lastTime: List<SetEntryEntity>): List<SetDraft> {
        val working = lastTime.filter { it.isCompleted && !it.isWarmup }.sortedBy { it.position }
        return List(count.coerceAtLeast(1)) { i ->
            (working.getOrNull(i) ?: working.lastOrNull())?.toDraft() ?: SetDraft()
        }
    }

    /** The "+ SET" button: copy the bottom row's numbers. */
    fun nextSet(existing: List<SetEntryEntity>): SetDraft =
        existing.maxByOrNull { it.position }?.toDraft() ?: SetDraft()

    /**
     * After checking off [done], the set that should inherit its numbers:
     * the next row, if it's untouched. Saves retyping on a first-ever workout.
     */
    fun nextToFill(done: SetEntryEntity, siblings: List<SetEntryEntity>): SetEntryEntity? {
        val next = siblings.filter { it.position > done.position }.minByOrNull { it.position } ?: return null
        val untouched = !next.isCompleted && next.toDraft() == SetDraft()
        return if (untouched) next.copy(
            weightKg = done.weightKg,
            reps = done.reps,
            addedWeightKg = done.addedWeightKg,
            durationSec = done.durationSec,
            distanceM = done.distanceM,
        ) else null
    }

    private fun SetEntryEntity.toDraft() = SetDraft(weightKg, reps, addedWeightKg, durationSec, distanceM)
}
