package com.gymbrosbro.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import com.gymbrosbro.app.data.db.entity.EarnedTrophyEntity
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleExceptionEntity
import com.gymbrosbro.app.data.db.entity.SchedulePauseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleRuleEntity
import com.gymbrosbro.app.data.db.entity.SessionExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.TemplateExerciseEntity
import com.gymbrosbro.app.data.db.entity.UnlockedItemEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.data.db.entity.WorkoutTemplateEntity

/** Whole-table reads and writes for export/import (SPEC §9). Use inside one transaction. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM exercise") suspend fun exercises(): List<ExerciseEntity>
    @Query("SELECT * FROM workout_template") suspend fun templates(): List<WorkoutTemplateEntity>
    @Query("SELECT * FROM template_exercise") suspend fun templateExercises(): List<TemplateExerciseEntity>
    @Query("SELECT * FROM workout_session") suspend fun sessions(): List<WorkoutSessionEntity>
    @Query("SELECT * FROM session_exercise") suspend fun sessionExercises(): List<SessionExerciseEntity>
    @Query("SELECT * FROM set_entry") suspend fun sets(): List<SetEntryEntity>
    @Query("SELECT * FROM bodyweight_entry") suspend fun bodyweight(): List<BodyweightEntryEntity>
    @Query("SELECT * FROM schedule_rule") suspend fun scheduleRules(): List<ScheduleRuleEntity>
    @Query("SELECT * FROM schedule_exception") suspend fun scheduleExceptions(): List<ScheduleExceptionEntity>
    @Query("SELECT * FROM schedule_pause") suspend fun schedulePauses(): List<SchedulePauseEntity>
    @Query("SELECT * FROM earned_trophy") suspend fun trophies(): List<EarnedTrophyEntity>
    @Query("SELECT * FROM unlocked_item") suspend fun unlocks(): List<UnlockedItemEntity>

    /** Anything the user made, as opposed to the bundled library. */
    @Query(
        """
        SELECT (SELECT COUNT(*) FROM workout_session) + (SELECT COUNT(*) FROM workout_template)
             + (SELECT COUNT(*) FROM schedule_rule) + (SELECT COUNT(*) FROM bodyweight_entry)
             + (SELECT COUNT(*) FROM exercise WHERE source = 'CUSTOM')
        """,
    )
    suspend fun userRowCount(): Int

    // Children before parents, so no foreign key blocks a delete.
    @Query("DELETE FROM set_entry") suspend fun clearSets()
    @Query("DELETE FROM session_exercise") suspend fun clearSessionExercises()
    @Query("DELETE FROM workout_session") suspend fun clearSessions()
    @Query("DELETE FROM template_exercise") suspend fun clearTemplateExercises()
    @Query("DELETE FROM schedule_exception") suspend fun clearScheduleExceptions()
    @Query("DELETE FROM schedule_rule") suspend fun clearScheduleRules()
    @Query("DELETE FROM schedule_pause") suspend fun clearSchedulePauses()
    @Query("DELETE FROM workout_template") suspend fun clearTemplates()
    @Query("DELETE FROM bodyweight_entry") suspend fun clearBodyweight()
    @Query("DELETE FROM earned_trophy") suspend fun clearTrophies()
    @Query("DELETE FROM unlocked_item") suspend fun clearUnlocks()
    @Query("DELETE FROM exercise WHERE source = 'CUSTOM'") suspend fun clearCustomExercises()

    @Upsert suspend fun upsertExercises(rows: List<ExerciseEntity>)
    @Insert suspend fun insertTemplates(rows: List<WorkoutTemplateEntity>)
    @Insert suspend fun insertTemplateExercises(rows: List<TemplateExerciseEntity>)
    @Insert suspend fun insertSessions(rows: List<WorkoutSessionEntity>)
    @Insert suspend fun insertSessionExercises(rows: List<SessionExerciseEntity>)
    @Insert suspend fun insertSets(rows: List<SetEntryEntity>)
    @Insert suspend fun insertBodyweight(rows: List<BodyweightEntryEntity>)
    @Insert suspend fun insertScheduleRules(rows: List<ScheduleRuleEntity>)
    @Insert suspend fun insertScheduleExceptions(rows: List<ScheduleExceptionEntity>)
    @Insert suspend fun insertSchedulePauses(rows: List<SchedulePauseEntity>)
    @Insert suspend fun insertTrophies(rows: List<EarnedTrophyEntity>)
    @Insert suspend fun insertUnlocks(rows: List<UnlockedItemEntity>)
}
