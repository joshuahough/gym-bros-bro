package com.gymbrosbro.app.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SetEntryEntity::class,
        BodyweightEntryEntity::class,
        ScheduleRuleEntity::class,
        ScheduleExceptionEntity::class,
        SchedulePauseEntity::class,
        EarnedTrophyEntity::class,
        UnlockedItemEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        // 2: schedule tables.
        AutoMigration(from = 1, to = 2),
        // 3: trophies and locker unlocks.
        AutoMigration(from = 2, to = 3),
        // 4: exercise.alternateId (time or reps for jump rope).
        AutoMigration(from = 3, to = 4),
        // 5: template_exercise.isWarmup.
        AutoMigration(from = 4, to = 5),
    ],
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun sessionDao(): SessionDao
    abstract fun bodyweightDao(): BodyweightDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun awardsDao(): AwardsDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "gymbrosbro.db"

        fun build(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, NAME)
                // Pre-release safety net: wipe if a migration is missing. Every
                // schema change still gets a migration so test data survives.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
