package com.gymbrosbro.app

import android.content.Context
import com.gymbrosbro.app.data.AwardsRepository
import com.gymbrosbro.app.data.BodyweightRepository
import com.gymbrosbro.app.data.ExerciseRepository
import com.gymbrosbro.app.data.ProgressRepository
import com.gymbrosbro.app.data.ScheduleRepository
import com.gymbrosbro.app.data.SessionRepository
import com.gymbrosbro.app.data.TemplateRepository
import com.gymbrosbro.app.data.db.GymDatabase
import com.gymbrosbro.app.data.seed.SeedLoader
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.data.settings.settingsDataStore
import com.gymbrosbro.app.feedback.Feedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container: one instance per process, owned by [GymBrosBroApp]. */
class AppContainer(context: Context) {
    /** Outlives screens; for writes that must finish even if the user navigates away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: GymDatabase = GymDatabase.build(context)
    val settings = SettingsRepository(context.settingsDataStore)
    val exerciseRepository = ExerciseRepository(database.exerciseDao(), database.sessionDao())
    val templateRepository = TemplateRepository(database)
    val bodyweightRepository = BodyweightRepository(database.bodyweightDao())
    val sessionRepository = SessionRepository(database, templateRepository, bodyweightRepository)
    val scheduleRepository = ScheduleRepository(database)
    val progressRepository = ProgressRepository(sessionRepository, scheduleRepository, settings, appScope)
    val awardsRepository = AwardsRepository(database, progressRepository, settings)
    val feedback = Feedback(context, settings, appScope)
    val seedLoader = SeedLoader(context.assets, database, settings)
}
