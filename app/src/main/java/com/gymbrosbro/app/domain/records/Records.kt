package com.gymbrosbro.app.domain.records

import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.domain.model.TrackingType
import java.time.Instant

/** What a personal record measures (SPEC §4.5). Persisted nowhere, so renaming is safe. */
enum class RecordType {
    /** Heaviest single set. */
    MAX_WEIGHT,

    /** Best estimated one-rep max (Epley, sets of 12 reps or fewer). */
    BEST_E1RM,

    /** Most reps in one set (bodyweight exercises). */
    MAX_REPS,

    /** Most weight × reps across a whole workout. */
    MAX_SESSION_VOLUME,

    /** Most reps across a whole workout (bodyweight exercises). */
    MAX_SESSION_REPS,
    LONGEST_DURATION,
    LONGEST_DISTANCE,
}

/** One time an exercise was done: the workout it was in and its sets. */
data class Performance(val sessionId: Long, val at: Instant, val sets: List<SetEntryEntity>)

/** A workout that beat every earlier workout on [type]. */
data class RecordEvent(
    val type: RecordType,
    val value: Double,
    val previous: Double,
    val sessionId: Long,
    val at: Instant,
)

data class Best(val value: Double, val sessionId: Long, val at: Instant)

data class RecordHistory(val bests: Map<RecordType, Best>, val events: List<RecordEvent>)

/**
 * Personal records, derived from history rather than stored, so editing an
 * old workout corrects them automatically. Only completed working sets count;
 * warm-ups never set a record (SPEC §7.3). The first time you do an exercise
 * sets your baseline and isn't celebrated, and a tie isn't a new record.
 */
object Records {
    /** Above this many reps the 1RM estimate is too unreliable to use. */
    const val E1RM_MAX_REPS = 12

    /** Epley: w × (1 + r/30). A single is just its weight. */
    fun e1rm(weightKg: Double, reps: Int): Double? = when {
        reps < 1 || reps > E1RM_MAX_REPS || weightKg <= 0 -> null
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps / 30.0)
    }

    /** Records a single set can break; these trigger NEW HIGH SCORE as you log. */
    fun setTypes(type: TrackingType): List<RecordType> = when (type) {
        TrackingType.WEIGHT_REPS -> listOf(RecordType.MAX_WEIGHT, RecordType.BEST_E1RM)
        TrackingType.BODYWEIGHT_REPS -> listOf(RecordType.MAX_REPS)
        TrackingType.DURATION -> listOf(RecordType.LONGEST_DURATION)
        TrackingType.DISTANCE_DURATION -> listOf(RecordType.LONGEST_DISTANCE)
        TrackingType.WEIGHT_DISTANCE -> listOf(RecordType.MAX_WEIGHT)
    }

    /** Records that only make sense for a whole workout; shown on the finish screen. */
    fun sessionTypes(type: TrackingType): List<RecordType> = when (type) {
        TrackingType.WEIGHT_REPS -> listOf(RecordType.MAX_SESSION_VOLUME)
        TrackingType.BODYWEIGHT_REPS -> listOf(RecordType.MAX_SESSION_REPS)
        else -> emptyList()
    }

    fun types(type: TrackingType) = setTypes(type) + sessionTypes(type)

    fun setValue(record: RecordType, set: SetEntryEntity): Double? = when (record) {
        RecordType.MAX_WEIGHT -> set.weightKg?.takeIf { it > 0 }
        RecordType.BEST_E1RM -> set.weightKg?.let { w -> set.reps?.let { r -> e1rm(w, r) } }
        RecordType.MAX_REPS -> set.reps?.takeIf { it > 0 }?.toDouble()
        RecordType.LONGEST_DURATION -> set.durationSec?.takeIf { it > 0 }?.toDouble()
        RecordType.LONGEST_DISTANCE -> set.distanceM?.takeIf { it > 0 }
        RecordType.MAX_SESSION_VOLUME, RecordType.MAX_SESSION_REPS -> null
    }

    private fun working(sets: List<SetEntryEntity>) = sets.filter { it.isCompleted && !it.isWarmup }

    /** A workout's best value for [record], or null if nothing in it counts. */
    fun sessionValue(record: RecordType, sets: List<SetEntryEntity>): Double? {
        val done = working(sets)
        return when (record) {
            RecordType.MAX_SESSION_VOLUME -> done.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }.takeIf { it > 0 }
            RecordType.MAX_SESSION_REPS -> done.sumOf { it.reps ?: 0 }.takeIf { it > 0 }?.toDouble()
            else -> done.mapNotNull { setValue(record, it) }.maxOrNull()
        }
    }

    /** Walks [performances] oldest-first, tracking bests and the workouts that beat them. */
    fun history(type: TrackingType, performances: List<Performance>): RecordHistory {
        val bests = mutableMapOf<RecordType, Best>()
        val events = mutableListOf<RecordEvent>()
        val records = types(type)
        for (p in performances.sortedBy { it.at }) {
            if (working(p.sets).isEmpty()) continue
            for (record in records) {
                val value = sessionValue(record, p.sets) ?: continue
                val previous = bests[record]
                if (previous == null || value > previous.value + EPSILON) {
                    if (previous != null) {
                        events += RecordEvent(record, value, previous.value, p.sessionId, p.at)
                    }
                    bests[record] = Best(value, p.sessionId, p.at)
                }
            }
        }
        return RecordHistory(bests, events)
    }

    /**
     * Set records [set] just broke, for the live NEW HIGH SCORE pop-up.
     * [priorBests] come from earlier workouts; [otherSets] are the rest of
     * this exercise's sets in the current workout. Empty with no history.
     */
    fun brokenBy(
        type: TrackingType,
        set: SetEntryEntity,
        priorBests: Map<RecordType, Double>,
        otherSets: List<SetEntryEntity>,
    ): List<RecordHit> {
        if (!set.isCompleted || set.isWarmup) return emptyList()
        val others = working(otherSets).filter { it.id != set.id }
        return setTypes(type).mapNotNull { record ->
            val value = setValue(record, set) ?: return@mapNotNull null
            val prior = priorBests[record] ?: return@mapNotNull null
            val bar = maxOf(prior, others.mapNotNull { setValue(record, it) }.maxOrNull() ?: prior)
            if (value > bar + EPSILON) RecordHit(record, value, bar) else null
        }
    }

    /** Every record event across all exercises in [sessions], oldest first. */
    fun allEvents(sessions: List<SessionWithExercises>): List<ExerciseRecord> {
        val byExercise = mutableMapOf<String, Pair<ExerciseEntity, MutableList<Performance>>>()
        for (s in sessions) {
            for (e in s.exercises) {
                val list = byExercise.getOrPut(e.exercise.id) { e.exercise to mutableListOf() }.second
                // The same exercise twice in one workout counts as one performance.
                val last = list.lastOrNull()
                if (last?.sessionId == s.session.id) {
                    list[list.lastIndex] = last.copy(sets = last.sets + e.sets)
                } else {
                    list += Performance(s.session.id, s.session.startedAt, e.sets)
                }
            }
        }
        return byExercise.values
            .flatMap { (exercise, performances) ->
                history(exercise.trackingType, performances).events.map { ExerciseRecord(exercise, it) }
            }
            .sortedBy { it.event.at }
    }

    private const val EPSILON = 1e-6
}

data class ExerciseRecord(val exercise: ExerciseEntity, val event: RecordEvent)

data class RecordHit(val type: RecordType, val value: Double, val previous: Double)
