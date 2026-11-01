package com.gymbrosbro.app.domain.schedule

import java.time.LocalDate

sealed interface Reminder {
    /** A workout is scheduled today. [templateIds] null entries = any workout. */
    data class WorkoutDay(val templateIds: List<Long?>) : Reminder

    /** Yesterday was missed and today is its grace day. */
    data class GraceDay(val missed: LocalDate) : Reminder
}

/**
 * Whether today's reminder should fire (SPEC §7.8). At most one a day, and
 * never once you've already trained today.
 */
object Reminders {
    fun decide(today: LocalDate, schedule: ScheduleData, workoutDays: Set<LocalDate>, graceEnabled: Boolean): Reminder? {
        if (today in workoutDays) return null
        val todays = Schedule.occurrences(schedule, today, today).filter { it.active }
        if (todays.isNotEmpty()) return Reminder.WorkoutDay(todays.map { it.templateId })
        if (!graceEnabled) return null
        val yesterday = today.minusDays(1)
        val missed = yesterday !in workoutDays &&
            Schedule.occurrences(schedule, yesterday, yesterday).any { it.active }
        return if (missed) Reminder.GraceDay(yesterday) else null
    }
}
