package com.gymbrosbro.app.ui.screens.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbrosbro.app.domain.model.Discipline
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.session.SetFormat
import com.gymbrosbro.app.domain.session.Suggestion
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.ConfirmDialog
import com.gymbrosbro.app.ui.components.MenuAction
import com.gymbrosbro.app.ui.components.OverflowMenu
import com.gymbrosbro.app.ui.components.PastDatePickerDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelDialog
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.PixelTextField
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.sprites.SpriteImage
import com.gymbrosbro.app.sprites.Sprites
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.format
import com.gymbrosbro.app.ui.label
import com.gymbrosbro.app.ui.modeLabel
import com.gymbrosbro.app.ui.screens.library.recommendationText
import com.gymbrosbro.app.ui.localDate
import com.gymbrosbro.app.ui.screens.library.MuscleIcon
import com.gymbrosbro.app.ui.summaryLine
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * The live workout (SPEC §7.3). Also edits a past workout from History, in
 * which case there's no timer and FINISH becomes DONE.
 */
@Composable
fun SessionScreen(
    onBack: () -> Unit,
    onAddExercises: () -> Unit,
    pickedExercises: List<String>?,
    onPickedHandled: () -> Unit,
    onFinished: (sessionId: Long) -> Unit,
) {
    val viewModel = appViewModel { c, s ->
        SessionViewModel(
            c.sessionRepository, c.bodyweightRepository, c.settings, c.awardsRepository, c.feedback, c.appScope, s,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val highScore by viewModel.highScore.collectAsStateWithLifecycle()
    val units = LocalUnitSystem.current

    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Let go of the cursor when you switch apps, so coming back doesn't pop
    // the keyboard up over the workout.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { focusManager.clearFocus() }

    LaunchedEffect(pickedExercises) {
        if (pickedExercises != null) {
            viewModel.addExercises(pickedExercises)
            onPickedHandled()
        }
    }

    val s = state ?: return
    KeepScreenOn(keepScreenOn && !s.isCompleted)

    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        PixelTopBar(s.session.name.uppercase(), onBack = onBack) {
            if (!s.isCompleted) ElapsedTime(s.session.startedAt)
        }
        // Rows above the first exercise card, for scrolling to a card by index.
        val leadingItems = (if (s.isCompleted) 1 else 0) + (if (s.needsBodyweight) 1 else 0)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (s.isCompleted) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            s.session.startedAt.localDate().display(),
                            style = MaterialTheme.typography.titleMedium,
                            color = GbbColors.Chalk,
                            modifier = Modifier.weight(1f),
                        )
                        PixelButton(
                            "CHANGE DATE",
                            onClick = { pickDate = true },
                            small = true,
                            color = GbbColors.Steel,
                            contentColor = GbbColors.Chalk,
                        )
                    }
                }
            }
            if (s.needsBodyweight) {
                item {
                    BodyweightPrompt(units, onSave = viewModel::saveBodyweight, onSkip = viewModel::dismissBodyweightPrompt)
                }
            }
            if (s.blocks.isEmpty()) {
                item {
                    Text(
                        "No exercises yet. Add some to get rolling.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GbbColors.Fog,
                    )
                }
            }
            itemsIndexed(s.blocks, key = { _, b -> b.sessionExerciseId }) { index, block ->
                ExerciseBlockCard(
                    block = block,
                    units = units,
                    showLastTime = !s.isCompleted,
                    isFirst = index == 0,
                    isLast = index == s.blocks.lastIndex,
                    viewModel = viewModel,
                    onAllChecked = {
                        if (index < s.blocks.lastIndex) {
                            scope.launch { listState.easeToItem(leadingItems + index + 1) }
                        }
                    },
                )
            }
            item {
                PixelButton("+ ADD EXERCISE", onClick = onAddExercises, color = GbbColors.Cyan, modifier = Modifier.fillMaxWidth())
            }
            item {
                Text(s.totals.summaryLine(units), style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
            }
            item {
                PixelButton(
                    if (s.isCompleted) "DELETE WORKOUT" else "DISCARD WORKOUT",
                    onClick = { confirmDelete = true },
                    color = GbbColors.Red,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        PixelButton(
            text = if (s.isCompleted) "DONE" else "FINISH WORKOUT",
            onClick = { if (s.isCompleted) onBack() else confirmFinish = true },
            color = if (s.isCompleted) GbbColors.Gold else GbbColors.Green,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }

    if (confirmFinish) {
        when {
            s.totals.sets == 0 -> ConfirmDialog(
                title = "NO SETS LOGGED",
                message = "Nothing's checked off yet. Discard this workout?",
                confirmText = "DISCARD",
                destructive = true,
                dismissText = "KEEP GOING",
                onConfirm = {
                    confirmFinish = false
                    viewModel.delete(onDone = onBack)
                },
                onDismiss = { confirmFinish = false },
            )
            else -> ConfirmDialog(
                title = if (s.uncheckedSets > 0) "FINISH WORKOUT?" else "ALL SETS COMPLETED!",
                message = if (s.uncheckedSets > 0) {
                    "${s.uncheckedSets} unchecked ${if (s.uncheckedSets == 1) "set" else "sets"} will be dropped."
                } else {
                    "Finish workout?"
                },
                confirmText = "FINISH",
                dismissText = "KEEP GOING",
                onConfirm = {
                    confirmFinish = false
                    viewModel.finish(onDone = { onFinished(viewModel.sessionId) })
                },
                onDismiss = { confirmFinish = false },
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = if (s.isCompleted) "DELETE WORKOUT?" else "DISCARD WORKOUT?",
            message = if (s.isCompleted) {
                "This workout and its sets are removed from your history for good."
            } else {
                "Everything logged in this workout will be thrown away."
            },
            confirmText = if (s.isCompleted) "DELETE" else "DISCARD",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDone = onBack)
            },
            onDismiss = { confirmDelete = false },
        )
    }

    highScore?.let { HighScoreDialog(it, units, onDismiss = viewModel::dismissHighScore) }

    if (pickDate) {
        PastDatePickerDialog(
            initial = s.session.startedAt.localDate(),
            onConfirm = { date ->
                pickDate = false
                viewModel.changeDate(date)
            },
            onDismiss = { pickDate = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseBlockCard(
    block: ExerciseBlock,
    units: UnitSystem,
    showLastTime: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    viewModel: SessionViewModel,
    onAllChecked: () -> Unit,
) {
    val type = block.exercise.trackingType
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmSwitch by remember { mutableStateOf(false) }
    val allWarmup = block.sets.isNotEmpty() && block.sets.all { it.isWarmup }

    PixelPanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MuscleIcon(block.exercise.primaryMuscle, size = 28)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(block.exercise.name, style = MaterialTheme.typography.titleMedium, color = GbbColors.Chalk)
                    if (allWarmup) {
                        Text("Warm-up", style = MaterialTheme.typography.bodySmall, color = GbbColors.Orange)
                    }
                    if (block.exercise.isUnilateral) {
                        Text("Reps per side", style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
                    }
                    if (type == TrackingType.BODYWEIGHT_REPS) {
                        Text(
                            "Added weight optional, leave blank for bodyweight",
                            style = MaterialTheme.typography.bodySmall,
                            color = GbbColors.Fog,
                        )
                    }
                    block.alternate?.let { alt ->
                        ModeSwitch(
                            current = type,
                            other = alt.trackingType,
                            onSwitch = {
                                if (block.sets.any { it.isCompleted }) confirmSwitch = true
                                else viewModel.switchMode(block.sessionExerciseId, alt.id)
                            },
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                OverflowMenu(
                    buildList {
                        if (!isFirst) add(MenuAction("Move up") { viewModel.moveExercise(block.sessionExerciseId, -1) })
                        if (!isLast) add(MenuAction("Move down") { viewModel.moveExercise(block.sessionExerciseId, 1) })
                        add(
                            MenuAction(if (allWarmup) "Mark as working sets" else "Mark all as warm-up") {
                                viewModel.setExerciseWarmup(block.sessionExerciseId, !allWarmup)
                            },
                        )
                        add(MenuAction("Remove exercise", destructive = true) {
                            if (block.sets.any { it.isCompleted }) confirmRemove = true
                            else viewModel.removeExercise(block.sessionExerciseId)
                        })
                    },
                )
            }
            if (showLastTime) {
                val last = SetFormat.summary(type, block.lastTime, units)
                if (last.isEmpty()) {
                    val recommended = recommendationText(block.exercise)
                    // Bodyweight moves and cardio have no load to pick.
                    val load = if (block.exercise.discipline in setOf(Discipline.CALISTHENICS, Discipline.CARDIO)) {
                        ""
                    } else {
                        ", ${block.exercise.recommendedLoad.label.lowercase()} load"
                    }
                    Text(
                        "First time — set your baseline." + (recommended?.let { " Try $it$load." } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GbbColors.Fog,
                    )
                } else {
                    Text("Last: $last", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Cyan)
                }
                if (block.suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        block.suggestions.forEach { suggestion ->
                            SuggestionChip(suggestion) { viewModel.applySuggestion(block.sessionExerciseId, suggestion) }
                        }
                    }
                }
            }
            SetHeader(type, units)
            var working = 0
            block.sets.forEach { set ->
                val label = if (set.isWarmup) "W" else (++working).toString()
                key(set.id) {
                    SetRow(
                        set = set,
                        label = label,
                        type = type,
                        units = units,
                        onUpdate = { transform -> viewModel.updateSet(set.id, transform) },
                        onToggleComplete = {
                            // Checking off the last open set moves you on to the next exercise.
                            val finishesBlock = !set.isCompleted && block.sets.none { it.id != set.id && !it.isCompleted }
                            viewModel.toggleComplete(set.id, block.exercise)
                            if (finishesBlock) onAllChecked()
                        },
                        onToggleWarmup = { viewModel.toggleWarmup(set.id) },
                        onDelete = { viewModel.deleteSet(set.id) },
                    )
                }
            }
            PixelButton(
                "+ SET",
                onClick = { viewModel.addSet(block.sessionExerciseId) },
                small = true,
                color = GbbColors.Steel,
                contentColor = GbbColors.Chalk,
            )
        }
    }

    val alternate = block.alternate
    if (confirmSwitch && alternate != null) {
        ConfirmDialog(
            title = "SWITCH TO ${alternate.trackingType.modeLabel}?",
            message = "The sets you've checked off for ${block.exercise.name} will be cleared.",
            confirmText = "SWITCH",
            destructive = true,
            onConfirm = {
                confirmSwitch = false
                viewModel.switchMode(block.sessionExerciseId, alternate.id)
            },
            onDismiss = { confirmSwitch = false },
        )
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "REMOVE EXERCISE?",
            message = "${block.exercise.name} and its checked-off sets will be removed from this workout.",
            confirmText = "REMOVE",
            destructive = true,
            onConfirm = {
                confirmRemove = false
                viewModel.removeExercise(block.sessionExerciseId)
            },
            onDismiss = { confirmRemove = false },
        )
    }
}

/** Two-way switch for exercises that can be logged by time or by reps. */
@Composable
private fun ModeSwitch(current: TrackingType, other: TrackingType, onSwitch: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(current, other).sortedBy { it.ordinal }.forEach { mode ->
            val selected = mode == current
            PixelPanel(
                modifier = Modifier
                    .semantics { this.selected = selected }
                    .clickable(enabled = !selected, role = Role.Tab, onClickLabel = "Log by ${mode.modeLabel.lowercase()}") {
                        onSwitch()
                    },
                fill = if (selected) GbbColors.Dusk else GbbColors.Night,
                border = if (selected) GbbColors.Gold else GbbColors.Steel,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    mode.modeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) GbbColors.Gold else GbbColors.Fog,
                )
            }
        }
    }
}

/** Tap to fill the unchecked sets with a suggested next step. */
@Composable
private fun SuggestionChip(suggestion: Suggestion, onClick: () -> Unit) {
    PixelPanel(
        modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Use suggestion", onClick = onClick),
        fill = GbbColors.Dusk,
        border = GbbColors.Gold,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        val arrow = if (suggestion.kind == Suggestion.Kind.LIGHTER) "▼" else "▲"
        Text("$arrow ${suggestion.label}", style = MaterialTheme.typography.labelMedium, color = GbbColors.Gold)
    }
}

@Composable
private fun HighScoreDialog(highScore: HighScore, units: UnitSystem, onDismiss: () -> Unit) {
    PixelDialog("", onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpriteImage(Sprites.Star.recolor('f' to GbbColors.Gold), contentDescription = null, size = 28.dp)
            Spacer(Modifier.width(10.dp))
            SpriteImage(Sprites.Star.recolor('f' to GbbColors.Pink), contentDescription = null, size = 28.dp)
            Spacer(Modifier.width(10.dp))
            SpriteImage(Sprites.Star.recolor('f' to GbbColors.Cyan), contentDescription = null, size = 28.dp)
        }
        Text(
            "NEW HIGH SCORE!",
            style = MaterialTheme.typography.headlineSmall,
            color = GbbColors.Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            highScore.exercise.name,
            style = MaterialTheme.typography.titleMedium,
            color = GbbColors.Chalk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        highScore.hits.forEach { hit ->
            Text(
                "${hit.type.label}: ${hit.type.format(hit.value, units)} (was ${hit.type.format(hit.previous, units)})",
                style = MaterialTheme.typography.bodyLarge,
                color = GbbColors.Green,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PixelButton("NICE!", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BodyweightPrompt(units: UnitSystem, onSave: (Double) -> Unit, onSkip: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val parsed = SetFormat.parseDecimal(text)?.takeIf { it > 0 }
    PixelPanel(Modifier.fillMaxWidth(), border = GbbColors.Cyan) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("YOUR BODYWEIGHT?", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
            Text(
                "Calisthenics count a share of your bodyweight toward weight lifted. You only need to enter it once.",
                style = MaterialTheme.typography.bodyMedium,
                color = GbbColors.Fog,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = Units.weightUnit(units),
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                )
                PixelButton(
                    "SAVE",
                    onClick = { parsed?.let { onSave(Units.displayToKg(it, units)) } },
                    enabled = parsed != null,
                    small = true,
                )
                PixelButton("SKIP", onClick = onSkip, small = true, color = GbbColors.Steel, contentColor = GbbColors.Chalk)
            }
        }
    }
}

/**
 * A beat after the check mark lands, glide the item to the top. Slower than
 * animateScrollToItem so you can see where you're going.
 */
private suspend fun LazyListState.easeToItem(index: Int) {
    delay(450)
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        animateScrollToItem(index)
    } else {
        animateScrollBy(item.offset.toFloat(), tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }
}

@Composable
private fun ElapsedTime(startedAt: Instant) {
    val now by produceState(Instant.now()) {
        while (true) {
            delay(1_000)
            value = Instant.now()
        }
    }
    val seconds = Duration.between(startedAt, now).seconds.coerceAtLeast(0).toInt()
    Text(SetFormat.duration(seconds), style = MaterialTheme.typography.titleLarge, color = GbbColors.Green)
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
