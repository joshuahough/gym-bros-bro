package com.gymbrosbro.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymbrosbro.app.data.db.entity.TemplateExerciseEntity
import com.gymbrosbro.app.data.db.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TemplateDao {
    @Transaction
    @Query("SELECT * FROM workout_template WHERE archivedAt IS NULL ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeTemplates(): Flow<List<TemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_template WHERE id = :id")
    suspend fun getTemplateWithExercises(id: Long): TemplateWithExercises?

    @Query("SELECT * FROM workout_template WHERE id = :id")
    suspend fun getTemplate(id: Long): WorkoutTemplateEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM workout_template")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: WorkoutTemplateEntity)

    @Query("UPDATE workout_template SET archivedAt = :at WHERE id = :id")
    suspend fun archive(id: Long, at: Instant)

    @Query("DELETE FROM template_exercise WHERE templateId = :templateId")
    suspend fun clearExercises(templateId: Long)

    @Insert
    suspend fun insertExercises(exercises: List<TemplateExerciseEntity>)
}
