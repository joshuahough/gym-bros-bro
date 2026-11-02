package com.gymbrosbro.app.ui.screens.workouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbrosbro.app.data.db.TemplateWithExercises
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.ConfirmDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.theme.GbbColors

@Composable
fun WorkoutsTab(
    onOpenSession: (Long) -> Unit,
    onEditTemplate: (Long) -> Unit,
    onNewTemplate: () -> Unit,
) {
    val viewModel = appViewModel { c, _ -> WorkoutsViewModel(c.templateRepository, c.sessionRepository) }
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val inProgress by viewModel.inProgress.collectAsStateWithLifecycle()
    val starter = rememberStartWorkoutController(viewModel, onOpenSession)
    var confirmDiscard by remember { mutableStateOf<WorkoutSessionEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        inProgress?.let { session ->
            item {
                PixelPanel(Modifier.fillMaxWidth(), border = GbbColors.Green) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("IN PROGRESS", style = MaterialTheme.typography.titleLarge, color = GbbColors.Green)
                            Text(session.name, style = MaterialTheme.typography.titleMedium, color = GbbColors.Chalk)
                        }
                        PixelButton(
                            "DELETE",
                            onClick = { confirmDiscard = session },
                            color = GbbColors.Red,
                            small = true,
                        )
                        Spacer(Modifier.width(8.dp))
                        PixelButton("RESUME", onClick = { onOpenSession(session.id) }, color = GbbColors.Green, small = true)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton("+ NEW WORKOUT", onClick = onNewTemplate, modifier = Modifier.weight(1f), small = true)
                PixelButton(
                    "QUICK START",
                    onClick = { starter.request(null) },
                    color = GbbColors.Cyan,
                    modifier = Modifier.weight(1f),
                    small = true,
                )
            }
        }
        val list = templates
        when {
            list == null -> item { Text("LOADING...", style = MaterialTheme.typography.titleLarge, color = GbbColors.Fog) }
            list.isEmpty() -> item {
                Text(
                    "No workouts yet. Build one from the library — like your Monday push day — and start it with one tap.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GbbColors.Fog,
                )
            }
            else -> items(list, key = { it.template.id }) { template ->
                TemplateCard(
                    template = template,
                    onStart = { starter.request(template.template.id) },
                    onEdit = { onEditTemplate(template.template.id) },
                )
            }
        }
    }

    StartWorkoutDialog(starter)

    confirmDiscard?.let { session ->
        ConfirmDialog(
            title = "DELETE WORKOUT?",
            message = "${session.name} is still in progress. Everything logged in it will be thrown away.",
            confirmText = "DELETE",
            destructive = true,
            onConfirm = {
                confirmDiscard = null
                viewModel.discard(session.id)
            },
            onDismiss = { confirmDiscard = null },
        )
    }
}

@Composable
private fun TemplateCard(template: TemplateWithExercises, onStart: () -> Unit, onEdit: () -> Unit) {
    val exercises = template.sortedExercises
    PixelPanel(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Edit workout", onClick = onEdit)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        template.template.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = GbbColors.Chalk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sets = exercises.sumOf { it.templateExercise.targetSets }
                    Text(
                        "${exercises.size} exercises · $sets sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = GbbColors.Fog,
                    )
                }
                Spacer(Modifier.width(8.dp))
                PixelButton("START", onClick = onStart, small = true)
            }
            if (exercises.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    exercises.joinToString(" · ") { it.exercise.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = GbbColors.Fog,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
