package com.gymbrosbro.app.domain.units

import kotlin.math.abs
import kotlin.math.roundToLong

enum class UnitSystem { IMPERIAL, METRIC }

/**
 * Storage is canonical — kilograms, meters, seconds — and only converted for
 * display, so switching unit systems never loses precision (SPEC §3).
 */
object Units {
    const val KG_PER_LB = 0.45359237
    const val METERS_PER_MILE = 1609.344
    const val METERS_PER_KM = 1000.0

    fun kgToDisplay(kg: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) kg / KG_PER_LB else kg

    fun displayToKg(value: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) value * KG_PER_LB else value

    fun metersToDisplay(meters: Double, system: UnitSystem): Double =
        meters / if (system == UnitSystem.IMPERIAL) METERS_PER_MILE else METERS_PER_KM

    fun displayToMeters(value: Double, system: UnitSystem): Double =
        value * if (system == UnitSystem.IMPERIAL) METERS_PER_MILE else METERS_PER_KM

    fun weightUnit(system: UnitSystem): String = if (system == UnitSystem.IMPERIAL) "lb" else "kg"

    fun distanceUnit(system: UnitSystem): String = if (system == UnitSystem.IMPERIAL) "mi" else "km"

    fun formatWeight(kg: Double, system: UnitSystem): String =
        "${formatNumber(kgToDisplay(kg, system), decimals = 1)} ${weightUnit(system)}"

    fun formatDistance(meters: Double, system: UnitSystem): String =
        "${formatNumber(metersToDisplay(meters, system), decimals = 2)} ${distanceUnit(system)}"

    /** Rounds to [decimals] places and drops trailing zeros: 45.0 → "45", 20.41 → "20.4". */
    fun formatNumber(value: Double, decimals: Int): String {
        var scale = 1L
        repeat(decimals) { scale *= 10 }
        val scaled = (abs(value) * scale).roundToLong()
        val whole = scaled / scale
        val fraction = (scaled % scale).toString().padStart(decimals, '0').trimEnd('0')
        val sign = if (value < 0 && scaled != 0L) "-" else ""
        return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
    }
}
