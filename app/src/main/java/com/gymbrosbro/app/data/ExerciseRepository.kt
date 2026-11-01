package com.gymbrosbro.app.data

import com.gymbrosbro.app.data.db.ExerciseDao
import com.gymbrosbro.app.data.db.ExercisePerformance
import com.gymbrosbro.app.data.db.SessionDao
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.domain.model.ExerciseSource
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

class ExerciseRepository(
    private val dao: ExerciseDao,
    private val sessionDao: SessionDao,
) {
    fun observeLibrary(): Flow<List<ExerciseEntity>> = dao.observeVisible()

    fun observeExercise(id: String): Flow<ExerciseEntity?> = dao.observeById(id)

    suspend fun getExercise(id: String): ExerciseEntity? = dao.getById(id)

    fun observeHistory(exerciseId: String, limit: Int = 5): Flow<List<ExercisePerformance>> =
        sessionDao.observePerformances(exerciseId, limit)

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite)

    /** Creates or replaces a custom exercise; seed exercises are never written here. */
    suspend fun saveCustom(exercise: ExerciseEntity) {
        require(exercise.source == ExerciseSource.CUSTOM) { "Only custom exercises can be edited" }
        dao.upsertAll(listOf(exercise))
    }

    /** Custom exercises are archived rather than deleted, since history may use them. */
    suspend fun archiveCustom(id: String) = dao.archiveCustom(id, Instant.now())

    companion object {
        fun newCustomId(): String = "custom-${UUID.randomUUID()}"
    }
}
