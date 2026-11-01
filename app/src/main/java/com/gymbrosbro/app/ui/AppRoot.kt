package com.gymbrosbro.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymbrosbro.app.appContainer
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.ui.components.PixelBottomBar
import com.gymbrosbro.app.ui.navigation.CalendarRoute
import com.gymbrosbro.app.ui.navigation.ExerciseDetailRoute
import com.gymbrosbro.app.ui.navigation.ExerciseEditorRoute
import com.gymbrosbro.app.ui.navigation.ExercisePickerRoute
import com.gymbrosbro.app.ui.navigation.HistoryRoute
import com.gymbrosbro.app.ui.navigation.HomeRoute
import com.gymbrosbro.app.ui.navigation.MeRoute
import com.gymbrosbro.app.ui.navigation.PICKED_EXERCISES_KEY
import com.gymbrosbro.app.ui.navigation.SessionRoute
import com.gymbrosbro.app.ui.navigation.SessionSummaryRoute
import com.gymbrosbro.app.ui.navigation.SettingsRoute
import com.gymbrosbro.app.ui.navigation.StatsRoute
import com.gymbrosbro.app.ui.navigation.TemplateEditorRoute
import com.gymbrosbro.app.ui.navigation.TopLevelDestination
import com.gymbrosbro.app.ui.navigation.TrainRoute
import com.gymbrosbro.app.ui.screens.calendar.CalendarScreen
import com.gymbrosbro.app.ui.screens.history.HistoryScreen
import com.gymbrosbro.app.ui.screens.home.HomeScreen
import com.gymbrosbro.app.ui.screens.library.ExerciseDetailScreen
import com.gymbrosbro.app.ui.screens.library.ExerciseEditorScreen
import com.gymbrosbro.app.ui.screens.library.ExercisePickerScreen
import com.gymbrosbro.app.ui.screens.me.MeScreen
import com.gymbrosbro.app.ui.screens.session.SessionScreen
import com.gymbrosbro.app.ui.screens.session.SessionSummaryScreen
import com.gymbrosbro.app.ui.screens.settings.SettingsScreen
import com.gymbrosbro.app.ui.screens.stats.StatsScreen
import com.gymbrosbro.app.ui.screens.train.TrainScreen
import com.gymbrosbro.app.ui.screens.workouts.TemplateEditorScreen
import com.gymbrosbro.app.ui.theme.GbbColors

/** Full-screen flows that hide the bottom bar. */
private val focusedRoutes = listOf(
    SessionRoute::class,
    SessionSummaryRoute::class,
    ExercisePickerRoute::class,
    TemplateEditorRoute::class,
    ExerciseEditorRoute::class,
)

@Composable
fun AppRoot() {
    val container = LocalContext.current.appContainer
    val units by container.settings.unitSystem.collectAsStateWithLifecycle(UnitSystem.IMPERIAL)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination

    // Pushed screens (exercise detail, history...) keep the tab they came from highlighted.
    val currentTab = TopLevelDestination.entries.firstOrNull { tab -> current?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }
    var lastTab by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    LaunchedEffect(currentTab) { if (currentTab != null) lastTab = currentTab }
    val showBottomBar = current != null && focusedRoutes.none { current.hasRoute(it) }

    CompositionLocalProvider(LocalUnitSystem provides units) {
        Scaffold(
            containerColor = GbbColors.Void,
            bottomBar = {
                if (showBottomBar) {
                    PixelBottomBar(
                        selected = { tab -> tab == (currentTab ?: lastTab) },
                        onSelect = { tab -> navController.navigateToTab(tab) },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.padding(padding),
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        onPressStart = { navController.navigateToTab(TopLevelDestination.TRAIN) },
                        onResume = { navController.navigate(SessionRoute(it)) },
                    )
                }
                composable<TrainRoute> {
                    TrainScreen(
                        onOpenSession = { navController.navigate(SessionRoute(it)) },
                        onEditTemplate = { navController.navigate(TemplateEditorRoute(it)) },
                        onNewTemplate = { navController.navigate(TemplateEditorRoute()) },
                        onOpenExercise = { navController.navigate(ExerciseDetailRoute(it)) },
                        onNewCustomExercise = { navController.navigate(ExerciseEditorRoute()) },
                    )
                }
                composable<CalendarRoute> { CalendarScreen() }
                composable<StatsRoute> { StatsScreen() }
                composable<MeRoute> {
                    MeScreen(
                        onOpenHistory = { navController.navigate(HistoryRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }

                composable<ExerciseDetailRoute> {
                    ExerciseDetailScreen(
                        onBack = navController::popBackStack,
                        onEdit = { navController.navigate(ExerciseEditorRoute(exerciseId = it)) },
                        onDuplicate = { navController.navigate(ExerciseEditorRoute(duplicateFromId = it)) },
                    )
                }
                composable<ExerciseEditorRoute> {
                    ExerciseEditorScreen(
                        onBack = navController::popBackStack,
                        onSaved = { id, wasEditing ->
                            if (wasEditing) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(ExerciseDetailRoute(id)) {
                                    popUpTo<ExerciseEditorRoute> { inclusive = true }
                                }
                            }
                        },
                    )
                }
                composable<ExercisePickerRoute> {
                    ExercisePickerScreen(
                        onBack = navController::popBackStack,
                        onPicked = { ids ->
                            navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_EXERCISES_KEY, ArrayList(ids))
                            navController.popBackStack()
                        },
                    )
                }
                composable<TemplateEditorRoute> { entry ->
                    TemplateEditorScreen(
                        onBack = navController::popBackStack,
                        onAddExercises = { navController.navigate(ExercisePickerRoute) },
                        pickedExercises = entry.pickedExercises(),
                        onPickedHandled = { entry.savedStateHandle.remove<ArrayList<String>>(PICKED_EXERCISES_KEY) },
                    )
                }
                composable<SessionRoute> { entry ->
                    SessionScreen(
                        onBack = navController::popBackStack,
                        onAddExercises = { navController.navigate(ExercisePickerRoute) },
                        pickedExercises = entry.pickedExercises(),
                        onPickedHandled = { entry.savedStateHandle.remove<ArrayList<String>>(PICKED_EXERCISES_KEY) },
                        onFinished = { id ->
                            navController.navigate(SessionSummaryRoute(id)) {
                                popUpTo<SessionRoute> { inclusive = true }
                            }
                        },
                    )
                }
                composable<SessionSummaryRoute> {
                    SessionSummaryScreen(onDone = navController::popBackStack)
                }
                composable<HistoryRoute> {
                    HistoryScreen(
                        onBack = navController::popBackStack,
                        onOpenSession = { navController.navigate(SessionRoute(it)) },
                    )
                }
                composable<SettingsRoute> { SettingsScreen(onBack = navController::popBackStack) }
            }
        }
    }
}

/** Exercise ids the picker handed back to this screen, if any are waiting. */
@Composable
private fun NavBackStackEntry.pickedExercises(): List<String>? {
    val picked by savedStateHandle.getStateFlow<ArrayList<String>?>(PICKED_EXERCISES_KEY, null)
        .collectAsStateWithLifecycle()
    return picked
}

/** Standard bottom-nav behavior: one copy of each tab, state saved when switching. */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
