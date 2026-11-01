package com.gymbrosbro.app.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymbrosbro.app.appContainer
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.sprites.SpriteImage
import com.gymbrosbro.app.sprites.Sprites
import com.gymbrosbro.app.ui.color
import com.gymbrosbro.app.ui.label
import com.gymbrosbro.app.ui.theme.GbbColors

@Composable
fun LibraryScreen() {
    val container = LocalContext.current.appContainer
    val viewModel: LibraryViewModel = viewModel(
        factory = viewModelFactory { initializer { LibraryViewModel(container.exerciseRepository) } },
    )
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    val list = exercises
    if (list == null) {
        Text("LOADING...", style = MaterialTheme.typography.titleLarge, color = GbbColors.Fog)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Text(
                text = "${list.size} EXERCISES",
                style = MaterialTheme.typography.titleLarge,
                color = GbbColors.Cyan,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        items(list, key = { it.id }) { exercise ->
            ExerciseRow(exercise)
            HorizontalDivider(color = GbbColors.Dusk, thickness = 2.dp)
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpriteImage(
            sprite = Sprites.Flex.recolor('f' to exercise.primaryMuscle.color),
            contentDescription = exercise.primaryMuscle.label,
            size = 36.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = GbbColors.Chalk)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${exercise.equipment.label} · ${exercise.primaryMuscle.label}",
                style = MaterialTheme.typography.bodySmall,
                color = GbbColors.Fog,
            )
        }
    }
}
