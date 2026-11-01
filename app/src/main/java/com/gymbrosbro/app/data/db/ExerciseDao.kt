package com.gymbrosbro.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE archivedAt IS NULL AND isHidden = 0 ORDER BY name COLLATE NOCASE")
    fun observeVisible(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE source = 'SEED'")
    suspend fun getSeedExercises(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercise WHERE source = 'SEED'")
    suspend fun countSeed(): Int

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Query("UPDATE exercise SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE exercise SET archivedAt = :at WHERE id = :id AND source = 'CUSTOM'")
    suspend fun archiveCustom(id: String, at: Instant)
}
