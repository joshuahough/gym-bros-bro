package com.gymbrosbro.app.ui.screens.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gymbrosbro.app.data.ExerciseRepository
import com.gymbrosbro.app.data.TemplateExerciseDraft
import com.gymbrosbro.app.data.TemplateRepository
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.session.SetFormat
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.ConfirmDialog
import com.gymbrosbro.app.ui.components.MenuAction
import com.gymbrosbro.app.ui.components.OverflowMenu
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.PixelStepper
import com.gymbrosbro.app.ui.components.PixelTextField
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.ui.navigation.TemplateEditorRoute
import com.gymbrosbro.app.ui.screens.library.MuscleIcon
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One exercise row in the editor; [key] keeps list identity stable while reordering. */
data class TemplateItem(
    val key: Long,
    val exercise: ExerciseEntity,
    val sets: Int,
    val repsMin: String,
    val repsMax: String,
    val duration: String,
) {
    val usesReps get() = exercise.trackingType == TrackingType.WEIGHT_REPS || exercise.trackingType == TrackingType.BODYWEIGHT_REPS
    val usesDuration get() = exercise.trackingType == TrackingType.DURATION || exercise.trackingType == TrackingType.DISTANCE_DURATION
}

data class TemplateEditorState(val name: String = "", val items: List<TemplateItem> = emptyList()) {
    val canSave get() = name.isNotBlank() && items.isNotEmpty()
}

class TemplateEditorViewModel(
    private val templates: TemplateRepository,
    private val exercises: ExerciseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val templateId = savedState.toRoute<TemplateEditorRoute>().templateId.takeIf { it != 0L }
    val isEditing = templateId != null
    private var nextKey = 0L

    private val _state = MutableStateFlow(TemplateEditorState())
    val state: StateFlow<TemplateEditorState> = _state.asStateFlow()

    init {
        if (templateId != null) {
            viewModelScope.launch {
                val t = templates.getTemplate(templateId) ?: return@launch
                _state.value = TemplateEditorState(
                    name = t.template.name,
                    items = t.sortedExercises.map { te ->
                        val x = te.templateExercise
                        TemplateItem(
                            key = nextKey++,
                            exercise = te.exercise,
                            sets = x.targetSets,
                            repsMin = x.targetRepsMin?.toString().orEmpty(),
                            repsMax = x.targetRepsMax?.toString().orEmpty(),
                            duration = x.targetDurationSec?.let(SetFormat::duration).orEmpty(),
                        )
                    },
                )
            }
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name) }

    fun addExercises(ids: List<String>) {
        viewModelScope.launch {
            val added = ids.mapNotNull { exercises.getExercise(it) }.map { e ->
                TemplateItem(
                    key = nextKey++,
                    exercise = e,
                    sets = e.recommendedSets ?: 3,
                    repsMin = e.recommendedRepsMin?.toString().orEmpty(),
                    repsMax = e.recommendedRepsMax?.toString().orEmpty(),
                    duration = e.recommendedDurationSec?.let(SetFormat::duration).orEmpty(),
                )
            }
            _state.update { it.copy(items = it.items + added) }
        }
    }

    fun updateItem(key: Long, transform: TemplateItem.() -> TemplateItem) =
        _state.update { s -> s.copy(items = s.items.map { if (it.key == key) it.transform() else it }) }

    fun remove(key: Long) = _state.update { s -> s.copy(items = s.items.filterNot { it.key == key }) }

    fun move(key: Long, delta: Int) = _state.update { s ->
        val list = s.items.toMutableList()
        val from = list.indexOfFirst { it.key == key }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@update s
        list.add(to, list.removeAt(from))
        s.copy(items = list)
    }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            templates.save(
                templateId,
                s.name.trim(),
                s.items.map { item ->
                    TemplateExerciseDraft(
                        exerciseId = item.exercise.id,
                        targetSets = item.sets,
                        targetRepsMin = if (item.usesReps) SetFormat.parseInt(item.repsMin) else null,
                        targetRepsMax = if (item.usesReps) SetFormat.parseInt(item.repsMax) else null,
                        targetDurationSec = if (item.usesDuration) SetFormat.parseDuration(item.duration) else null,
                        targetDistanceM = item.exercise.recommendedDistanceM,
                    )
                },
            )
            onSaved()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = templateId ?: return
        viewModelScope.launch {
            templates.archive(id)
            onDone()
        }
    }
}

@Composable
fun TemplateEditorScreen(
    onBack: () -> Unit,
    onAddExercises: () -> Unit,
    pickedExercises: List<String>?,
    onPickedHandled: () -> Unit,
) {
    val viewModel = appViewModel { c, s -> TemplateEditorViewModel(c.templateRepository, c.exerciseRepository, s) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(pickedExercises) {
        if (pickedExercises != null) {
            viewModel.addExercises(pickedExercises)
            onPickedHandled()
        }
    }

    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        PixelTopBar(if (viewModel.isEditing) "EDIT WORKOUT" else "NEW WORKOUT", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("NAME", style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
                Spacer(Modifier.padding(top = 6.dp))
                PixelTextField(state.name, viewModel::setName, Modifier.fillMaxWidth(), "e.g. Monday Push")
            }
            itemsIndexed(state.items, key = { _, item -> item.key }) { index, item ->
                TemplateItemCard(
                    item = item,
                    isFirst = index == 0,
                    isLast = index == state.items.lastIndex,
                    onUpdate = { transform -> viewModel.updateItem(item.key, transform) },
                    onMove = { delta -> viewModel.move(item.key, delta) },
                    onRemove = { viewModel.remove(item.key) },
                )
            }
            item {
                PixelButton(
                    "+ ADD EXERCISES",
                    onClick = onAddExercises,
                    color = GbbColors.Cyan,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (viewModel.isEditing) {
                item {
                    PixelButton(
                        "DELETE WORKOUT",
                        onClick = { confirmDelete = true },
                        color = GbbColors.Steel,
                        contentColor = GbbColors.Chalk,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        PixelButton(
            text = "SAVE WORKOUT",
            onClick = { viewModel.save(onSaved = onBack) },
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "DELETE WORKOUT?",
            message = "The workout is removed from your list. Sessions you've already logged with it stay in your history.",
            confirmText = "DELETE",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDone = onBack)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun TemplateItemCard(
    item: TemplateItem,
    isFirst: Boolean,
    isLast: Boolean,
    onUpdate: (TemplateItem.() -> TemplateItem) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    PixelPanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MuscleIcon(item.exercise.primaryMuscle, size = 28)
                Spacer(Modifier.width(10.dp))
                Text(item.exercise.name, style = MaterialTheme.typography.titleMedium, color = GbbColors.Chalk, modifier = Modifier.weight(1f))
                OverflowMenu(
                    buildList {
                        if (!isFirst) add(MenuAction("Move up") { onMove(-1) })
                        if (!isLast) add(MenuAction("Move down") { onMove(1) })
                        add(MenuAction("Remove", destructive = true, onClick = onRemove))
                    },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SETS", style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog, modifier = Modifier.width(56.dp))
                PixelStepper(item.sets, { v -> onUpdate { copy(sets = v) } })
            }
            if (item.usesReps) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("REPS", style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog, modifier = Modifier.width(56.dp))
                    SmallNumberField(item.repsMin) { v -> onUpdate { copy(repsMin = v) } }
                    Text("  to  ", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                    SmallNumberField(item.repsMax) { v -> onUpdate { copy(repsMax = v) } }
                }
            }
            if (item.usesDuration) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TIME", style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog, modifier = Modifier.width(56.dp))
                    PixelTextField(
                        item.duration,
                        { v -> onUpdate { copy(duration = v) } },
                        Modifier.width(100.dp),
                        "m:ss",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallNumberField(value: String, onChange: (String) -> Unit) {
    PixelTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter(Char::isDigit).take(3)) },
        modifier = Modifier.width(64.dp),
        keyboardType = KeyboardType.Number,
        textAlign = TextAlign.Center,
    )
}
