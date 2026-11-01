package com.gymbrosbro.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymbrosbro.app.ui.components.PixelBottomBar
import com.gymbrosbro.app.ui.navigation.CalendarRoute
import com.gymbrosbro.app.ui.navigation.HomeRoute
import com.gymbrosbro.app.ui.navigation.MeRoute
import com.gymbrosbro.app.ui.navigation.StatsRoute
import com.gymbrosbro.app.ui.navigation.TopLevelDestination
import com.gymbrosbro.app.ui.navigation.TrainRoute
import com.gymbrosbro.app.ui.screens.calendar.CalendarScreen
import com.gymbrosbro.app.ui.screens.home.HomeScreen
import com.gymbrosbro.app.ui.screens.me.MeScreen
import com.gymbrosbro.app.ui.screens.stats.StatsScreen
import com.gymbrosbro.app.ui.screens.train.TrainScreen
import com.gymbrosbro.app.ui.theme.GbbColors

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination

    Scaffold(
        containerColor = GbbColors.Void,
        bottomBar = {
            PixelBottomBar(
                selected = { tab -> current?.hierarchy?.any { it.hasRoute(tab.route::class) } == true },
                onSelect = { tab -> navController.navigateToTab(tab) },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<HomeRoute> {
                HomeScreen(onPressStart = { navController.navigateToTab(TopLevelDestination.TRAIN) })
            }
            composable<TrainRoute> { TrainScreen() }
            composable<CalendarRoute> { CalendarScreen() }
            composable<StatsRoute> { StatsScreen() }
            composable<MeRoute> { MeScreen() }
        }
    }
}

/** Standard bottom-nav behavior: one copy of each tab, state saved when switching. */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
