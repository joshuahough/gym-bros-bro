package com.gymbrosbro.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymbrosbro.app.data.db.entity.EarnedTrophyEntity
import com.gymbrosbro.app.data.db.entity.UnlockedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AwardsDao {
    @Query("SELECT * FROM earned_trophy ORDER BY earnedAt")
    fun observeTrophies(): Flow<List<EarnedTrophyEntity>>

    @Query("SELECT trophyId FROM earned_trophy")
    suspend fun trophyIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrophies(trophies: List<EarnedTrophyEntity>)

    @Query("SELECT * FROM unlocked_item ORDER BY unlockedAt")
    fun observeUnlocks(): Flow<List<UnlockedItemEntity>>

    @Query("SELECT itemId FROM unlocked_item")
    suspend fun unlockedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlocks(items: List<UnlockedItemEntity>)
}
