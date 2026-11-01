package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.data.TemplateExerciseDraft
import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.db.TemplateWithExercises

/**
 * The finish screen's "Save changes to workout?" (SPEC §7.3): compares what
 * you actually did with the workout it started from.
 */
object TemplateSync {
    private fun sessionPlan(session: SessionWithExercises): List<Pair<String, Int>> =
        session.sortedExercises.map { se ->
            se.exercise.id to se.sets.count { !it.isWarmup }.coerceAtLeast(1)
        }

    private fun templatePlan(template: TemplateWithExercises): List<Pair<String, Int>> =
        template.sortedExercises.map { it.exercise.id to it.templateExercise.targetSets }

    /** True when the exercise order or working-set counts changed. */
    fun differs(session: SessionWithExercises, template: TemplateWithExercises): Boolean =
        sessionPlan(session) != templatePlan(template)

    /**
     * The workout rebuilt from the session. Rep targets carry over for
     * exercises already in it; new ones get their recommended range.
     */
    fun drafts(session: SessionWithExercises, template: TemplateWithExercises): List<TemplateExerciseDraft> {
        val existing = template.exercises.associateBy { it.exercise.id }
        return session.sortedExercises.zip(sessionPlan(session)) { se, (_, sets) ->
            val old = existing[se.exercise.id]?.templateExercise
            TemplateExerciseDraft(
                exerciseId = se.exercise.id,
                targetSets = sets,
                targetRepsMin = old?.targetRepsMin ?: se.exercise.recommendedRepsMin,
                targetRepsMax = old?.targetRepsMax ?: se.exercise.recommendedRepsMax,
                targetDurationSec = old?.targetDurationSec ?: se.exercise.recommendedDurationSec,
                targetDistanceM = old?.targetDistanceM ?: se.exercise.recommendedDistanceM,
                notes = old?.notes,
            )
        }
    }
}
