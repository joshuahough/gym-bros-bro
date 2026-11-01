package com.gymbrosbro.app

import android.app.Application
import android.content.Context
import kotlinx.coroutines.launch

class GymBrosBroApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.seedLoader.ensureSeeded() }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as GymBrosBroApp).container
