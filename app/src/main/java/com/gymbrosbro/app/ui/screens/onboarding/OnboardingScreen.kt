package com.gymbrosbro.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.avatar.AvatarConfig
import com.gymbrosbro.app.data.BodyweightRepository
import com.gymbrosbro.app.data.ScheduleRepository
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.model.ScheduleKind
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.AvatarView
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelChip
import com.gymbrosbro.app.ui.components.PixelTextField
import com.gymbrosbro.app.ui.screens.settings.BackupControls
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Plausible bodyweights; anything outside is almost certainly a typo or the wrong unit. */
private val BODYWEIGHT_RANGE_KG = 25.0..350.0

class OnboardingViewModel(
    private val settings: SettingsRepository,
    private val bodyweight: BodyweightRepository,
    private val schedule: ScheduleRepository,
    /** Finishing writes several things; they all land even if the screen goes away mid-way. */
    private val appScope: CoroutineScope,
) : ViewModel() {
    fun setUnits(system: UnitSystem) = viewModelScope.launch { settings.setUnitSystem(system) }

    fun finish(name: String, bodyweightKg: Double?, days: Set<DayOfWeek>) = appScope.launch {
        settings.setPlayerName(name)
        bodyweightKg?.let { bodyweight.record(it) }
        if (days.isNotEmpty()) {
            schedule.addRule(
                templateId = null,
                kind = ScheduleKind.WEEKLY,
                startDate = LocalDate.now(),
                daysOfWeek = Onboarding.dayMask(days),
            )
            settings.setWeeklyGoal(days.size)
        }
        // Last, so the app only opens once everything above is saved.
        settings.setOnboarded()
    }

    fun skip() = appScope.launch { settings.setOnboarded() }
}

object Onboarding {
    /** Schedule rule bits: Monday = bit 0 … Sunday = bit 6. */
    fun dayMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    /** The typed bodyweight in kg; null when blank or implausible. */
    fun parseBodyweightKg(text: String, units: UnitSystem): Double? {
        val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return Units.displayToKg(value, units).takeIf { it in BODYWEIGHT_RANGE_KG }
    }
}

private enum class Step { WELCOME, PROFILE, SCHEDULE }

/** First-run setup (SPEC §11 Phase 6). Every step can be skipped; all of it is in Settings later. */
@Composable
fun OnboardingScreen() {
    val viewModel = appViewModel { c, _ ->
        OnboardingViewModel(c.settings, c.bodyweightRepository, c.scheduleRepository, c.appScope)
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf(setOf<DayOfWeek>()) }
    val units = LocalUnitSystem.current
    val current = Step.entries[step]

    BackHandler(enabled = step > 0) { step-- }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepDots(step, Step.entries.size)
        when (current) {
            Step.WELCOME -> WelcomeStep(
                onStart = { step = 1 },
                onSkip = viewModel::skip,
            )
            Step.PROFILE -> ProfileStep(
                name = name,
                onName = { name = it },
                units = units,
                onUnits = viewModel::setUnits,
                weightText = weightText,
                onWeight = { weightText = it },
                onNext = { step = 2 },
            )
            Step.SCHEDULE -> ScheduleStep(
                days = days,
                onToggle = { day -> days = if (day in days) days - day else days + day },
                onDone = { viewModel.finish(name, Onboarding.parseBodyweightKg(weightText, units), days) },
            )
        }
    }
}

@Composable
private fun StepDots(step: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(Modifier.size(width = 24.dp, height = 6.dp).background(if (i <= step) GbbColors.Gold else GbbColors.Steel))
        }
    }
}

@Composable
private fun ColumnScope.WelcomeStep(onStart: () -> Unit, onSkip: () -> Unit) {
    Spacer(Modifier.size(12.dp))
    Text(
        "GYM BROS BRO",
        style = MaterialTheme.typography.headlineMedium,
        color = GbbColors.Gold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        AvatarView(AvatarConfig(), 150.dp)
    }
    Body(
        "Log your lifts, runs and stretches. Every workout earns XP, keeps your streak alive " +
            "and unlocks gear for your bro.",
    )
    Body("No sign-up and no ads. Your data stays on your phone and in its Google backup.")
    PixelButton("PRESS START", onClick = onStart, modifier = Modifier.fillMaxWidth())
    Hint("Played before? Restore a backup file instead.")
    BackupControls(Modifier.fillMaxWidth(), showExport = false, restoreLabel = "RESTORE A BACKUP")
    PixelButton(
        "SKIP SETUP",
        onClick = onSkip,
        small = true,
        color = GbbColors.Night,
        contentColor = GbbColors.Fog,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.ProfileStep(
    name: String,
    onName: (String) -> Unit,
    units: UnitSystem,
    onUnits: (UnitSystem) -> Unit,
    weightText: String,
    onWeight: (String) -> Unit,
    onNext: () -> Unit,
) {
    Title("WHO'S PLAYING?")
    Label("PLAYER NAME")
    PixelTextField(
        value = name,
        onValueChange = { v -> onName(v.filter { it.isLetterOrDigit() || it == ' ' }.take(SettingsRepository.PLAYER_NAME_MAX)) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = SettingsRepository.DEFAULT_PLAYER_NAME,
        imeAction = ImeAction.Done,
    )
    Label("UNITS")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PixelChip("Pounds · miles", units == UnitSystem.IMPERIAL, { onUnits(UnitSystem.IMPERIAL) })
        PixelChip("Kilograms · km", units == UnitSystem.METRIC, { onUnits(UnitSystem.METRIC) })
    }
    Label("BODYWEIGHT (OPTIONAL)")
    Hint("Counts toward volume for push-ups, pull-ups and other bodyweight moves.")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PixelTextField(
            value = weightText,
            onValueChange = { v -> onWeight(v.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        )
        Text(Units.weightUnit(units), style = MaterialTheme.typography.titleMedium, color = GbbColors.Chalk)
    }
    val weightInvalid = weightText.isNotBlank() && Onboarding.parseBodyweightKg(weightText, units) == null
    if (weightInvalid) {
        Text("That doesn't look right. Check the number and units.", style = MaterialTheme.typography.bodySmall, color = GbbColors.Red)
    }
    PixelButton("NEXT", onClick = onNext, enabled = !weightInvalid, modifier = Modifier.fillMaxWidth())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.ScheduleStep(days: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit, onDone: () -> Unit) {
    Title("WHEN DO YOU TRAIN?")
    Body("Pick your usual days. They go on the calendar, and training on them builds your COMBO streak.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEach { day ->
            PixelChip(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), day in days, { onToggle(day) })
        }
    }
    Hint(
        if (days.isEmpty()) {
            "No fixed days? Leave them all off. You can set a plan any time from the calendar."
        } else {
            "Weekly goal: ${days.size} workout${if (days.size == 1) "" else "s"}. Change it any time from the calendar."
        },
    )
    PixelButton("LET'S GO", onClick = onDone, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Title(text: String) =
    Text(text, style = MaterialTheme.typography.headlineSmall, color = GbbColors.Gold)

@Composable
private fun Label(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium, color = GbbColors.Cyan)

@Composable
private fun Body(text: String) =
    Text(text, style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)

@Composable
private fun Hint(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
