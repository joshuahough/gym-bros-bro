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

    /** Shown under the avatar. Blank goes back to the default. */
    val playerName: Flow<String> = dataStore.data.map { it[PLAYER_NAME] ?: DEFAULT_PLAYER_NAME }

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

    suspend fun setPlayerName(name: String) {
        val clean = cleanPlayerName(name)
        dataStore.edit { if (clean.isEmpty()) it.remove(PLAYER_NAME) else it[PLAYER_NAME] = clean }
    }

    suspend fun setSeedVersion(version: Int) {
        dataStore.edit { it[SEED_VERSION] = version }
    }

    companion object {
        const val DEFAULT_PLAYER_NAME = "PLAYER 1"
        const val PLAYER_NAME_MAX = 12

        /** Letters, digits and single spaces, up to [PLAYER_NAME_MAX] characters. */
        fun cleanPlayerName(name: String): String = name
            .filter { it.isLetterOrDigit() || it == ' ' }
            .replace(Regex(" +"), " ")
            .trim()
            .take(PLAYER_NAME_MAX)
            .trim()

        private val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val SEED_VERSION = intPreferencesKey("seed_version")
        private val WEEK_START = stringPreferencesKey("week_start")
        private val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        private val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        private val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        private val GRACE_REMINDER = booleanPreferencesKey("grace_reminder")
        private val REMINDER_OFFERED = booleanPreferencesKey("reminder_offered")
        private val SOUND = booleanPreferencesKey("sound")
        private val HAPTICS = booleanPreferencesKey("haptics")
        private val AVATAR = stringPreferencesKey("avatar")
        private val PLAYER_NAME = stringPreferencesKey("player_name")
        private val json = Json { ignoreUnknownKeys = true }
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
