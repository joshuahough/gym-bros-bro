package com.gymbrosbro.app.ui.screens.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.session.SetFormat
import com.gymbrosbro.app.domain.session.WorkoutMath
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import com.gymbrosbro.app.ui.components.PixelTextField
import com.gymbrosbro.app.ui.components.SetCheckBox
import com.gymbrosbro.app.ui.theme.GbbColors

private val LabelWidth = 40.dp
private val CheckWidth = 44.dp

/** Column titles that line up with [SetRow]'s inputs. */
@Composable
fun SetHeader(type: TrackingType, units: UnitSystem) {
    val weight = Units.weightUnit(units).uppercase()
    val labels = when (type) {
        TrackingType.WEIGHT_REPS -> listOf(weight, "REPS")
        TrackingType.BODYWEIGHT_REPS -> listOf("+$weight", "REPS")
        TrackingType.DURATION -> listOf("TIME")
        TrackingType.DISTANCE_DURATION -> listOf(Units.distanceUnit(units).uppercase(), "TIME")
        TrackingType.WEIGHT_DISTANCE -> listOf(weight, Units.shortDistanceUnit(units).uppercase())
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HeaderText("SET", Modifier.width(LabelWidth))
        labels.forEach { HeaderText(it, Modifier.weight(1f)) }
        HeaderText("✓", Modifier.width(CheckWidth))
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = GbbColors.Fog, textAlign = TextAlign.Center, modifier = modifier)
}

/**
 * One set: label (tap for warm-up / delete), inputs for the tracking type,
 * and the check-off box. Edits save as you type.
 */
@Composable
fun SetRow(
    set: SetEntryEntity,
    label: String,
    type: TrackingType,
    units: UnitSystem,
    onUpdate: ((SetEntryEntity) -> SetEntryEntity) -> Unit,
    onToggleComplete: () -> Unit,
    onToggleWarmup: () -> Unit,
    onDelete: () -> Unit,
) {
    val fill = if (set.isCompleted) GbbColors.Dusk else GbbColors.Night
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetLabel(label, set.isWarmup, onToggleWarmup, onDelete)
        when (type) {
            TrackingType.WEIGHT_REPS -> {
                WeightField(set.weightKg, units, fill) { v -> onUpdate { it.copy(weightKg = v) } }
                RepsField(set.reps, fill) { v -> onUpdate { it.copy(reps = v) } }
            }
            TrackingType.BODYWEIGHT_REPS -> {
                WeightField(set.addedWeightKg, units, fill, placeholder = "BW") { v -> onUpdate { it.copy(addedWeightKg = v) } }
                RepsField(set.reps, fill) { v -> onUpdate { it.copy(reps = v) } }
            }
            TrackingType.DURATION -> DurationField(set.durationSec, fill) { v -> onUpdate { it.copy(durationSec = v) } }
            TrackingType.DISTANCE_DURATION -> {
                DecimalField(
                    dbValue = set.distanceM?.let { Units.metersToDisplay(it, units) },
                    fill = fill,
                    placeholder = "0.0",
                ) { v -> onUpdate { it.copy(distanceM = v?.let { d -> Units.displayToMeters(d, units) }) } }
                DurationField(set.durationSec, fill) { v -> onUpdate { it.copy(durationSec = v) } }
            }
            TrackingType.WEIGHT_DISTANCE -> {
                WeightField(set.weightKg, units, fill) { v -> onUpdate { it.copy(weightKg = v) } }
                DecimalField(
                    dbValue = set.distanceM?.let { Units.metersToShort(it, units) },
                    fill = fill,
                    placeholder = "0",
                ) { v -> onUpdate { it.copy(distanceM = v?.let { d -> Units.shortToMeters(d, units) }) } }
            }
        }
        SetCheckBox(
            checked = set.isCompleted,
            enabled = WorkoutMath.canComplete(type, set),
            onToggle = onToggleComplete,
        )
    }
}

@Composable
private fun SetLabel(label: String, isWarmup: Boolean, onToggleWarmup: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(LabelWidth)
            .height(44.dp)
            .clickable(role = Role.Button, onClickLabel = "Set options") { open = true },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            color = if (isWarmup) GbbColors.Orange else GbbColors.Chalk,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(if (isWarmup) "Mark as working set" else "Mark as warm-up", color = GbbColors.Chalk) },
                onClick = {
                    open = false
                    onToggleWarmup()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete set", color = GbbColors.Red) },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}

// Inputs keep their own text while you type and only resync when the stored
// value changes to something the text doesn't already represent. That way
// "145." or "1:3" mid-typing isn't reformatted under your thumb.

@Composable
private fun RowScope.SyncedField(
    dbText: String,
    normalize: (String) -> String,
    onCommit: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    fill: Color,
) {
    var text by remember { mutableStateOf(dbText) }
    LaunchedEffect(dbText) {
        if (normalize(text) != dbText) text = dbText
    }
    PixelTextField(
        value = text,
        onValueChange = {
            text = it
            onCommit(it)
        },
        modifier = Modifier.weight(1f),
        placeholder = placeholder,
        keyboardType = keyboardType,
        textAlign = TextAlign.Center,
        fill = fill,
        textStyle = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun RowScope.WeightField(
    kg: Double?,
    units: UnitSystem,
    fill: Color,
    placeholder: String = "0",
    onChange: (Double?) -> Unit,
) = SyncedField(
    dbText = kg?.let { Units.formatNumber(Units.kgToDisplay(it, units), 2) }.orEmpty(),
    normalize = { t -> SetFormat.parseDecimal(t)?.let { Units.formatNumber(it, 2) }.orEmpty() },
    onCommit = { t -> onChange(SetFormat.parseDecimal(t)?.let { Units.displayToKg(it, units) }) },
    placeholder = placeholder,
    keyboardType = KeyboardType.Decimal,
    fill = fill,
)

@Composable
private fun RowScope.RepsField(reps: Int?, fill: Color, onChange: (Int?) -> Unit) = SyncedField(
    dbText = reps?.toString().orEmpty(),
    normalize = { t -> SetFormat.parseInt(t)?.toString().orEmpty() },
    onCommit = { t -> onChange(SetFormat.parseInt(t)?.takeIf { it >= 0 }) },
    placeholder = "0",
    keyboardType = KeyboardType.Number,
    fill = fill,
)

@Composable
private fun RowScope.DurationField(sec: Int?, fill: Color, onChange: (Int?) -> Unit) = SyncedField(
    dbText = sec?.let(SetFormat::duration).orEmpty(),
    normalize = { t -> SetFormat.parseDuration(t)?.let(SetFormat::duration).orEmpty() },
    onCommit = { t -> onChange(SetFormat.parseDuration(t)) },
    placeholder = "m:ss",
    keyboardType = KeyboardType.Text,
    fill = fill,
)

@Composable
private fun RowScope.DecimalField(dbValue: Double?, fill: Color, placeholder: String, onChange: (Double?) -> Unit) =
    SyncedField(
        dbText = dbValue?.let { Units.formatNumber(it, 2) }.orEmpty(),
        normalize = { t -> SetFormat.parseDecimal(t)?.let { Units.formatNumber(it, 2) }.orEmpty() },
        onCommit = { t -> onChange(SetFormat.parseDecimal(t)) },
        placeholder = placeholder,
        keyboardType = KeyboardType.Decimal,
        fill = fill,
    )
