package com.gymbrosbro.app.domain.schedule

import com.gymbrosbro.app.data.db.entity.ScheduleExceptionEntity
import com.gymbrosbro.app.data.db.entity.SchedulePauseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleRuleEntity
import com.gymbrosbro.app.domain.model.ScheduleAction
import com.gymbrosbro.app.domain.model.ScheduleKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** One planned workout on one day, after moves, skips and pauses are applied. */
data class Occurrence(
    val ruleId: Long,
    val templateId: Long?,
    /** The day the rule put it on. */
    val originalDate: LocalDate,
    /** The day it actually lands on (differs when moved). */
    val date: LocalDate,
    val skipped: Boolean = false,
    val paused: Boolean = false,
) {
    val moved get() = date != originalDate

    /** Counts toward the schedule streak. */
    val active get() = !skipped && !paused
}

data class ScheduleData(
    val rules: List<ScheduleRuleEntity> = emptyList(),
    val exceptions: List<ScheduleExceptionEntity> = emptyList(),
    val pauses: List<SchedulePauseEntity> = emptyList(),
) {
    fun isPaused(date: LocalDate) = pauses.any { date >= it.startDate && date <= it.endDate }

    /** Earliest day any rule applies, or null with no rules. */
    val firstDate: LocalDate? get() = rules.minOfOrNull { it.startDate }
}

/** Expands schedule rules into dated occurrences (SPEC §7.5). */
object Schedule {
    fun dayBit(day: DayOfWeek) = 1 shl (day.value - 1)

    fun hasDay(mask: Int, day: DayOfWeek) = mask and dayBit(day) != 0

    fun weekStart(date: LocalDate, firstDay: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDay))

    /** Whether [rule] puts an occurrence on [date], before exceptions. */
    fun fallsOn(rule: ScheduleRuleEntity, date: LocalDate): Boolean {
        if (date < rule.startDate || (rule.endDate != null && date > rule.endDate)) return false
        return when (rule.kind) {
            ScheduleKind.WEEKLY -> hasDay(rule.daysOfWeek, date.dayOfWeek)
            ScheduleKind.EVERY_N_DAYS -> {
                val n = rule.intervalDays?.takeIf { it > 0 } ?: return false
                ChronoUnit.DAYS.between(rule.startDate, date) % n == 0L
            }
            ScheduleKind.ONE_OFF -> date == rule.startDate
        }
    }

    /** Occurrences landing in [from]..[to] (inclusive), sorted by date. */
    fun occurrences(data: ScheduleData, from: LocalDate, to: LocalDate): List<Occurrence> {
        if (to < from) return emptyList()
        val exceptions = data.exceptions.associateBy { it.ruleId to it.originalDate }
        val result = mutableListOf<Occurrence>()
        // Moves stay inside their week, so a week of margin catches anything moved in.
        var day = from.minusDays(6)
        val last = to.plusDays(6)
        while (day <= last) {
            for (rule in data.rules) {
                if (!fallsOn(rule, day)) continue
                val exception = exceptions[rule.id to day]
                val date = if (exception?.action == ScheduleAction.MOVE) exception.newDate ?: day else day
                if (date < from || date > to) continue
                result += Occurrence(
                    ruleId = rule.id,
                    templateId = rule.templateId,
                    originalDate = day,
                    date = date,
                    skipped = exception?.action == ScheduleAction.SKIP,
                    paused = data.isPaused(date),
                )
            }
            day = day.plusDays(1)
        }
        return result.sortedWith(compareBy({ it.date }, { it.ruleId }))
    }
}

/** How a calendar day looks (SPEC §7.5). */
enum class DayStatus {
    NONE,

    /** A workout was logged (filled marker). */
    DONE,

    /** Planned for today or later (outlined marker). */
    SCHEDULED,

    /** Planned, passed, nothing logged (gray). */
    MISSED,
    SKIPPED,
    PAUSED,
    ;

    companion object {
        fun of(date: LocalDate, today: LocalDate, occurrences: List<Occurrence>, worked: Boolean, paused: Boolean): DayStatus = when {
            worked -> DONE
            occurrences.any { it.active } -> if (date >= today) SCHEDULED else MISSED
            occurrences.any { it.skipped } -> SKIPPED
            paused && occurrences.isNotEmpty() -> PAUSED
            else -> NONE
        }
    }
}
