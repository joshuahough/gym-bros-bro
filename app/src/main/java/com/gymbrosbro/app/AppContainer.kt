package com.gymbrosbro.app

import android.content.Context
import com.gymbrosbro.app.data.ExerciseRepository
import com.gymbrosbro.app.data.db.GymDatabase
import com.gymbrosbro.app.data.seed.SeedLoader
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.data.settings.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container: one instance per process, owned by [GymBrosBroApp]. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: GymDatabase = GymDatabase.build(context)
    val settings = SettingsRepository(context.settingsDataStore)
    val exerciseRepository = ExerciseRepository(database.exerciseDao())
    val seedLoader = SeedLoader(context.assets, database, settings)
}
