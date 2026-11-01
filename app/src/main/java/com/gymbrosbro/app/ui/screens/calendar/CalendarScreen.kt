package com.gymbrosbro.app.ui.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbrosbro.app.domain.schedule.DayStatus
import com.gymbrosbro.app.domain.schedule.Occurrence
import com.gymbrosbro.app.domain.schedule.Schedule
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelChip
import com.gymbrosbro.app.ui.components.PixelDialog
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.ScreenHeader
import com.gymbrosbro.app.ui.components.StatTile
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.screens.workouts.StartWorkoutDialog
import com.gymbrosbro.app.ui.screens.workouts.WorkoutsViewModel
import com.gymbrosbro.app.ui.screens.workouts.rememberStartWorkoutController
import com.gymbrosbro.app.ui.theme.GbbColors
import com.gymbrosbro.app.ui.theme.PixelDimens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/** Month view of the plan and what you actually did (SPEC §7.5). */
@Composable
fun CalendarScreen(onOpenSession: (Long) -> Unit, onEditSchedule: () -> Unit) {
    val viewModel = appViewModel { c, _ ->
        CalendarViewModel(c.scheduleRepository, c.sessionRepository, c.progressRepository, c.templateRepository, c.settings)
    }
    val workouts = appViewModel { c, _ -> WorkoutsViewModel(c.templateRepository, c.sessionRepository) }
    val starter = rememberStartWorkoutController(workouts, onOpenSession)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var moving by remember { mutableStateOf<PlannedWorkout?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("CALENDAR", subtitle = "Plan your week. Keep your combo.")
        val s = state ?: return@Column

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("COMBO", "×${s.streaks.schedule.current}", GbbColors.Pink, Modifier.weight(1f))
            StatTile("WEEKLY", "×${s.streaks.weekly.current}", GbbColors.Cyan, Modifier.weight(1f))
        }
        s.streaks.schedule.atRisk?.let { missed ->
            Text(
                "Missed ${missed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} — train today to keep your COMBO.",
                style = MaterialTheme.typography.bodyMedium,
                color = GbbColors.Gold,
            )
        }

        PixelPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NavArrow("<") { viewModel.showMonth(-1) }
                    Text(
                        s.month.format(monthTitle).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = GbbColors.Gold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    NavArrow(">") { viewModel.showMonth(1) }
                }
                MonthGrid(s, onSelect = viewModel::select)
                Legend()
            }
        }

        DayPanel(
            day = s.selected,
            onStart = { starter.request(it) },
            onSkip = viewModel::skip,
            onRestore = viewModel::restore,
            onMove = { moving = it },
            onOpenSession = onOpenSession,
            onLogPast = { viewModel.logPastWorkout(s.selected.date, onOpenSession) },
        )

        PixelButton(
            if (s.hasSchedule) "EDIT SCHEDULE" else "SET UP SCHEDULE",
            onClick = onEditSchedule,
            color = if (s.hasSchedule) GbbColors.Steel else GbbColors.Gold,
            contentColor = if (s.hasSchedule) GbbColors.Chalk else GbbColors.Void,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    StartWorkoutDialog(starter)
    moving?.let { planned ->
        MoveDialog(
            planned = planned,
            weekStart = state?.weekStart ?: DayOfWeek.MONDAY,
            onMove = { date ->
                viewModel.move(planned.occurrence, date)
                moving = null
            },
            onDismiss = { moving = null },
        )
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onSelect: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            (0L until 7L).forEach { i ->
                Text(
                    state.weekStart.plus(i).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = GbbColors.Fog,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        state.days.chunked(7).forEach { week ->
            Row {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        isToday = day.date == today,
                        isSelected = day.date == state.selected.date,
                        onClick = { onSelect(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val numberColor = when {
        isToday -> GbbColors.Gold
        day.inMonth -> GbbColors.Chalk
        else -> GbbColors.Steel
    }
    Column(
        modifier = modifier
            .height(52.dp)
            .semantics {
                selected = isSelected
                contentDescription = "${day.date.display()}, ${day.status.description}"
            }
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                if (isSelected) {
                    val u = PixelDimens.unit.toPx()
                    drawRect(GbbColors.Gold, Offset.Zero, Size(size.width, u))
                    drawRect(GbbColors.Gold, Offset(0f, size.height - u), Size(size.width, u))
                    drawRect(GbbColors.Gold, Offset.Zero, Size(u, size.height))
                    drawRect(GbbColors.Gold, Offset(size.width - u, 0f), Size(u, size.height))
                }
            }
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium, color = numberColor)
        Spacer(Modifier.height(6.dp))
        DayMarker(if (day.inMonth) day.status else day.status.takeIf { it == DayStatus.DONE } ?: DayStatus.NONE)
    }
}

/** Filled = done, outlined = planned, gray = missed, dash = skipped, dim = paused. */
@Composable
private fun DayMarker(status: DayStatus, size: Int = 14) {
    Box(
        Modifier.size(size.dp).drawBehind {
            val u = PixelDimens.unit.toPx()
            fun outline(c: Color) {
                drawRect(c, Offset.Zero, Size(this.size.width, u))
                drawRect(c, Offset(0f, this.size.height - u), Size(this.size.width, u))
                drawRect(c, Offset.Zero, Size(u, this.size.height))
                drawRect(c, Offset(this.size.width - u, 0f), Size(u, this.size.height))
            }
            when (status) {
                DayStatus.DONE -> drawRect(GbbColors.Green)
                DayStatus.SCHEDULED -> outline(GbbColors.Gold)
                DayStatus.MISSED -> drawRect(GbbColors.Steel)
                DayStatus.SKIPPED -> drawRect(GbbColors.Fog, Offset(0f, this.size.height / 2 - u / 2), Size(this.size.width, u))
                DayStatus.PAUSED -> outline(GbbColors.Dusk)
                DayStatus.NONE -> Unit
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(DayStatus.DONE, DayStatus.SCHEDULED, DayStatus.MISSED, DayStatus.SKIPPED, DayStatus.PAUSED).forEach { status ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                DayMarker(status, size = 10)
                Spacer(Modifier.size(4.dp))
                Text(status.description, style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
            }
        }
    }
}

private val DayStatus.description
    get() = when (this) {
        DayStatus.NONE -> "rest day"
        DayStatus.DONE -> "Done"
        DayStatus.SCHEDULED -> "Planned"
        DayStatus.MISSED -> "Missed"
        DayStatus.SKIPPED -> "Skipped"
        DayStatus.PAUSED -> "Paused"
    }

@Composable
private fun DayPanel(
    day: SelectedDay,
    onStart: (Long?) -> Unit,
    onSkip: (Occurrence) -> Unit,
    onRestore: (Occurrence) -> Unit,
    onMove: (PlannedWorkout) -> Unit,
    onOpenSession: (Long) -> Unit,
    onLogPast: () -> Unit,
) {
    val today = LocalDate.now()
    PixelPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (day.date == today) "TODAY" else day.date.display().uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = GbbColors.Cyan,
            )
            if (day.paused) Text("Schedule paused.", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)

            day.workouts.forEach { w ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenSession(w.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DayMarker(DayStatus.DONE, size = 10)
                    Spacer(Modifier.size(8.dp))
                    Text(w.name, style = MaterialTheme.typography.titleMedium, color = GbbColors.Chalk, modifier = Modifier.weight(1f))
                    Text(">", style = MaterialTheme.typography.titleLarge, color = GbbColors.Fog)
                }
            }

            day.planned.forEach { planned ->
                val o = planned.occurrence
                val upcoming = day.date >= today
                val status = when {
                    o.paused -> "paused"
                    o.skipped -> "skipped"
                    day.workouts.isNotEmpty() -> "done"
                    upcoming -> if (o.moved) "moved from ${o.originalDate.display()}" else "planned"
                    else -> "missed"
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${planned.name} · $status", style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)
                    if (upcoming && !o.paused && day.workouts.isEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when {
                                o.skipped -> SmallButton("UNSKIP") { onRestore(o) }
                                else -> {
                                    if (day.date == today) PixelButton("START", onClick = { onStart(o.templateId) }, small = true)
                                    SmallButton("MOVE") { onMove(planned) }
                                    SmallButton("SKIP") { onSkip(o) }
                                }
                            }
                            if (o.moved && !o.skipped) SmallButton("UNDO MOVE") { onRestore(o) }
                        }
                    }
                }
            }

            if (day.workouts.isEmpty() && day.planned.isEmpty()) {
                Text(
                    if (day.date > today) "Nothing planned." else "Rest day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GbbColors.Fog,
                )
            }
            if (day.date < today) {
                SmallButton("+ LOG PAST WORKOUT", onClick = onLogPast)
            }
        }
    }
}

/** Pick another day in the same week (SPEC §7.5: moves keep the streak, within the week). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoveDialog(planned: PlannedWorkout, weekStart: DayOfWeek, onMove: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    val week = Schedule.weekStart(planned.occurrence.originalDate, weekStart)
    val options = (0L until 7L).map { week.plusDays(it) }.filter { it >= today && it != planned.occurrence.date }
    PixelDialog("MOVE ${planned.name.uppercase()}", onDismiss) {
        if (options.isEmpty()) {
            Text("No other days left this week.", style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)
        } else {
            Text("Move it to another day this week:", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { date ->
                    PixelChip(
                        "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}",
                        selected = false,
                        onClick = { onMove(date) },
                    )
                }
            }
        }
        PixelButton("CANCEL", onClick = onDismiss, color = GbbColors.Steel, contentColor = GbbColors.Chalk, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    PixelButton(text, onClick = onClick, small = true, color = GbbColors.Steel, contentColor = GbbColors.Chalk)
}

@Composable
private fun NavArrow(label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = GbbColors.Gold)
    }
}
