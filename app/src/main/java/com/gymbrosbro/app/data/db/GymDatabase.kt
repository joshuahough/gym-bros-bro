package com.gymbrosbro.app.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleExceptionEntity
import com.gymbrosbro.app.data.db.entity.SchedulePauseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleRuleEntity
import com.gymbrosbro.app.data.db.entity.SessionExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.TemplateExerciseEntity
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
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // 2: schedule tables.
        AutoMigration(from = 1, to = 2),
    ],
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun sessionDao(): SessionDao
    abstract fun bodyweightDao(): BodyweightDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        fun build(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, "gymbrosbro.db")
                // Pre-release safety net: wipe if a migration is missing. Every
                // schema change still gets a migration so test data survives.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
