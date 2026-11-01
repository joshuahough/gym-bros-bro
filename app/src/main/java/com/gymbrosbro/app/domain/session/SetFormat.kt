package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units

/** Parsing for set inputs and compact "last time" summaries. */
object SetFormat {
    /** 45 → "0:45", 125 → "2:05", 3725 → "1:02:05". */
    fun duration(totalSec: Int): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "$h:${m.pad()}:${s.pad()}" else "$m:${s.pad()}"
    }

    /** Accepts "ss", "m:ss" or "h:mm:ss". Blank or malformed → null. */
    fun parseDuration(text: String): Int? {
        val parts = text.trim().split(":")
        if (parts.any { it.isEmpty() || !it.all(Char::isDigit) } || parts.size > 3) return null
        return parts.fold(0) { acc, part -> acc * 60 + part.toInt() }
    }

    /** Accepts "," as the decimal separator too. */
    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    fun parseInt(text: String): Int? = text.trim().toIntOrNull()

    /** Completed sets as one line, e.g. "145 lb × 8, 8, 8" or "BW+25 lb × 6, 5". */
    fun summary(type: TrackingType, sets: List<SetEntryEntity>, system: UnitSystem): String {
        val done = sets.filter { it.isCompleted }.sortedBy { it.position }
        if (done.isEmpty()) return ""
        return when (type) {
            TrackingType.WEIGHT_REPS -> groupedReps(done) { s ->
                s.weightKg?.let { Units.formatWeight(it, system) } ?: "?"
            }
            TrackingType.BODYWEIGHT_REPS -> groupedReps(done) { s -> bodyweightLabel(s.addedWeightKg, system) }
            TrackingType.DURATION -> done.joinToString(", ") { duration(it.durationSec ?: 0) }
            TrackingType.DISTANCE_DURATION -> done.joinToString(", ") { s ->
                listOfNotNull(
                    s.distanceM?.let { Units.formatDistance(it, system) },
                    s.durationSec?.let { duration(it) },
                ).joinToString(" in ")
            }
            TrackingType.WEIGHT_DISTANCE -> done.joinToString(", ") { s ->
                val distance = s.distanceM?.let {
                    "${Units.formatNumber(Units.metersToShort(it, system), 0)} ${Units.shortDistanceUnit(system)}"
                } ?: "?"
                "${s.weightKg?.let { Units.formatWeight(it, system) } ?: "?"} × $distance"
            }
        }
    }

    fun bodyweightLabel(addedKg: Double?, system: UnitSystem): String = when {
        addedKg == null || addedKg == 0.0 -> "BW"
        addedKg > 0 -> "BW+${Units.formatWeight(addedKg, system)}"
        else -> "BW−${Units.formatWeight(-addedKg, system)}"
    }

    /** Consecutive sets at the same load share a label: "135 lb × 10 / 145 lb × 8, 8". */
    private fun groupedReps(sets: List<SetEntryEntity>, loadLabel: (SetEntryEntity) -> String): String {
        val groups = mutableListOf<Pair<String, MutableList<Int>>>()
        sets.forEach { s ->
            val label = loadLabel(s)
            if (groups.lastOrNull()?.first == label) groups.last().second += s.reps ?: 0
            else groups += label to mutableListOf(s.reps ?: 0)
        }
        return groups.joinToString(" / ") { (label, reps) -> "$label × ${reps.joinToString(", ")}" }
    }

    private fun Int.pad() = toString().padStart(2, '0')
}
