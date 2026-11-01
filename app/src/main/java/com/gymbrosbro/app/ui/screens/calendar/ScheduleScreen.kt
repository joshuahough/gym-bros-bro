package com.gymbrosbro.app.ui.screens.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.ScheduleRepository
import com.gymbrosbro.app.data.TemplateRepository
import com.gymbrosbro.app.data.db.entity.SchedulePauseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleRuleEntity
import com.gymbrosbro.app.data.db.entity.WorkoutTemplateEntity
import com.gymbrosbro.app.data.settings.ReminderSettings
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.model.ScheduleKind
import com.gymbrosbro.app.domain.schedule.Schedule
import com.gymbrosbro.app.notifications.rememberNotificationPermission
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.ConfirmDialog
import com.gymbrosbro.app.ui.components.FutureDatePickerDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelChip
import com.gymbrosbro.app.ui.components.PixelDialog
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.PixelStepper
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class ScheduleUiState(
    /** Rules still in effect today or later. */
    val rules: List<ScheduleRuleEntity>,
    val templateNames: Map<Long, String>,
    /** Workouts you can schedule (not archived). */
    val templates: List<WorkoutTemplateEntity>,
    /** Pauses that haven't ended yet. */
    val pauses: List<SchedulePauseEntity>,
    val weekStart: DayOfWeek,
    val weeklyGoal: Int,
    val reminders: ReminderSettings,
)

class ScheduleViewModel(
    private val schedule: ScheduleRepository,
    templates: TemplateRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<ScheduleUiState?> = combine(
        schedule.observe(),
        templates.observeAll(),
        combine(settings.weekStart, settings.weeklyGoal, ::Pair),
        settings.reminders,
    ) { data, all, (weekStart, goal), reminders ->
        val today = LocalDate.now()
        ScheduleUiState(
            rules = data.rules.filter { it.endDate == null || it.endDate >= today },
            templateNames = all.associate { it.id to it.name },
            templates = all.filter { it.archivedAt == null }.sortedBy { it.sortOrder },
            pauses = data.pauses.filter { it.endDate >= today },
            weekStart = weekStart,
            weeklyGoal = goal,
            reminders = reminders,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Adds a rule; [onFirstRule] fires when it's the first one ever, to offer reminders. */
    fun addRule(templateId: Long?, kind: ScheduleKind, days: Int, interval: Int, date: LocalDate, onFirstRule: () -> Unit) =
        viewModelScope.launch {
            val first = !schedule.hasRules()
            schedule.addRule(
                templateId = templateId,
                kind = kind,
                startDate = if (kind == ScheduleKind.ONE_OFF) date else LocalDate.now(),
                daysOfWeek = if (kind == ScheduleKind.WEEKLY) days else 0,
                intervalDays = if (kind == ScheduleKind.EVERY_N_DAYS) interval else null,
            )
            if (first && state.value?.reminders?.offered != true) onFirstRule()
        }

    fun removeRule(id: Long) = viewModelScope.launch { schedule.removeRule(id) }

    fun pauseUntil(end: LocalDate) = viewModelScope.launch { schedule.pause(LocalDate.now(), end) }

    fun endPause(pause: SchedulePauseEntity) = viewModelScope.launch { schedule.endPause(pause) }

    fun setWeeklyGoal(goal: Int) = viewModelScope.launch { settings.setWeeklyGoal(goal) }

    fun answerReminderOffer(enabled: Boolean) = viewModelScope.launch {
        if (enabled) settings.setRemindersEnabled(true) else settings.setReminderOffered()
    }
}

/** Set up the repeating plan, pause it, and set the weekly goal. */
@Composable
fun ScheduleScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c, _ -> ScheduleViewModel(c.scheduleRepository, c.templateRepository, c.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<ScheduleRuleEntity?>(null) }
    var pickPauseEnd by remember { mutableStateOf(false) }
    var offerReminders by remember { mutableStateOf(false) }
    val askPermission = rememberNotificationPermission { granted -> viewModel.answerReminderOffer(granted) }

    Column(Modifier.fillMaxSize()) {
        PixelTopBar("SCHEDULE", onBack = onBack)
        val s = state ?: return@Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PixelPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("YOUR PLAN", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    if (s.rules.isEmpty()) {
                        Text(
                            "Nothing scheduled yet. Add your training days and every one you hit builds your COMBO.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GbbColors.Fog,
                        )
                    }
                    s.rules.forEach { rule ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    rule.templateId?.let { s.templateNames[it] } ?: "Any workout",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GbbColors.Chalk,
                                )
                                Text(describe(rule, s.weekStart), style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                            }
                            PixelButton("REMOVE", onClick = { removing = rule }, small = true, color = GbbColors.Steel, contentColor = GbbColors.Chalk)
                        }
                    }
                    PixelButton("+ ADD TO SCHEDULE", onClick = { adding = true }, modifier = Modifier.fillMaxWidth())
                }
            }

            PixelPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PAUSE", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    Text(
                        "Vacation or sick? Pausing turns planned days off and freezes your COMBO.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GbbColors.Fog,
                    )
                    s.pauses.forEach { pause ->
                        val today = LocalDate.now()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (pause.startDate <= today) "Paused through ${pause.endDate.display()}"
                                else "${pause.startDate.display()} – ${pause.endDate.display()}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = GbbColors.Chalk,
                                modifier = Modifier.weight(1f),
                            )
                            PixelButton(
                                if (pause.startDate <= today) "RESUME" else "CANCEL",
                                onClick = { viewModel.endPause(pause) },
                                small = true,
                            )
                        }
                    }
                    if (s.pauses.none { it.startDate <= LocalDate.now() }) {
                        PixelButton(
                            "PAUSE FROM TODAY",
                            onClick = { pickPauseEnd = true },
                            color = GbbColors.Steel,
                            contentColor = GbbColors.Chalk,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            PixelPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("WEEKLY GOAL", style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
                    Text(
                        "Workouts per week for your weekly streak. Any workout counts, planned or not.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GbbColors.Fog,
                    )
                    PixelStepper(s.weeklyGoal, viewModel::setWeeklyGoal, range = 1..7)
                }
            }
        }

        if (adding) {
            AddRuleDialog(
                templates = s.templates,
                weekStart = s.weekStart,
                onSave = { templateId, kind, days, interval, date ->
                    adding = false
                    viewModel.addRule(templateId, kind, days, interval, date, onFirstRule = { offerReminders = true })
                },
                onDismiss = { adding = false },
            )
        }
    }

    removing?.let { rule ->
        ConfirmDialog(
            title = "REMOVE FROM SCHEDULE?",
            message = "Upcoming days come off the calendar. Days already past stay, so your streak history doesn't change.",
            confirmText = "REMOVE",
            destructive = true,
            onConfirm = {
                viewModel.removeRule(rule.id)
                removing = null
            },
            onDismiss = { removing = null },
        )
    }

    if (pickPauseEnd) {
        FutureDatePickerDialog(
            initial = LocalDate.now().plusDays(6),
            onConfirm = { end ->
                pickPauseEnd = false
                viewModel.pauseUntil(end)
            },
            onDismiss = { pickPauseEnd = false },
        )
    }

    if (offerReminders) {
        PixelDialog("REMINDERS?", onDismiss = {
            offerReminders = false
            viewModel.answerReminderOffer(false)
        }) {
            Text(
                "Want a nudge on workout days? One notification at most, and none once you've trained. You can change this anytime in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = GbbColors.Chalk,
            )
            PixelButton("YES, REMIND ME", onClick = {
                offerReminders = false
                askPermission()
            }, modifier = Modifier.fillMaxWidth())
            PixelButton("NO THANKS", onClick = {
                offerReminders = false
                viewModel.answerReminderOffer(false)
            }, color = GbbColors.Steel, contentColor = GbbColors.Chalk, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddRuleDialog(
    templates: List<WorkoutTemplateEntity>,
    weekStart: DayOfWeek,
    onSave: (templateId: Long?, kind: ScheduleKind, days: Int, interval: Int, date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var templateId by remember { mutableStateOf(templates.firstOrNull()?.id) }
    var kind by remember { mutableStateOf(ScheduleKind.WEEKLY) }
    var days by remember { mutableIntStateOf(0) }
    var interval by remember { mutableIntStateOf(2) }
    var date by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var pickDate by remember { mutableStateOf(false) }
    val valid = kind != ScheduleKind.WEEKLY || days != 0

    PixelDialog("ADD TO SCHEDULE", onDismiss) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Workout", style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                templates.forEach { t -> PixelChip(t.name, templateId == t.id, { templateId = t.id }) }
                PixelChip("Any workout", templateId == null, { templateId = null })
            }
            Text("Repeat", style = MaterialTheme.typography.labelMedium, color = GbbColors.Fog)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelChip("Weekly", kind == ScheduleKind.WEEKLY, { kind = ScheduleKind.WEEKLY })
                PixelChip("Every few days", kind == ScheduleKind.EVERY_N_DAYS, { kind = ScheduleKind.EVERY_N_DAYS })
                PixelChip("One day", kind == ScheduleKind.ONE_OFF, { kind = ScheduleKind.ONE_OFF })
            }
            when (kind) {
                ScheduleKind.WEEKLY -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0L until 7L).map { weekStart.plus(it) }.forEach { day ->
                        val bit = Schedule.dayBit(day)
                        PixelChip(
                            day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            days and bit != 0,
                            { days = days xor bit },
                        )
                    }
                }
                ScheduleKind.EVERY_N_DAYS -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Every", style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)
                    PixelStepper(interval, { interval = it }, range = 2..14)
                    Text("days, from today", style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)
                }
                ScheduleKind.ONE_OFF -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(date.display(), style = MaterialTheme.typography.titleMedium, color = GbbColors.Chalk, modifier = Modifier.weight(1f))
                    PixelButton("CHANGE", onClick = { pickDate = true }, small = true, color = GbbColors.Steel, contentColor = GbbColors.Chalk)
                }
            }
            PixelButton(
                "SAVE",
                onClick = { onSave(templateId, kind, days, interval, date) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            )
            PixelButton("CANCEL", onClick = onDismiss, color = GbbColors.Steel, contentColor = GbbColors.Chalk, modifier = Modifier.fillMaxWidth())
        }
    }

    if (pickDate) {
        FutureDatePickerDialog(
            initial = date,
            onConfirm = {
                date = it
                pickDate = false
            },
            onDismiss = { pickDate = false },
        )
    }
}

/** "Mon · Wed · Fri", "Every 3 days", "Sat, Nov 21". */
private fun describe(rule: ScheduleRuleEntity, weekStart: DayOfWeek): String = when (rule.kind) {
    ScheduleKind.WEEKLY -> (0L until 7L).map { weekStart.plus(it) }
        .filter { Schedule.hasDay(rule.daysOfWeek, it) }
        .joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    ScheduleKind.EVERY_N_DAYS -> "Every ${rule.intervalDays} days"
    ScheduleKind.ONE_OFF -> rule.startDate.display()
}
