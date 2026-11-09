package com.gymbrosbro.app.data.backup

import androidx.room.withTransaction
import com.gymbrosbro.app.data.AwardsRepository
import com.gymbrosbro.app.data.db.GymDatabase
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import com.gymbrosbro.app.data.db.entity.EarnedTrophyEntity
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleExceptionEntity
import com.gymbrosbro.app.data.db.entity.SchedulePauseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleRuleEntity
import com.gymbrosbro.app.data.db.entity.SessionExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.TemplateExerciseEntity
import com.gymbrosbro.app.data.db.entity.UnlockedItemEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.data.db.entity.WorkoutTemplateEntity
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.data.settings.SettingsSnapshot
import com.gymbrosbro.app.domain.model.ExerciseSource
import com.gymbrosbro.app.domain.model.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate

/**
 * A whole-app backup file (SPEC §9): every table the user can change, plus
 * settings. Seed exercises are only included when the user flagged or used
 * them, since the app ships the rest.
 */
@Serializable
data class BackupFile(
    val app: String = APP_ID,
    val format: Int = FORMAT,
    /** Room schema version at export, for converting old files later. */
    val dbVersion: Int,
    @Serializable(with = InstantSerializer::class) val exportedAt: Instant,
    val settings: SettingsSnapshot = SettingsSnapshot(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val templateExercises: List<TemplateExerciseEntity> = emptyList(),
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val sessionExercises: List<SessionExerciseEntity> = emptyList(),
    val sets: List<SetEntryEntity> = emptyList(),
    val bodyweight: List<BodyweightEntryEntity> = emptyList(),
    val scheduleRules: List<ScheduleRuleEntity> = emptyList(),
    val scheduleExceptions: List<ScheduleExceptionEntity> = emptyList(),
    val schedulePauses: List<SchedulePauseEntity> = emptyList(),
    val trophies: List<EarnedTrophyEntity> = emptyList(),
    val unlocks: List<UnlockedItemEntity> = emptyList(),
) {
    val workoutCount: Int get() = sessions.count { it.status == SessionStatus.COMPLETED }

    companion object {
        const val APP_ID = "gymbrosbro"
        const val FORMAT = 1
    }
}

/** A file that isn't a backup, or is from a newer version of the app. */
class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

class BackupRepository(
    private val database: GymDatabase,
    private val settings: SettingsRepository,
    private val awards: AwardsRepository,
) {
    private val dao = database.backupDao()

    /** Whether there's anything worth protecting, e.g. before skipping first-run setup. */
    suspend fun hasUserData(): Boolean = withContext(Dispatchers.IO) { dao.userRowCount() > 0 }

    suspend fun export(out: OutputStream) = withContext(Dispatchers.IO) {
        val file = snapshot()
        out.bufferedWriter().use { it.write(json.encodeToString(file)) }
    }

    suspend fun snapshot(): BackupFile {
        val file = database.withTransaction {
            val templateExercises = dao.templateExercises()
            val sessionExercises = dao.sessionExercises()
            val used = templateExercises.mapTo(mutableSetOf()) { it.exerciseId } +
                sessionExercises.map { it.exerciseId }
            BackupFile(
                dbVersion = database.openHelper.readableDatabase.version,
                exportedAt = Instant.now(),
                exercises = exercisesToExport(dao.exercises(), used),
                templates = dao.templates(),
                templateExercises = templateExercises,
                sessions = dao.sessions(),
                sessionExercises = sessionExercises,
                sets = dao.sets(),
                bodyweight = dao.bodyweight(),
                scheduleRules = dao.scheduleRules(),
                scheduleExceptions = dao.scheduleExceptions(),
                schedulePauses = dao.schedulePauses(),
                trophies = dao.trophies(),
                unlocks = dao.unlocks(),
            )
        }
        return file.copy(settings = settings.snapshot())
    }

    /** Reads and checks a file without changing anything, so the user can confirm first. */
    suspend fun read(input: InputStream): BackupFile = withContext(Dispatchers.IO) {
        parse(input.bufferedReader().use { it.readText() })
    }

    /**
     * Replaces everything with [file]. The database part is one transaction,
     * so a file that doesn't fit leaves the current data untouched.
     */
    suspend fun restore(file: BackupFile) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.clearSets()
            dao.clearSessionExercises()
            dao.clearSessions()
            dao.clearTemplateExercises()
            dao.clearScheduleExceptions()
            dao.clearScheduleRules()
            dao.clearSchedulePauses()
            dao.clearTemplates()
            dao.clearBodyweight()
            dao.clearTrophies()
            dao.clearUnlocks()
            dao.clearCustomExercises()

            val currentSeed = dao.exercises().filter { it.source == ExerciseSource.SEED }
            dao.upsertExercises(exercisesToRestore(file.exercises, currentSeed))
            dao.insertTemplates(file.templates)
            dao.insertTemplateExercises(file.templateExercises)
            dao.insertSessions(file.sessions)
            dao.insertSessionExercises(file.sessionExercises)
            dao.insertSets(file.sets)
            dao.insertBodyweight(file.bodyweight)
            dao.insertScheduleRules(file.scheduleRules)
            dao.insertScheduleExceptions(file.scheduleExceptions)
            dao.insertSchedulePauses(file.schedulePauses)
            dao.insertTrophies(file.trophies)
            dao.insertUnlocks(file.unlocks)
        }
        settings.restore(file.settings)
        // Anything the restored history qualifies for but the file predates.
        awards.sync(sessionId = null)
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fileName(today: LocalDate = LocalDate.now()) = "gymbrosbro-backup-$today.json"

        fun parse(text: String): BackupFile {
            val file = try {
                json.decodeFromString<BackupFile>(text)
            } catch (e: SerializationException) {
                throw BackupFormatException("This isn't a Gym Bros Bro backup file.", e)
            } catch (e: IllegalArgumentException) {
                throw BackupFormatException("This isn't a Gym Bros Bro backup file.", e)
            }
            if (file.app != BackupFile.APP_ID) throw BackupFormatException("This isn't a Gym Bros Bro backup file.")
            if (file.format > BackupFile.FORMAT) {
                throw BackupFormatException("This backup is from a newer version of the app. Update the app, then try again.")
            }
            return file
        }

        /** Custom exercises, plus library ones the user flagged or logged. */
        fun exercisesToExport(all: List<ExerciseEntity>, usedIds: Set<String>): List<ExerciseEntity> =
            all.filter { e ->
                e.source == ExerciseSource.CUSTOM ||
                    e.id in usedIds ||
                    e.isFavorite || e.isHidden || e.userNotes != null || e.archivedAt != null
            }

        /**
         * The exercise rows to write on restore. Library rows keep the installed
         * library's content but take the backup's flags (cleared when the backup
         * doesn't mention them). Backup rows for library exercises this install
         * doesn't have, and all custom ones, go in as they are.
         */
        fun exercisesToRestore(backup: List<ExerciseEntity>, currentSeed: List<ExerciseEntity>): List<ExerciseEntity> {
            val fromBackup = backup.associateBy { it.id }
            val seedIds = currentSeed.mapTo(mutableSetOf()) { it.id }
            val seed = currentSeed.map { row ->
                val saved = fromBackup[row.id]
                row.copy(
                    isFavorite = saved?.isFavorite ?: false,
                    isHidden = saved?.isHidden ?: false,
                    userNotes = saved?.userNotes,
                    archivedAt = saved?.archivedAt,
                )
            }
            return seed + backup.filter { it.id !in seedIds }
        }
    }
}
