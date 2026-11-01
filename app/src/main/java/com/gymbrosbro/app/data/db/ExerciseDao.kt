package com.gymbrosbro.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE archivedAt IS NULL AND isHidden = 0 ORDER BY name COLLATE NOCASE")
    fun observeVisible(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE source = 'SEED'")
    suspend fun getSeedExercises(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercise WHERE source = 'SEED'")
    suspend fun countSeed(): Int

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)
}
