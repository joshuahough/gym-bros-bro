package com.gymbrosbro.app.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.ExerciseSource
import com.gymbrosbro.app.domain.model.MuscleGroup
import com.gymbrosbro.app.sprites.SpriteImage
import com.gymbrosbro.app.sprites.Sprites
import com.gymbrosbro.app.ui.color
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelChip
import com.gymbrosbro.app.ui.components.PixelTextField
import com.gymbrosbro.app.ui.components.SetCheckBox
import com.gymbrosbro.app.ui.label
import com.gymbrosbro.app.ui.theme.GbbColors

/**
 * Searchable, filterable exercise list. Browse mode ([selectedIds] null) opens
 * an exercise on tap; picker mode toggles a checkbox instead.
 */
@Composable
fun ExerciseBrowser(
    viewModel: LibraryViewModel,
    onExerciseClick: (ExerciseEntity) -> Unit,
    modifier: Modifier = Modifier,
    selectedIds: List<String>? = null,
    onNewCustom: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state
    if (current == null) {
        Text("LOADING...", style = MaterialTheme.typography.titleLarge, color = GbbColors.Fog, modifier = modifier)
        return
    }
    val query = current.query

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            PixelTextField(
                value = query.text,
                onValueChange = viewModel::setText,
                placeholder = "Search ${current.totalCount} exercises",
                imeAction = ImeAction.Search,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { PixelChip("★ Favorites", query.favoritesOnly, viewModel::toggleFavorites) }
                item { PixelChip("All muscles", query.muscle == null, { viewModel.setMuscle(null) }) }
                items(MuscleGroup.entries) { m ->
                    PixelChip(m.label, query.muscle == m, { viewModel.setMuscle(if (query.muscle == m) null else m) }, accent = m.color)
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { PixelChip("All equipment", query.equipment == null, { viewModel.setEquipment(null) }) }
                items(Equipment.entries) { e ->
                    PixelChip(e.label, query.equipment == e, { viewModel.setEquipment(if (query.equipment == e) null else e) })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${current.results.size} EXERCISES",
                    style = MaterialTheme.typography.titleLarge,
                    color = GbbColors.Cyan,
                    modifier = Modifier.weight(1f),
                )
                if (onNewCustom != null) PixelButton("+ CUSTOM", onClick = onNewCustom, small = true)
            }
        }
        if (current.results.isEmpty()) {
            item {
                Text(
                    "Nothing matches. Try fewer filters, or add it as a custom exercise.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GbbColors.Fog,
                )
            }
        }
        items(current.results, key = { it.id }) { exercise ->
            ExerciseRow(
                exercise = exercise,
                selected = selectedIds?.let { exercise.id in it },
                onClick = { onExerciseClick(exercise) },
            )
            HorizontalDivider(color = GbbColors.Dusk, thickness = 2.dp)
        }
    }
}

@Composable
fun ExerciseRow(exercise: ExerciseEntity, selected: Boolean?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = if (selected != null) Role.Checkbox else Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MuscleIcon(exercise.primaryMuscle, size = 36)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = GbbColors.Chalk)
            Spacer(Modifier.height(2.dp))
            val tags = buildList {
                add(exercise.equipment.label)
                add(exercise.primaryMuscle.label)
                if (exercise.source == ExerciseSource.CUSTOM) add("Custom")
                if (exercise.isFavorite) add("★")
            }
            Text(tags.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
        }
        if (selected != null) {
            Spacer(Modifier.width(8.dp))
            SetCheckBox(checked = selected, enabled = true, onToggle = onClick)
        }
    }
}

@Composable
fun MuscleIcon(muscle: MuscleGroup, size: Int, modifier: Modifier = Modifier) {
    SpriteImage(
        sprite = Sprites.Flex.recolor('f' to muscle.color),
        contentDescription = muscle.label,
        size = size.dp,
        modifier = modifier,
    )
}
