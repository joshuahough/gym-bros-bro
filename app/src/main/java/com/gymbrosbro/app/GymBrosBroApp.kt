package com.gymbrosbro.app

import android.app.Application
import android.content.Context
import com.gymbrosbro.app.notifications.ReminderScheduler
import kotlinx.coroutines.launch

class GymBrosBroApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.seedLoader.ensureSeeded() }
        // Keep the reminder alarm in step with the settings, including on every launch.
        container.appScope.launch {
            container.settings.reminders.collect { ReminderScheduler.sync(this@GymBrosBroApp, it) }
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as GymBrosBroApp).container
