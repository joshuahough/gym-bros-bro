package com.gymbrosbro.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.BodyweightRepository
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import com.gymbrosbro.app.data.settings.ReminderSettings
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import com.gymbrosbro.app.notifications.rememberNotificationPermission
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.NumberInputDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelChip
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.PixelTimePickerDialog
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val bodyweight: BodyweightRepository,
) : ViewModel() {
    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val latestBodyweight: StateFlow<BodyweightEntryEntity?> = bodyweight.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val weekStart: StateFlow<DayOfWeek> = settings.weekStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayOfWeek.MONDAY)

    val reminders: StateFlow<ReminderSettings> = settings.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings())

    val sound: StateFlow<Boolean> = settings.sound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val haptics: StateFlow<Boolean> = settings.haptics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setUnits(system: UnitSystem) = viewModelScope.launch { settings.setUnitSystem(system) }
    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { settings.setKeepScreenOn(enabled) }
    fun recordBodyweight(weightKg: Double) = viewModelScope.launch { bodyweight.record(weightKg) }
    fun setWeekStart(day: DayOfWeek) = viewModelScope.launch { settings.setWeekStart(day) }
    fun setReminders(enabled: Boolean) = viewModelScope.launch { settings.setRemindersEnabled(enabled) }
    fun setReminderTime(time: LocalTime) = viewModelScope.launch { settings.setReminderTime(time) }
    fun setGraceReminder(enabled: Boolean) = viewModelScope.launch { settings.setGraceReminder(enabled) }
    fun setSound(enabled: Boolean) = viewModelScope.launch { settings.setSound(enabled) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { settings.setHaptics(enabled) }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c, _ -> SettingsViewModel(c.settings, c.bodyweightRepository) }
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val latest by viewModel.latestBodyweight.collectAsStateWithLifecycle()
    val weekStart by viewModel.weekStart.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val sound by viewModel.sound.collectAsStateWithLifecycle()
    val haptics by viewModel.haptics.collectAsStateWithLifecycle()
    var editBodyweight by remember { mutableStateOf(false) }
    var editReminderTime by remember { mutableStateOf(false) }
    val enableReminders = rememberNotificationPermission { granted -> if (granted) viewModel.setReminders(true) }
    val units = LocalUnitSystem.current

    Column(Modifier.fillMaxSize()) {
        PixelTopBar("SETTINGS", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingPanel("UNITS", "Changing units converts everything; nothing is lost.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelChip("Pounds · miles", units == UnitSystem.IMPERIAL, { viewModel.setUnits(UnitSystem.IMPERIAL) })
                    PixelChip("Kilograms · km", units == UnitSystem.METRIC, { viewModel.setUnits(UnitSystem.METRIC) })
                }
            }
            SettingPanel("BODYWEIGHT", "Used for calisthenics volume. Each workout keeps the weight you were at that day.") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val l = latest
                    Text(
                        if (l == null) "Not set" else "${Units.formatWeight(l.weightKg, units)}  ·  ${l.date.display()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = GbbColors.Chalk,
                        modifier = Modifier.weight(1f),
                    )
                    PixelButton("UPDATE", onClick = { editBodyweight = true }, small = true)
                }
            }
            SettingPanel(
                "REMINDERS",
                "One notification on days you have a workout planned, and none once you've trained. Off by default.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelChip("On", reminders.enabled, { enableReminders() })
                    PixelChip("Off", !reminders.enabled, { viewModel.setReminders(false) })
                }
                if (reminders.enabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            reminders.time.format(timeFormat),
                            style = MaterialTheme.typography.titleMedium,
                            color = GbbColors.Chalk,
                            modifier = Modifier.weight(1f),
                        )
                        PixelButton("CHANGE", onClick = { editReminderTime = true }, small = true)
                    }
                    Text(
                        "Grace-day reminder: the day after a missed workout, if training that day would save your COMBO.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GbbColors.Fog,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PixelChip("On", reminders.graceDay, { viewModel.setGraceReminder(true) })
                        PixelChip("Off", !reminders.graceDay, { viewModel.setGraceReminder(false) })
                    }
                }
            }
            SettingPanel("WEEK STARTS ON", "For the calendar, weekly streak and weekly stats.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelChip("Monday", weekStart == DayOfWeek.MONDAY, { viewModel.setWeekStart(DayOfWeek.MONDAY) })
                    PixelChip("Sunday", weekStart == DayOfWeek.SUNDAY, { viewModel.setWeekStart(DayOfWeek.SUNDAY) })
                }
            }
            SettingPanel("SOUND", "Chiptune blips for finished sets, high scores and level-ups.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelChip("On", sound, { viewModel.setSound(true) })
                    PixelChip("Off", !sound, { viewModel.setSound(false) })
                }
            }
            SettingPanel("VIBRATION", "A buzz at the same moments.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelChip("On", haptics, { viewModel.setHaptics(true) })
                    PixelChip("Off", !haptics, { viewModel.setHaptics(false) })
                }
            }
            SettingPanel("KEEP SCREEN AWAKE", "Stops the screen locking during a workout.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelChip("On", keepScreenOn, { viewModel.setKeepScreenOn(true) })
                    PixelChip("Off", !keepScreenOn, { viewModel.setKeepScreenOn(false) })
                }
            }
        }
    }

    if (editReminderTime) {
        PixelTimePickerDialog(
            title = "REMINDER TIME",
            initial = reminders.time,
            onConfirm = {
                viewModel.setReminderTime(it)
                editReminderTime = false
            },
            onDismiss = { editReminderTime = false },
        )
    }

    if (editBodyweight) {
        NumberInputDialog(
            title = "BODYWEIGHT",
            unitLabel = Units.weightUnit(units),
            initial = latest?.let { Units.formatNumber(Units.kgToDisplay(it.weightKg, units), 1) }.orEmpty(),
            onConfirm = { value ->
                viewModel.recordBodyweight(Units.displayToKg(value, units))
                editBodyweight = false
            },
            onDismiss = { editBodyweight = false },
        )
    }
}

@Composable
private fun SettingPanel(title: String, description: String, content: @Composable () -> Unit) {
    PixelPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
            Text(description, style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
            content()
        }
    }
}
