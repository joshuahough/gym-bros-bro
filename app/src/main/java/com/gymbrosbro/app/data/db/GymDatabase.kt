package com.gymbrosbro.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
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
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun sessionDao(): SessionDao
    abstract fun bodyweightDao(): BodyweightDao

    companion object {
        fun build(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, "gymbrosbro.db")
                // Pre-release only: wipe on schema change. Replace with real
                // migrations before the first store release.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
