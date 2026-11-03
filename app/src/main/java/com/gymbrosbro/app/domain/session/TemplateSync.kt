package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.data.TemplateExerciseDraft
import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.db.TemplateWithExercises

/**
 * The finish screen's "Save changes to workout?" (SPEC §7.3): compares what
 * you actually did with the workout it started from.
 */
object TemplateSync {
    private data class Slot(val exerciseId: String, val sets: Int, val warmup: Boolean)

    /**
     * An exercise whose sets are all warm-ups is a warm-up exercise and counts
     * every set; otherwise only working sets count, like a template's target.
     */
    private fun sessionPlan(session: SessionWithExercises): List<Slot> =
        session.sortedExercises.map { se ->
            val warmup = se.sets.isNotEmpty() && se.sets.all { it.isWarmup }
            val sets = if (warmup) se.sets.size else se.sets.count { !it.isWarmup }
            Slot(se.exercise.id, sets.coerceAtLeast(1), warmup)
        }

    private fun templatePlan(template: TemplateWithExercises): List<Slot> =
        template.sortedExercises.map {
            Slot(it.exercise.id, it.templateExercise.targetSets, it.templateExercise.isWarmup)
        }

    /** True when the exercise order, set counts or warm-up choices changed. */
    fun differs(session: SessionWithExercises, template: TemplateWithExercises): Boolean =
        sessionPlan(session) != templatePlan(template)

    /**
     * The workout rebuilt from the session. Rep targets carry over for
     * exercises already in it; new ones get their recommended range.
     */
    fun drafts(session: SessionWithExercises, template: TemplateWithExercises): List<TemplateExerciseDraft> {
        val existing = template.exercises.associateBy { it.exercise.id }
        return session.sortedExercises.zip(sessionPlan(session)) { se, slot ->
            val old = existing[se.exercise.id]?.templateExercise
            TemplateExerciseDraft(
                exerciseId = se.exercise.id,
                targetSets = slot.sets,
                targetRepsMin = old?.targetRepsMin ?: se.exercise.recommendedRepsMin,
                targetRepsMax = old?.targetRepsMax ?: se.exercise.recommendedRepsMax,
                targetDurationSec = old?.targetDurationSec ?: se.exercise.recommendedDurationSec,
                targetDistanceM = old?.targetDistanceM ?: se.exercise.recommendedDistanceM,
                notes = old?.notes,
                isWarmup = slot.warmup,
            )
        }
    }
}
