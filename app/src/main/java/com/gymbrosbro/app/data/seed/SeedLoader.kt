package com.gymbrosbro.app.data.seed

import android.content.res.AssetManager
import androidx.room.withTransaction
import com.gymbrosbro.app.data.db.GymDatabase
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Merges the bundled exercise library into the database whenever the asset's
 * version is newer than what was last merged (SPEC §6).
 */
class SeedLoader(
    private val assets: AssetManager,
    private val database: GymDatabase,
    private val settings: SettingsRepository,
) {
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val dao = database.exerciseDao()
        val seed = readSeed()
        val upToDate = settings.seedVersion.first() >= seed.version && dao.countSeed() > 0
        if (upToDate) return@withContext

        val existing = dao.getSeedExercises().associateBy { it.id }
        val merged = mergeSeed(seed.exercises.map { it.toEntity() }, existing)
        database.withTransaction { dao.upsertAll(merged) }
        settings.setSeedVersion(seed.version)
    }

    private fun readSeed(): SeedFile =
        assets.open(SEED_ASSET).bufferedReader().use { json.decodeFromString(it.readText()) }

    companion object {
        const val SEED_ASSET = "exercises.json"

        val json = Json { ignoreUnknownKeys = true }

        /**
         * Seed content wins, but the user's flags on existing rows survive.
         * Exercises dropped from a newer seed are left in place, since history
         * may reference them.
         */
        fun mergeSeed(
            incoming: List<ExerciseEntity>,
            existing: Map<String, ExerciseEntity>,
        ): List<ExerciseEntity> = incoming.map { new ->
            val old = existing[new.id] ?: return@map new
            new.copy(
                isFavorite = old.isFavorite,
                isHidden = old.isHidden,
                userNotes = old.userNotes,
                archivedAt = old.archivedAt,
            )
        }
    }
}
