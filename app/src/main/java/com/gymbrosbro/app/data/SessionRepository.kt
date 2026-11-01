package com.gymbrosbro.app.data

import androidx.room.withTransaction
import com.gymbrosbro.app.data.db.ExercisePerformance
import com.gymbrosbro.app.data.db.GymDatabase
import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.db.entity.SessionExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.domain.model.SessionStatus
import com.gymbrosbro.app.domain.session.Prefill
import com.gymbrosbro.app.domain.session.SetDraft
import com.gymbrosbro.app.domain.session.TemplateSync
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Logging workouts: starting, editing sets, finishing, and editing history. */
class SessionRepository(
    private val database: GymDatabase,
    private val templates: TemplateRepository,
    private val bodyweight: BodyweightRepository,
) {
    private val dao = database.sessionDao()
    private val exerciseDao = database.exerciseDao()

    fun observeInProgress(): Flow<WorkoutSessionEntity?> = dao.observeInProgress()

    fun observeLatestCompleted(): Flow<WorkoutSessionEntity?> = dao.observeLatestCompleted()

    suspend fun getInProgress(): WorkoutSessionEntity? = dao.getInProgress()

    fun observeSession(id: Long): Flow<SessionWithExercises?> = dao.observeDetail(id)

    suspend fun getSession(id: Long): SessionWithExercises? = dao.getDetail(id)

    fun observeCompleted(): Flow<List<SessionWithExercises>> = dao.observeCompleted()

    /** Completed workouts started on or after [date] (local time). */
    suspend fun completedSince(date: LocalDate): List<WorkoutSessionEntity> =
        dao.getCompletedSince(date.atStartOfDay(ZoneId.systemDefault()).toInstant())

    // Starting

    suspend fun startFromTemplate(templateId: Long): Long = database.withTransaction {
        val template = checkNotNull(templates.getTemplate(templateId)) { "Workout $templateId not found" }
        val sessionId = dao.insertSession(newSession(templateId, template.template.name))
        template.sortedExercises.forEachIndexed { i, te ->
            addExercise(sessionId, te.exercise.id, i, te.templateExercise.targetSets)
        }
        sessionId
    }

    suspend fun startEmpty(): Long = dao.insertSession(newSession(templateId = null, name = "Quick Workout"))

    /** A past workout logged after the fact; it starts completed and empty. */
    suspend fun createBackfill(date: LocalDate): Long {
        val at = date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant()
        return dao.insertSession(
            WorkoutSessionEntity(
                templateId = null,
                name = "Logged Workout",
                startedAt = at,
                endedAt = at,
                status = SessionStatus.COMPLETED,
                bodyweightKgSnapshot = bodyweight.getLatest()?.weightKg,
            ),
        )
    }

    private suspend fun newSession(templateId: Long?, name: String) = WorkoutSessionEntity(
        templateId = templateId,
        name = name,
        startedAt = Instant.now(),
        status = SessionStatus.IN_PROGRESS,
        bodyweightKgSnapshot = bodyweight.getLatest()?.weightKg,
    )

    // Exercises

    suspend fun addExercises(sessionId: Long, exerciseIds: List<String>) = database.withTransaction {
        var position = dao.maxExercisePosition(sessionId) + 1
        exerciseIds.forEach { addExercise(sessionId, it, position++, targetSets = null) }
    }

    /**
     * Adds an exercise with pre-filled rows. Set count: the workout's target,
     * else how many you did last time, else the exercise's recommendation.
     */
    private suspend fun addExercise(sessionId: Long, exerciseId: String, position: Int, targetSets: Int?) {
        val exercise = checkNotNull(exerciseDao.getById(exerciseId)) { "Exercise $exerciseId not found" }
        val sessionExerciseId = dao.insertSessionExercise(
            SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, position = position),
        )
        val last = lastSets(exerciseId, excludeSessionId = sessionId)
        val count = targetSets
            ?: last.count { it.isCompleted && !it.isWarmup }.takeIf { it > 0 }
            ?: exercise.recommendedSets
            ?: 3
        dao.insertSets(Prefill.sets(count, last).mapIndexed { i, d -> d.toEntity(sessionExerciseId, i) })
    }

    suspend fun lastSets(exerciseId: String, excludeSessionId: Long): List<SetEntryEntity> =
        dao.getPerformances(exerciseId, excludeSessionId, limit = 1).firstOrNull()?.sortedSets.orEmpty()

    /** Every earlier completed performance of [exerciseId], newest first. */
    suspend fun priorPerformances(exerciseId: String, session: WorkoutSessionEntity): List<ExercisePerformance> =
        dao.getPerformances(exerciseId, session.id, limit = Int.MAX_VALUE).filter { it.session.startedAt < session.startedAt }

    suspend fun removeExercise(sessionExerciseId: Long) = dao.deleteSessionExercise(sessionExerciseId)

    /** Moves an exercise up (-1) or down (+1) in the session. */
    suspend fun moveExercise(sessionId: Long, sessionExerciseId: Long, delta: Int) = database.withTransaction {
        val list = dao.getSessionExercises(sessionId).toMutableList()
        val from = list.indexOfFirst { it.id == sessionExerciseId }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@withTransaction
        list.add(to, list.removeAt(from))
        dao.updateSessionExercises(list.mapIndexed { i, e -> e.copy(position = i) })
    }

    // Sets

    suspend fun addSet(sessionExerciseId: Long) = database.withTransaction {
        val existing = dao.getSets(sessionExerciseId)
        val position = (existing.maxOfOrNull { it.position } ?: -1) + 1
        dao.insertSets(listOf(Prefill.nextSet(existing).toEntity(sessionExerciseId, position)))
    }

    /** Read-modify-write in a transaction so quick edits to different fields can't overwrite each other. */
    suspend fun updateSet(setId: Long, transform: (SetEntryEntity) -> SetEntryEntity): SetEntryEntity? =
        database.withTransaction {
            val current = dao.getSet(setId) ?: return@withTransaction null
            val updated = transform(current)
            if (updated != current) dao.updateSet(updated)
            updated
        }

    suspend fun getSets(sessionExerciseId: Long): List<SetEntryEntity> = dao.getSets(sessionExerciseId)

    /** Fills an exercise's unchecked working sets with a suggestion, in order; the last draft repeats. */
    suspend fun applyDrafts(sessionExerciseId: Long, drafts: List<SetDraft>) = database.withTransaction {
        if (drafts.isEmpty()) return@withTransaction
        dao.getSets(sessionExerciseId)
            .filter { !it.isCompleted && !it.isWarmup }
            .forEachIndexed { i, set ->
                val d = drafts.getOrElse(i) { drafts.last() }
                dao.updateSet(
                    set.copy(
                        weightKg = d.weightKg,
                        reps = d.reps,
                        addedWeightKg = d.addedWeightKg,
                        durationSec = d.durationSec,
                        distanceM = d.distanceM,
                    ),
                )
            }
    }

    /** Copies a just-completed set's numbers into the next row if that row is still empty. */
    suspend fun carryForward(setId: Long) = database.withTransaction {
        val done = dao.getSet(setId)?.takeIf { it.isCompleted } ?: return@withTransaction
        Prefill.nextToFill(done, dao.getSets(done.sessionExerciseId))?.let { dao.updateSet(it) }
    }

    suspend fun deleteSet(setId: Long) = dao.deleteSet(setId)

    // Finishing & editing

    /** Completes the session and drops sets that were never checked off. */
    suspend fun finish(sessionId: Long) = database.withTransaction {
        val session = dao.getSession(sessionId) ?: return@withTransaction
        dao.deleteIncompleteSets(sessionId)
        dao.deleteEmptySessionExercises(sessionId)
        dao.updateSession(
            session.copy(
                status = SessionStatus.COMPLETED,
                endedAt = Instant.now(),
                bodyweightKgSnapshot = session.bodyweightKgSnapshot ?: bodyweight.getLatest()?.weightKg,
            ),
        )
    }

    /** After editing a past workout: drop rows that were added but never checked off. */
    suspend fun pruneIncomplete(sessionId: Long) = database.withTransaction {
        dao.deleteIncompleteSets(sessionId)
        dao.deleteEmptySessionExercises(sessionId)
    }

    suspend fun delete(sessionId: Long) = dao.deleteSession(sessionId)

    suspend fun setBodyweightSnapshot(sessionId: Long, weightKg: Double) {
        val session = dao.getSession(sessionId) ?: return
        dao.updateSession(session.copy(bodyweightKgSnapshot = weightKg))
    }

    /** Moves a past workout to another day, keeping its time of day and length. */
    suspend fun changeDate(sessionId: Long, date: LocalDate) {
        val session = dao.getSession(sessionId) ?: return
        val zone = ZoneId.systemDefault()
        val start = session.startedAt.atZone(zone)
        val newStart = date.atTime(start.toLocalTime()).atZone(zone).toInstant()
        val length = session.endedAt?.let { Duration.between(session.startedAt, it) }
        dao.updateSession(session.copy(startedAt = newStart, endedAt = length?.let { newStart.plus(it) }))
    }

    /** Whether the session's exercises/set counts differ from the workout it came from. */
    suspend fun differsFromTemplate(session: SessionWithExercises): Boolean {
        val template = session.session.templateId?.let { templates.getTemplate(it) } ?: return false
        if (template.template.archivedAt != null) return false
        return TemplateSync.differs(session, template)
    }

    suspend fun saveToTemplate(sessionId: Long) {
        val session = dao.getDetail(sessionId) ?: return
        val templateId = session.session.templateId ?: return
        val template = templates.getTemplate(templateId) ?: return
        templates.save(templateId, template.template.name, TemplateSync.drafts(session, template))
    }

    private fun SetDraft.toEntity(sessionExerciseId: Long, position: Int) = SetEntryEntity(
        sessionExerciseId = sessionExerciseId,
        position = position,
        weightKg = weightKg,
        reps = reps,
        addedWeightKg = addedWeightKg,
        durationSec = durationSec,
        distanceM = distanceM,
    )
}
