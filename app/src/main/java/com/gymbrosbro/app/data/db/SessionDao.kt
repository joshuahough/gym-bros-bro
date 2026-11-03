package com.gymbrosbro.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymbrosbro.app.data.db.entity.SessionExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SessionDao {
    // Sessions

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_session WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    fun observeInProgress(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_session WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getInProgress(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_session WHERE status = 'COMPLETED' ORDER BY startedAt DESC LIMIT 1")
    fun observeLatestCompleted(): Flow<WorkoutSessionEntity?>

    @Transaction
    @Query("SELECT * FROM workout_session WHERE id = :id")
    fun observeDetail(id: Long): Flow<SessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun getDetail(id: Long): SessionWithExercises?

    @Transaction
    @Query("SELECT * FROM workout_session WHERE status = 'COMPLETED' ORDER BY startedAt DESC")
    fun observeCompleted(): Flow<List<SessionWithExercises>>

    @Query("SELECT * FROM workout_session WHERE status = 'COMPLETED' AND startedAt >= :from")
    suspend fun getCompletedSince(from: Instant): List<WorkoutSessionEntity>

    // Exercises within a session

    @Insert
    suspend fun insertSessionExercise(exercise: SessionExerciseEntity): Long

    @Update
    suspend fun updateSessionExercises(exercises: List<SessionExerciseEntity>)

    @Query("SELECT * FROM session_exercise WHERE id = :id")
    suspend fun getSessionExercise(id: Long): SessionExerciseEntity?

    @Query("DELETE FROM set_entry WHERE sessionExerciseId = :sessionExerciseId")
    suspend fun deleteSets(sessionExerciseId: Long)

    @Query("DELETE FROM session_exercise WHERE id = :id")
    suspend fun deleteSessionExercise(id: Long)

    @Query("SELECT * FROM session_exercise WHERE sessionId = :sessionId ORDER BY position")
    suspend fun getSessionExercises(sessionId: Long): List<SessionExerciseEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM session_exercise WHERE sessionId = :sessionId")
    suspend fun maxExercisePosition(sessionId: Long): Int

    // Sets

    @Insert
    suspend fun insertSets(sets: List<SetEntryEntity>)

    @Update
    suspend fun updateSet(set: SetEntryEntity)

    @Query("SELECT * FROM set_entry WHERE id = :id")
    suspend fun getSet(id: Long): SetEntryEntity?

    @Query("SELECT * FROM set_entry WHERE sessionExerciseId = :sessionExerciseId ORDER BY position")
    suspend fun getSets(sessionExerciseId: Long): List<SetEntryEntity>

    @Query("DELETE FROM set_entry WHERE id = :id")
    suspend fun deleteSet(id: Long)

    /** Drops sets that were never checked off (pre-filled but not done). */
    @Query(
        """DELETE FROM set_entry WHERE isCompleted = 0 AND sessionExerciseId IN
           (SELECT id FROM session_exercise WHERE sessionId = :sessionId)""",
    )
    suspend fun deleteIncompleteSets(sessionId: Long)

    @Query(
        """DELETE FROM session_exercise WHERE sessionId = :sessionId AND id NOT IN
           (SELECT DISTINCT sessionExerciseId FROM set_entry)""",
    )
    suspend fun deleteEmptySessionExercises(sessionId: Long)

    // History per exercise

    @Transaction
    @Query(
        """SELECT se.* FROM session_exercise se JOIN workout_session s ON s.id = se.sessionId
           WHERE se.exerciseId = :exerciseId AND s.status = 'COMPLETED' AND s.id != :excludeSessionId
           ORDER BY s.startedAt DESC, se.position DESC LIMIT :limit""",
    )
    suspend fun getPerformances(exerciseId: String, excludeSessionId: Long, limit: Int): List<ExercisePerformance>

    @Transaction
    @Query(
        """SELECT se.* FROM session_exercise se JOIN workout_session s ON s.id = se.sessionId
           WHERE se.exerciseId = :exerciseId AND s.status = 'COMPLETED'
           ORDER BY s.startedAt DESC, se.position DESC LIMIT :limit""",
    )
    fun observePerformances(exerciseId: String, limit: Int): Flow<List<ExercisePerformance>>
}
