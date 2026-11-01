package com.gymbrosbro.app.data

import com.gymbrosbro.app.data.db.ExerciseDao
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val dao: ExerciseDao) {
    fun observeLibrary(): Flow<List<ExerciseEntity>> = dao.observeVisible()
}
