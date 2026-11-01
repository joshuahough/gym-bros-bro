package com.gymbrosbro.app.ui

import com.gymbrosbro.app.domain.session.SetFormat
import com.gymbrosbro.app.domain.session.WorkoutTotals
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val dayYearFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

fun Instant.localDate(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()

/** "Mon, Nov 3", with the year only when it isn't this year. */
fun LocalDate.display(): String =
    format(if (year == LocalDate.now().year) dayFormat else dayYearFormat)

/** Volume with thousands separators: "12,450 lb". */
fun formatVolume(kg: Double, system: UnitSystem): String =
    "%,d %s".format(Locale.getDefault(), Units.kgToDisplay(kg, system).toLong(), Units.weightUnit(system))

/** "18 sets · 12,450 lb · 3.1 mi", skipping parts that are zero. */
fun WorkoutTotals.summaryLine(system: UnitSystem): String = listOfNotNull(
    "$sets ${if (sets == 1) "set" else "sets"}",
    volumeKg.takeIf { it > 0 }?.let { formatVolume(it, system) },
    distanceM.takeIf { it > 0 }?.let { Units.formatDistance(it, system) },
    durationSec.takeIf { it > 0 && reps == 0 }?.let { SetFormat.duration(it) },
).joinToString(" · ")
