package com.gymbrosbro.app.data

import com.gymbrosbro.app.data.db.BodyweightDao
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class BodyweightRepository(private val dao: BodyweightDao) {
    fun observeLatest(): Flow<BodyweightEntryEntity?> = dao.observeLatest()

    suspend fun getLatest(): BodyweightEntryEntity? = dao.getLatest()

    /** One entry per day; recording again the same day replaces it. */
    suspend fun record(weightKg: Double, date: LocalDate = LocalDate.now()) =
        dao.upsert(BodyweightEntryEntity(date = date, weightKg = weightKg))
}
