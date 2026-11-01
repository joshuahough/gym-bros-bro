package com.gymbrosbro.app.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gymbrosbro.app.data.ExerciseRepository
import com.gymbrosbro.app.data.db.ExercisePerformance
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.domain.model.ExerciseSource
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.session.SetFormat
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.color
import com.gymbrosbro.app.ui.components.ConfirmDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelChip
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.label
import com.gymbrosbro.app.ui.localDate
import com.gymbrosbro.app.ui.navigation.ExerciseDetailRoute
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseDetailViewModel(
    private val repository: ExerciseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val id = savedState.toRoute<ExerciseDetailRoute>().exerciseId

    val exercise: StateFlow<ExerciseEntity?> = repository.observeExercise(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val history: StateFlow<List<ExercisePerformance>> = repository.observeHistory(id, limit = 5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite() {
        val current = exercise.value ?: return
        viewModelScope.launch { repository.setFavorite(current.id, !current.isFavorite) }
    }

    fun archive(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.archiveCustom(id)
            onDone()
        }
    }
}

@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDuplicate: (String) -> Unit,
) {
    val viewModel = appViewModel { c, s -> ExerciseDetailViewModel(c.exerciseRepository, s) }
    val exercise by viewModel.exercise.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    var confirmArchive by remember { mutableStateOf(false) }
    val units = LocalUnitSystem.current

    val e = exercise
    Column(Modifier.fillMaxSize()) {
        PixelTopBar(e?.name?.uppercase() ?: "", onBack = onBack)
        if (e == null) return@Column

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MuscleIcon(e.primaryMuscle, size = 56)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.titleMedium, color = GbbColors.Chalk)
                    Text(
                        listOf(e.discipline.label, e.equipment.label, e.trackingType.label).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = GbbColors.Fog,
                    )
                }
                PixelChip("★", selected = e.isFavorite, onClick = viewModel::toggleFavorite)
            }

            PixelPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("RECOMMENDED", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    Text(
                        text = "${e.recommendedLoad.label} load",
                        style = MaterialTheme.typography.titleMedium,
                        color = e.recommendedLoad.color,
                    )
                    recommendationText(e)?.let {
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)
                    }
                    if (e.isUnilateral) Text("Log reps per side.", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                    e.bodyweightFactor?.let {
                        Text(
                            "Counts ${(it * 100).toInt()}% of your bodyweight per rep.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GbbColors.Fog,
                        )
                    }
                }
            }

            PixelPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("MUSCLES", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    Text("Primary: ${e.primaryMuscle.label}", style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)
                    if (e.secondaryMuscles.isNotEmpty()) {
                        Text(
                            "Also: ${e.secondaryMuscles.joinToString { it.label }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GbbColors.Fog,
                        )
                    }
                }
            }

            PixelPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("YOUR HISTORY", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    if (history.isEmpty()) {
                        Text("Not logged yet. Your sets will show up here.", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                    }
                    history.forEach { p ->
                        Text(p.session.startedAt.localDate().display(), style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
                        Text(
                            SetFormat.summary(e.trackingType, p.sortedSets, units).ifEmpty { "—" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = GbbColors.Chalk,
                        )
                    }
                }
            }

            if (e.instructions.isNotEmpty()) {
                PixelPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("HOW TO", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                        e.instructions.forEachIndexed { i, step ->
                            Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Chalk)
                        }
                    }
                }
            }

            e.userNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                PixelPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("NOTES", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                        Text(notes, style = MaterialTheme.typography.bodyMedium, color = GbbColors.Chalk)
                    }
                }
            }

            if (e.source == ExerciseSource.CUSTOM) {
                PixelButton("EDIT", onClick = { onEdit(e.id) }, modifier = Modifier.fillMaxWidth())
                PixelButton(
                    "ARCHIVE",
                    onClick = { confirmArchive = true },
                    color = GbbColors.Steel,
                    contentColor = GbbColors.Chalk,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PixelButton(
                    "DUPLICATE & EDIT",
                    onClick = { onDuplicate(e.id) },
                    color = GbbColors.Steel,
                    contentColor = GbbColors.Chalk,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmArchive) {
        ConfirmDialog(
            title = "ARCHIVE EXERCISE?",
            message = "It disappears from the library. Workouts you've already logged keep it.",
            confirmText = "ARCHIVE",
            destructive = true,
            onConfirm = {
                confirmArchive = false
                viewModel.archive(onDone = onBack)
            },
            onDismiss = { confirmArchive = false },
        )
    }
}

/** "3 sets × 5–8 reps", "3 × 45 sec", etc. */
fun recommendationText(e: ExerciseEntity): String? {
    val sets = e.recommendedSets ?: return null
    val setsLabel = "$sets ${if (sets == 1) "set" else "sets"}"
    return when (e.trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.BODYWEIGHT_REPS -> {
            val min = e.recommendedRepsMin
            val max = e.recommendedRepsMax
            when {
                min != null && max != null && min != max -> "$setsLabel × $min–$max reps"
                min != null -> "$setsLabel × $min reps"
                else -> setsLabel
            }
        }
        TrackingType.DURATION, TrackingType.DISTANCE_DURATION ->
            e.recommendedDurationSec?.let { "$setsLabel × ${SetFormat.duration(it)}" } ?: setsLabel
        TrackingType.WEIGHT_DISTANCE -> setsLabel
    }
}
