package com.gymbrosbro.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymbrosbro.app.AppContainer
import com.gymbrosbro.app.appContainer
import com.gymbrosbro.app.domain.units.UnitSystem

/** The user's lb/kg choice, provided once at the root so any screen can format numbers. */
val LocalUnitSystem = compositionLocalOf { UnitSystem.IMPERIAL }

/**
 * Creates a ViewModel scoped to the current screen, wired from the app's
 * [AppContainer]. Route arguments are in the [SavedStateHandle] (`toRoute()`).
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM {
    val container = LocalContext.current.appContainer
    return viewModel(factory = viewModelFactory { initializer { create(container, createSavedStateHandle()) } })
}
