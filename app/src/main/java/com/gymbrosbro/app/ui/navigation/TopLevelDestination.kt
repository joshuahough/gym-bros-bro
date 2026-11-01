package com.gymbrosbro.app.ui.navigation

import com.gymbrosbro.app.sprites.Sprite
import com.gymbrosbro.app.sprites.Sprites
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object TrainRoute
@Serializable data object CalendarRoute
@Serializable data object StatsRoute
@Serializable data object MeRoute

/** The five bottom-bar tabs (SPEC §7.12). */
enum class TopLevelDestination(val route: Any, val label: String, val icon: Sprite) {
    HOME(HomeRoute, "Home", Sprites.Home),
    TRAIN(TrainRoute, "Train", Sprites.Dumbbell),
    CALENDAR(CalendarRoute, "Calendar", Sprites.Calendar),
    STATS(StatsRoute, "Stats", Sprites.BarChart),
    ME(MeRoute, "Me", Sprites.Player),
}
