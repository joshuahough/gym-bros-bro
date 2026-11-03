package com.gymbrosbro.app.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gymbrosbro.app.data.ExerciseRepository
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.domain.model.Discipline
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.ExerciseSource
import com.gymbrosbro.app.domain.model.LoadClass
import com.gymbrosbro.app.domain.model.MuscleGroup
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.session.SetFormat
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.color
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelChip
import com.gymbrosbro.app.ui.components.PixelStepper
import com.gymbrosbro.app.ui.components.PixelDurationField
import com.gymbrosbro.app.ui.components.PixelTextField
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.ui.label
import com.gymbrosbro.app.ui.navigation.ExerciseEditorRoute
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editable form state; text inputs stay strings until save. */
data class ExerciseDraft(
    val name: String = "",
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    val equipment: Equipment = Equipment.DUMBBELL,
    val primaryMuscle: MuscleGroup = MuscleGroup.CHEST,
    val discipline: Discipline = Discipline.WEIGHTLIFTING,
    val load: LoadClass = LoadClass.MEDIUM,
    val sets: Int = 3,
    val repsMin: String = "8",
    val repsMax: String = "12",
    val duration: String = "",
    val isUnilateral: Boolean = false,
    val bodyweightPercent: String = "",
    val instructions: String = "",
    val notes: String = "",
) {
    val usesReps get() = trackingType == TrackingType.WEIGHT_REPS || trackingType == TrackingType.BODYWEIGHT_REPS ||
        trackingType == TrackingType.REPS
    val usesDuration get() = trackingType == TrackingType.DURATION || trackingType == TrackingType.DISTANCE_DURATION
    val isValid get() = name.isNotBlank()
}

class ExerciseEditorViewModel(
    private val repository: ExerciseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val route = savedState.toRoute<ExerciseEditorRoute>()
    val isEditing = route.exerciseId != null

    /** The exercise being edited, or the library one being duplicated. */
    private var original: ExerciseEntity? = null

    private val _draft = MutableStateFlow(ExerciseDraft())
    val draft: StateFlow<ExerciseDraft> = _draft.asStateFlow()

    init {
        val sourceId = route.exerciseId ?: route.duplicateFromId
        if (sourceId != null) {
            viewModelScope.launch {
                val e = repository.getExercise(sourceId) ?: return@launch
                original = e
                _draft.value = e.toDraft(nameSuffix = if (isEditing) "" else " (Custom)")
            }
        }
    }

    fun update(transform: ExerciseDraft.() -> ExerciseDraft) = _draft.update(transform)

    fun save(onSaved: (String) -> Unit) {
        val d = _draft.value
        if (!d.isValid) return
        val base = original
        val entity = ExerciseEntity(
            id = if (isEditing && base != null) base.id else ExerciseRepository.newCustomId(),
            name = d.name.trim(),
            aliases = base?.aliases.orEmpty(),
            discipline = d.discipline,
            trackingType = d.trackingType,
            equipment = d.equipment,
            primaryMuscle = d.primaryMuscle,
            secondaryMuscles = base?.secondaryMuscles.orEmpty().filter { it != d.primaryMuscle },
            mechanic = base?.mechanic,
            recommendedLoad = d.load,
            recommendedSets = d.sets,
            recommendedRepsMin = if (d.usesReps) SetFormat.parseInt(d.repsMin) else null,
            recommendedRepsMax = if (d.usesReps) SetFormat.parseInt(d.repsMax) else null,
            recommendedDurationSec = if (d.usesDuration) SetFormat.parseDuration(d.duration) else null,
            recommendedDistanceM = base?.recommendedDistanceM,
            isUnilateral = d.isUnilateral,
            bodyweightFactor = if (d.trackingType == TrackingType.BODYWEIGHT_REPS) {
                SetFormat.parseDecimal(d.bodyweightPercent)?.takeIf { it in 0.0..200.0 }?.div(100)
            } else {
                null
            },
            weightIncrementKg = base?.weightIncrementKg,
            instructions = d.instructions.lines().map { it.trim() }.filter { it.isNotEmpty() },
            source = ExerciseSource.CUSTOM,
            isFavorite = if (isEditing) base?.isFavorite ?: false else false,
            userNotes = d.notes.trim().ifEmpty { null },
        )
        viewModelScope.launch {
            repository.saveCustom(entity)
            onSaved(entity.id)
        }
    }

    private fun ExerciseEntity.toDraft(nameSuffix: String) = ExerciseDraft(
        name = name + nameSuffix,
        trackingType = trackingType,
        equipment = equipment,
        primaryMuscle = primaryMuscle,
        discipline = discipline,
        load = recommendedLoad,
        sets = recommendedSets ?: 3,
        repsMin = recommendedRepsMin?.toString().orEmpty(),
        repsMax = recommendedRepsMax?.toString().orEmpty(),
        duration = recommendedDurationSec?.let(SetFormat::duration).orEmpty(),
        isUnilateral = isUnilateral,
        bodyweightPercent = bodyweightFactor?.let { (it * 100).toInt().toString() }.orEmpty(),
        instructions = instructions.joinToString("\n"),
        notes = userNotes.orEmpty(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditorScreen(onBack: () -> Unit, onSaved: (id: String, wasEditing: Boolean) -> Unit) {
    val viewModel = appViewModel { c, s -> ExerciseEditorViewModel(c.exerciseRepository, s) }
    val d by viewModel.draft.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        PixelTopBar(if (viewModel.isEditing) "EDIT EXERCISE" else "NEW EXERCISE", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("NAME") {
                PixelTextField(d.name, { v -> viewModel.update { copy(name = v) } }, Modifier.fillMaxWidth(), "e.g. Landmine Press")
            }
            Section("TRACK") {
                ChipGroup(TrackingType.entries, d.trackingType, { it.label }) { viewModel.update { copy(trackingType = it) } }
            }
            Section("EQUIPMENT") {
                ChipGroup(Equipment.entries, d.equipment, { it.label }) { viewModel.update { copy(equipment = it) } }
            }
            Section("PRIMARY MUSCLE") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuscleGroup.entries.forEach { m ->
                        PixelChip(m.label, d.primaryMuscle == m, { viewModel.update { copy(primaryMuscle = m) } }, accent = m.color)
                    }
                }
            }
            Section("TYPE") {
                ChipGroup(Discipline.entries, d.discipline, { it.label }) { viewModel.update { copy(discipline = it) } }
            }
            Section("RECOMMENDED LOAD") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LoadClass.entries.forEach { l ->
                        PixelChip(l.label, d.load == l, { viewModel.update { copy(load = l) } }, accent = l.color)
                    }
                }
            }
            Section("RECOMMENDED SETS") {
                PixelStepper(d.sets, { v -> viewModel.update { copy(sets = v) } })
            }
            if (d.usesReps) {
                Section("RECOMMENDED REPS") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberBox(d.repsMin) { v -> viewModel.update { copy(repsMin = v) } }
                        Text("  to  ", style = MaterialTheme.typography.bodyLarge, color = GbbColors.Fog)
                        NumberBox(d.repsMax) { v -> viewModel.update { copy(repsMax = v) } }
                    }
                }
            }
            if (d.usesDuration) {
                Section("RECOMMENDED TIME (MIN : SEC)") {
                    PixelDurationField(
                        seconds = SetFormat.parseDuration(d.duration),
                        onChange = { v -> viewModel.update { copy(duration = v?.let(SetFormat::duration).orEmpty()) } },
                        modifier = Modifier.width(140.dp),
                        imeAction = ImeAction.Next,
                    )
                }
            }
            if (d.trackingType == TrackingType.BODYWEIGHT_REPS) {
                Section("% OF BODYWEIGHT PER REP") {
                    Text(
                        "Counts toward weight lifted. Push-up ≈ 65, pull-up = 100. Leave blank to count reps only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GbbColors.Fog,
                    )
                    NumberBox(d.bodyweightPercent) { v -> viewModel.update { copy(bodyweightPercent = v) } }
                }
            }
            PixelChip(
                "Log reps per side (one arm / one leg)",
                d.isUnilateral,
                { viewModel.update { copy(isUnilateral = !isUnilateral) } },
            )
            Section("HOW TO (ONE STEP PER LINE)") {
                PixelTextField(
                    d.instructions,
                    { v -> viewModel.update { copy(instructions = v) } },
                    Modifier.fillMaxWidth().heightIn(min = 96.dp),
                    singleLine = false,
                )
            }
            Section("NOTES") {
                PixelTextField(
                    d.notes,
                    { v -> viewModel.update { copy(notes = v) } },
                    Modifier.fillMaxWidth().heightIn(min = 72.dp),
                    "Seat height 4, grip, cues...",
                    singleLine = false,
                )
            }
        }
        PixelButton(
            text = "SAVE",
            onClick = { viewModel.save { id -> onSaved(id, viewModel.isEditing) } },
            enabled = d.isValid,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option -> PixelChip(label(option), option == selected, { onSelect(option) }) }
    }
}

@Composable
private fun NumberBox(value: String, onChange: (String) -> Unit) {
    PixelTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter(Char::isDigit).take(3)) },
        modifier = Modifier.width(72.dp),
        keyboardType = KeyboardType.Number,
        textAlign = TextAlign.Center,
    )
}
