package com.gymbrosbro.app.ui

import com.gymbrosbro.app.domain.records.RecordType
import com.gymbrosbro.app.domain.session.SetFormat
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units

val RecordType.label: String
    get() = when (this) {
        RecordType.MAX_WEIGHT -> "Heaviest weight"
        RecordType.BEST_E1RM -> "Best est. 1-rep max"
        RecordType.MAX_REPS -> "Most reps in a set"
        RecordType.MAX_SESSION_VOLUME -> "Most volume in a workout"
        RecordType.MAX_SESSION_REPS -> "Most reps in a workout"
        RecordType.LONGEST_DURATION -> "Longest time"
        RecordType.LONGEST_DISTANCE -> "Longest distance"
    }

/** A record's value in the user's units: "155 lb", "12 reps", "1:30". */
fun RecordType.format(value: Double, units: UnitSystem): String = when (this) {
    RecordType.MAX_WEIGHT, RecordType.BEST_E1RM -> Units.formatWeight(value, units)
    RecordType.MAX_SESSION_VOLUME -> formatVolume(value, units)
    RecordType.MAX_REPS, RecordType.MAX_SESSION_REPS -> "${value.toInt()} reps"
    RecordType.LONGEST_DURATION -> SetFormat.duration(value.toInt())
    RecordType.LONGEST_DISTANCE -> Units.formatDistance(value, units)
}
