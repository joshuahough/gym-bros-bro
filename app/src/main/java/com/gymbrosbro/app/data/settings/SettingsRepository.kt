package com.gymbrosbro.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gymbrosbro.app.avatar.AvatarConfig
import com.gymbrosbro.app.domain.units.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalTime

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val unitSystem: Flow<UnitSystem> = dataStore.data.map { prefs ->
        prefs[UNIT_SYSTEM]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.IMPERIAL
    }

    /** Keep the screen awake during a workout so it doesn't lock between sets. */
    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[KEEP_SCREEN_ON] ?: true }

    /** First day of the week for the calendar, streaks and weekly stats. */
    val weekStart: Flow<DayOfWeek> = dataStore.data.map { prefs ->
        prefs[WEEK_START]?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() } ?: DayOfWeek.MONDAY
    }

    /** Workouts per week for the weekly streak. */
    val weeklyGoal: Flow<Int> = dataStore.data.map { it[WEEKLY_GOAL] ?: 3 }

    val reminders: Flow<ReminderSettings> = dataStore.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[REMINDERS_ENABLED] ?: false,
            time = prefs[REMINDER_MINUTES]?.let { LocalTime.of(it / 60, it % 60) } ?: ReminderSettings.DEFAULT_TIME,
            graceDay = prefs[GRACE_REMINDER] ?: false,
            offered = prefs[REMINDER_OFFERED] ?: false,
        )
    }

    /** Chiptune sound effects for set complete, high scores and level-ups. */
    val sound: Flow<Boolean> = dataStore.data.map { it[SOUND] ?: true }

    /** Vibration for the same moments as [sound]. */
    val haptics: Flow<Boolean> = dataStore.data.map { it[HAPTICS] ?: true }

    /** What the avatar is wearing. Use AwardsRepository.avatar, which drops anything locked. */
    val avatar: Flow<AvatarConfig> = dataStore.data.map { prefs ->
        prefs[AVATAR]?.let { runCatching { json.decodeFromString<AvatarConfig>(it) }.getOrNull() } ?: AvatarConfig()
    }

    /** Version of the bundled exercise library last merged into the database. */
    val seedVersion: Flow<Int> = dataStore.data.map { it[SEED_VERSION] ?: 0 }

    suspend fun setUnitSystem(system: UnitSystem) {
        dataStore.edit { it[UNIT_SYSTEM] = system.name }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setWeekStart(day: DayOfWeek) {
        dataStore.edit { it[WEEK_START] = day.name }
    }

    suspend fun setWeeklyGoal(goal: Int) {
        dataStore.edit { it[WEEKLY_GOAL] = goal }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit {
            it[REMINDERS_ENABLED] = enabled
            it[REMINDER_OFFERED] = true
        }
    }

    suspend fun setReminderTime(time: LocalTime) {
        dataStore.edit { it[REMINDER_MINUTES] = time.hour * 60 + time.minute }
    }

    suspend fun setGraceReminder(enabled: Boolean) {
        dataStore.edit { it[GRACE_REMINDER] = enabled }
    }

    /** The one-time "want reminders?" offer was shown (SPEC §7.8). */
    suspend fun setReminderOffered() {
        dataStore.edit { it[REMINDER_OFFERED] = true }
    }

    suspend fun setSound(enabled: Boolean) {
        dataStore.edit { it[SOUND] = enabled }
    }

    suspend fun setHaptics(enabled: Boolean) {
        dataStore.edit { it[HAPTICS] = enabled }
    }

    suspend fun setAvatar(config: AvatarConfig) {
        dataStore.edit { it[AVATAR] = json.encodeToString(config) }
    }

    suspend fun setSeedVersion(version: Int) {
        dataStore.edit { it[SEED_VERSION] = version }
    }

    private companion object {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SEED_VERSION = intPreferencesKey("seed_version")
        val WEEK_START = stringPreferencesKey("week_start")
        val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val GRACE_REMINDER = booleanPreferencesKey("grace_reminder")
        val REMINDER_OFFERED = booleanPreferencesKey("reminder_offered")
        val SOUND = booleanPreferencesKey("sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val AVATAR = stringPreferencesKey("avatar")
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** Workout reminders: off by default, one a day at most (SPEC §7.8). */
data class ReminderSettings(
    val enabled: Boolean = false,
    val time: LocalTime = DEFAULT_TIME,
    val graceDay: Boolean = false,
    val offered: Boolean = false,
) {
    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(7, 0)
    }
}
