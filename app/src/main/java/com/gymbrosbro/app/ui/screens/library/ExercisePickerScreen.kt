package com.gymbrosbro.app.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelTopBar

/** Multi-select picker used by the workout editor and live sessions. */
@Composable
fun ExercisePickerScreen(onBack: () -> Unit, onPicked: (List<String>) -> Unit) {
    val viewModel = appViewModel { c, _ -> LibraryViewModel(c.exerciseRepository) }
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        PixelTopBar("ADD EXERCISES", onBack = onBack)
        ExerciseBrowser(
            viewModel = viewModel,
            onExerciseClick = { viewModel.toggleSelected(it.id) },
            selectedIds = selected,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        )
        PixelButton(
            text = if (selected.isEmpty()) "PICK EXERCISES" else "ADD ${selected.size}",
            onClick = { onPicked(selected) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
