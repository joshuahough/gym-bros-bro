package com.gymbrosbro.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gymbrosbro.app.domain.units.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val unitSystem: Flow<UnitSystem> = dataStore.data.map { prefs ->
        prefs[UNIT_SYSTEM]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.IMPERIAL
    }

    /** Keep the screen awake during a workout so it doesn't lock between sets. */
    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[KEEP_SCREEN_ON] ?: true }

    /** Version of the bundled exercise library last merged into the database. */
    val seedVersion: Flow<Int> = dataStore.data.map { it[SEED_VERSION] ?: 0 }

    suspend fun setUnitSystem(system: UnitSystem) {
        dataStore.edit { it[UNIT_SYSTEM] = system.name }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setSeedVersion(version: Int) {
        dataStore.edit { it[SEED_VERSION] = version }
    }

    private companion object {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SEED_VERSION = intPreferencesKey("seed_version")
    }
}
