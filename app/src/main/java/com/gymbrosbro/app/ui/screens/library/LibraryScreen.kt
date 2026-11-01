package com.gymbrosbro.app.ui.screens.library

import androidx.compose.runtime.Composable
import com.gymbrosbro.app.ui.appViewModel

/** Library tab inside Train: browse and open exercises. */
@Composable
fun LibraryScreen(onOpenExercise: (String) -> Unit, onNewCustom: () -> Unit) {
    val viewModel = appViewModel { c, _ -> LibraryViewModel(c.exerciseRepository) }
    ExerciseBrowser(
        viewModel = viewModel,
        onExerciseClick = { onOpenExercise(it.id) },
        onNewCustom = onNewCustom,
    )
}
