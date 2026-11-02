package com.gymbrosbro.app.ui.navigation

import com.gymbrosbro.app.sprites.Sprite
import com.gymbrosbro.app.sprites.Sprites
import kotlinx.serialization.Serializable

// Tabs
@Serializable data object HomeRoute
@Serializable data object TrainRoute
@Serializable data object CalendarRoute
@Serializable data object StatsRoute
@Serializable data object MeRoute

// Screens pushed on top of a tab
@Serializable data class ExerciseDetailRoute(val exerciseId: String)

/** New custom exercise, edit one ([exerciseId]), or copy a library one ([duplicateFromId]). */
@Serializable data class ExerciseEditorRoute(val exerciseId: String? = null, val duplicateFromId: String? = null)

/** Multi-select exercise picker; returns ids via [PICKED_EXERCISES_KEY]. */
@Serializable data object ExercisePickerRoute

/** [templateId] 0 = new workout. */
@Serializable data class TemplateEditorRoute(val templateId: Long = 0L)
@Serializable data class SessionRoute(val sessionId: Long)
@Serializable data class SessionSummaryRoute(val sessionId: Long)
@Serializable data object HistoryRoute
@Serializable data object SettingsRoute
@Serializable data object ScheduleRoute
@Serializable data object LockerRoute
@Serializable data object TrophiesRoute

/** SavedStateHandle key the exercise picker writes its result to. */
const val PICKED_EXERCISES_KEY = "picked_exercises"

/** The five bottom-bar tabs (SPEC §7.12). */
enum class TopLevelDestination(val route: Any, val label: String, val icon: Sprite) {
    HOME(HomeRoute, "Home", Sprites.Home),
    TRAIN(TrainRoute, "Train", Sprites.Dumbbell),
    CALENDAR(CalendarRoute, "Calendar", Sprites.Calendar),
    STATS(StatsRoute, "Stats", Sprites.BarChart),
    ME(MeRoute, "Me", Sprites.Player),
}
