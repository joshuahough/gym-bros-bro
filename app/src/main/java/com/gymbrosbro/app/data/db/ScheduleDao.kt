package com.gymbrosbro.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.gymbrosbro.app.data.db.entity.ScheduleExceptionEntity
import com.gymbrosbro.app.data.db.entity.SchedulePauseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleRuleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_rule ORDER BY startDate, id")
    fun observeRules(): Flow<List<ScheduleRuleEntity>>

    @Query("SELECT * FROM schedule_rule ORDER BY startDate, id")
    suspend fun getRules(): List<ScheduleRuleEntity>

    @Query("SELECT * FROM schedule_rule WHERE id = :id")
    suspend fun getRule(id: Long): ScheduleRuleEntity?

    @Insert
    suspend fun insertRule(rule: ScheduleRuleEntity): Long

    @Update
    suspend fun updateRule(rule: ScheduleRuleEntity)

    @Query("DELETE FROM schedule_rule WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("SELECT * FROM schedule_exception")
    fun observeExceptions(): Flow<List<ScheduleExceptionEntity>>

    @Query("SELECT * FROM schedule_exception")
    suspend fun getExceptions(): List<ScheduleExceptionEntity>

    @Upsert
    suspend fun upsertException(exception: ScheduleExceptionEntity)

    @Query("SELECT * FROM schedule_exception WHERE ruleId = :ruleId AND originalDate = :originalDate")
    suspend fun getException(ruleId: Long, originalDate: LocalDate): ScheduleExceptionEntity?

    @Query("DELETE FROM schedule_exception WHERE ruleId = :ruleId AND originalDate = :originalDate")
    suspend fun deleteException(ruleId: Long, originalDate: LocalDate)

    @Query("DELETE FROM schedule_exception WHERE ruleId = :ruleId AND originalDate > :after")
    suspend fun deleteExceptionsAfter(ruleId: Long, after: LocalDate)

    @Query("SELECT * FROM schedule_pause ORDER BY startDate")
    fun observePauses(): Flow<List<SchedulePauseEntity>>

    @Query("SELECT * FROM schedule_pause ORDER BY startDate")
    suspend fun getPauses(): List<SchedulePauseEntity>

    @Insert
    suspend fun insertPause(pause: SchedulePauseEntity)

    @Update
    suspend fun updatePause(pause: SchedulePauseEntity)

    @Query("DELETE FROM schedule_pause WHERE id = :id")
    suspend fun deletePause(id: Long)
}
