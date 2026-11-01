package com.gymbrosbro.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyweightDao {
    @Upsert
    suspend fun upsert(entry: BodyweightEntryEntity)

    @Query("SELECT * FROM bodyweight_entry ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<BodyweightEntryEntity?>

    @Query("SELECT * FROM bodyweight_entry ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): BodyweightEntryEntity?
}
