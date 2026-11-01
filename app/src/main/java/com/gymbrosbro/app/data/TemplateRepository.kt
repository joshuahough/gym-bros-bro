package com.gymbrosbro.app.data

import androidx.room.withTransaction
import com.gymbrosbro.app.data.db.GymDatabase
import com.gymbrosbro.app.data.db.TemplateWithExercises
import com.gymbrosbro.app.data.db.entity.TemplateExerciseEntity
import com.gymbrosbro.app.data.db.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** One exercise slot in a workout being saved. */
data class TemplateExerciseDraft(
    val exerciseId: String,
    val targetSets: Int,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationSec: Int? = null,
    val targetDistanceM: Double? = null,
    val notes: String? = null,
)

class TemplateRepository(private val database: GymDatabase) {
    private val dao = database.templateDao()

    fun observeTemplates(): Flow<List<TemplateWithExercises>> = dao.observeTemplates()

    fun observeAll(): Flow<List<WorkoutTemplateEntity>> = dao.observeAll()

    suspend fun getTemplate(id: Long): TemplateWithExercises? = dao.getTemplateWithExercises(id)

    /** Inserts ([id] == null) or replaces a workout and its exercise list. Returns its id. */
    suspend fun save(id: Long?, name: String, exercises: List<TemplateExerciseDraft>): Long =
        database.withTransaction {
            val templateId = if (id == null) {
                dao.insertTemplate(
                    WorkoutTemplateEntity(name = name, sortOrder = dao.maxSortOrder() + 1, createdAt = Instant.now()),
                )
            } else {
                val existing = checkNotNull(dao.getTemplate(id)) { "Workout $id not found" }
                dao.updateTemplate(existing.copy(name = name))
                id
            }
            dao.clearExercises(templateId)
            dao.insertExercises(
                exercises.mapIndexed { i, d ->
                    TemplateExerciseEntity(
                        templateId = templateId,
                        exerciseId = d.exerciseId,
                        position = i,
                        targetSets = d.targetSets,
                        targetRepsMin = d.targetRepsMin,
                        targetRepsMax = d.targetRepsMax,
                        targetDurationSec = d.targetDurationSec,
                        targetDistanceM = d.targetDistanceM,
                        notes = d.notes,
                    )
                },
            )
            templateId
        }

    /** Archived, not deleted, so past sessions keep their link. */
    suspend fun archive(id: Long) = dao.archive(id, Instant.now())
}
