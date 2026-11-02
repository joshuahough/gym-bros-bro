package com.gymbrosbro.app.ui

import com.gymbrosbro.app.avatar.Locker
import com.gymbrosbro.app.avatar.LockerItem
import com.gymbrosbro.app.avatar.Unlock
import com.gymbrosbro.app.domain.progress.Trophies
import com.gymbrosbro.app.domain.progress.Trophy
import com.gymbrosbro.app.domain.progress.TrophyMetric
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import java.util.Locale

private fun count(n: Double) = "%,d".format(Locale.getDefault(), n.toLong())

/** A value of [metric] in the user's units: "12,450 lb", "26.2 mi", "37". */
fun TrophyMetric.format(value: Double, units: UnitSystem): String = when (this) {
    TrophyMetric.VOLUME_KG -> formatVolume(value, units)
    TrophyMetric.DISTANCE_M -> Units.formatDistance(value, units)
    else -> count(value)
}

/** What it takes: "Finish 50 workouts". */
fun Trophy.description(units: UnitSystem): String = when (metric) {
    TrophyMetric.WORKOUTS -> if (target == 1.0) "Finish your first workout" else "Finish ${count(target)} workouts"
    TrophyMetric.RECORDS -> if (target == 1.0) "Set your first high score" else "Set ${count(target)} high scores"
    TrophyMetric.VOLUME_KG -> "Lift ${formatVolume(target, units)} in total"
    TrophyMetric.DISTANCE_M -> "Cover a marathon (${Units.formatDistance(target, units)}) in total"
    TrophyMetric.PULL_UPS -> "Do ${count(target)} pull-ups in total"
    TrophyMetric.WEEKLY_STREAK -> "Hit your weekly goal ${count(target)} weeks in a row"
    TrophyMetric.COMBO -> "Make ${count(target)} scheduled workouts in a row"
}

/** The locker item a trophy unlocks, if any. */
val Trophy.reward: LockerItem?
    get() = Locker.items.firstOrNull { (it.unlock as? Unlock.ByTrophy)?.trophyId == id }

/** "LV 5", "TROPHY: GYM RAT". */
val LockerItem.requirement: String
    get() = when (val u = unlock) {
        Unlock.Starter -> ""
        is Unlock.AtLevel -> "Reach level ${u.level}"
        is Unlock.ByTrophy -> "Earn the ${Trophies.byId(u.trophyId)?.name ?: "trophy"} trophy"
    }
